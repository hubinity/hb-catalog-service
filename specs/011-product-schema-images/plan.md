# Implementation Plan: Propriedade images no schema Product

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da linhagem) | **Date**: 2026-07-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-product-schema-images/spec.md`

**Summary**: Acrescentar a propriedade `images` ao schema `Product` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-6), fechando a divergência que a spec 009 apontou: sem ela, o serviço devolveria imagens (após `T-003-4`) que o contrato nunca declarou.

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Uma adição: a propriedade `images` em `components/schemas/Product/properties` — `type: array`, itens `type: string` / `format: uri` / `maxLength: 2048` e **nenhuma outra restrição**, com `description` declarando ordem, papel do primeiro elemento, semântica de ausência e remissão à estratégia URL-only de `info.description`.

**A task materializa o beneficiário de `T-002-5`.** A justificativa registrada para colocar a estratégia em `info.description` — e não no Path Item de imagens — foi alcançar quem lê a operação de produto **depois que `T-002-6` expusesse `images` no schema `Product`**. Sem esta task, aquela decisão de posicionamento fica sem o leitor que a motivou.

**Terceira task da linhagem a gerar código, e a primeira a alterar um DTO preexistente** em vez de criar um novo. `T-002-3` e `T-002-4` acrescentaram arquivos (5→6, 6→6+1); `T-002-5` não gerou nada; esta mantém **6 ↔ 6** e altera **exatamente um** conteúdo: `Product.java`. O gate é, por isso, de forma inédita na linhagem — nem incremento, nem inércia, mas **alteração dirigida e única**.

A spec registra **uma lacuna nova** (`T-002-9` — operações de coleção ausentes, alvo de `T-006-1`..`T-006-6`) e **uma divergência estrutural** encaminhada a ADR, não a task; `TASKS.json` permanece intocado.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` com `generateApis=false`, `generateModels=true` (ADR 0002). **Contagem reverificada empiricamente antes de afirmar**: **6 schemas ↔ 6 DTOs**. Esta task mantém 6 ↔ 6 — nenhum schema criado ou removido, um alterado.

**Storage**: N/A. A propriedade expõe URLs de imagens hospedadas externamente, conforme a estratégia URL-only ratificada em `T-002-5` (`c6eaccb`). A coluna que persistirá essas URLs é da cadeia T-003 (`text[]`, fixada por `T-002-3`).

**Testing**: build do módulo + comparação dirigida de inventário de DTOs + inspeção do campo gerado + regressão do consumidor. **O terceiro gate é novo na linhagem**: nas tasks anteriores bastava contar arquivos; aqui é preciso confirmar **o conteúdo** da alteração, porque checksum diferente prova que algo mudou, não o quê.

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: **Estritamente aditiva** — zero remoções, como em `T-002-1`, `T-002-3` e `T-002-4`; `T-002-5` foi a única da linhagem a remover, por mandato de encerramento de cadeia. `images` fora de `required` (FR-006); as nove propriedades preexistentes intocadas (FR-007); **conjunto de restrições dos itens fechado** (FR-003) — sem `minLength`, sem `pattern`, sem qualquer palavra-chave além das três; sem `minItems` (FR-008), sem `maxItems` (FR-009), sem `readOnly` (FR-010); nenhum outro schema, operação ou path alterado (FR-012/FR-013).

**Scale/Scope**: 1 arquivo; ~11 linhas novas em um ponto. Zero arquivos Java escritos à mão; **zero arquivos gerados a mais**, **um alterado**.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo que o possui. O princípio manda que payloads entre serviços venham de `platform-shared-contracts`, "nunca duplicatas escritas à mão". **Divergência estrutural constatada e registrada**: hoje o serviço **não consome** os DTOs gerados — nada em `src/` importa `com.hubinity.contracts` — e mantém `api/dto/ProductResponse.java` como paralelo do `Product` do contrato. Esta task **não cria** essa divergência (preexiste a toda a linhagem) nem a agrava; corrigi-la é decisão de arquitetura com alcance ecossistêmico, encaminhada a ADR em *Out of Scope*. A `ProblemDetail` segue em `T-002-7`. | ✅ PASS (duas divergências registradas, nenhuma criada aqui) |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. A coluna correspondente é de `T-003-2`, sob Flyway. | ✅ N/A |
| III. Tiered Testing Discipline | Há geração (`Product.java` ganha um campo), mas **geração ≠ comportamento**: o artefato é portador de dados sem lógica e, neste repositório, **sem consumidor** — nada em `hb-catalog-service/src/` o importa. Não existe unidade de comportamento a testar. Gates: build do módulo, comparação dirigida do inventário, **inspeção do campo gerado** e `mvn -B verify`, todos reproduzidos nesta execução com captura prévia própria. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nada de contadores de estoque nem de endpoints mutantes. | ✅ N/A |
| VI. Security & Configuration Hygiene | Nenhuma mudança de autenticação ou autorização. A propriedade expõe URLs que o próprio administrador registrou via `POST …/images` (gated em `admin`), não dado sensível novo. **Nota**: nenhum `readOnly: true` é declarado, e isso foi verificado como seguro — `Product` não é corpo de requisição em nenhuma operação do documento, então não há superfície de escrita a proteger. Nenhum secret entra no documento. | ✅ PASS |
| Technology Constraints | Nenhuma dependência, framework ou infraestrutura nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

**Pendências registradas** (nenhuma é violação; nenhuma é resolvida aqui):

| Item | Natureza | Encaminhamento |
|---|---|---|
| Operações de coleção ausentes no contrato | `T-006-1`..`T-006-6` especificam ETag sobre `GET /api/v1/products` e `GET /api/v1/categories`, que **o contrato não declara** — há apenas 3 paths | Entrada proposta `T-002-9` (spec §Out of Scope) |
| `Product` (contrato) × `ProductResponse` (serviço) | Divergência estrutural: DTOs paralelos sem derivação; `T-003-4` a aprofunda | **ADR**, não task — a decisão precede a task |
| `T-005-3` exige recepção multipart | Contradição com a estratégia URL-only ratificada em `T-002-5` | Substituição proposta na spec 010, **ainda pendente** |
| Corpos de erro sem `ProblemDetail` | Herdada | `T-002-7`, no tracker |
| HTTPS sem enforcement | Herdada | Proposta `T-002-8`, ainda não inserida |

## Project Structure

### Documentation (this feature)

```text
specs/011-product-schema-images/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── product-images-property.yaml   # O bloco-alvo, em estado final
├── checklists/
│   ├── requirements.md   # /speckit-specify quality gate (16/16)
│   └── schema.md         # /speckit-checklist (40 itens; 2 falhas → FR-003 fechado + edge case novo)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado à mão)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + propriedade images em components/schemas/Product/properties
                           #   (anexada após updatedAt; required NÃO é tocada)

# NÃO tocados, por exigência explícita:
#   Product.required                          → permanece [id, sku, name, price, categoryId, active]
#   as 9 propriedades preexistentes de Product
#   components/schemas/ProductImageRequest    (T-002-3, done)
#   components/schemas/ProductImageResponse   (T-002-4, done — molde de forma)
#   info.description                          (T-002-5, done — alvo da remissão)
#   todos os paths, operações e desfechos

# Artefato GERADO: os mesmos 6 arquivos.
#   Nenhum criado, nenhum removido, EXATAMENTE UM alterado: Product.java (+ campo images)

# hb-catalog-service/ — NENHUM arquivo alterado (apenas medido)
```

**Structure Decision**: Padrão da linhagem — edição no repo irmão, serviço apenas como medidor de regressão. Primeira task da série T-002 **posterior ao encerramento da cadeia original** (T-002-1..T-002-5): nasceu de lacuna identificada pela spec 009 e inserida pelo usuário no tracker.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

Registre-se o ponto que mais se aproximaria de uma: o Princípio I diz que payloads entre serviços devem vir de `platform-shared-contracts`, "nunca duplicatas escritas à mão", e o serviço hoje mantém DTOs próprios paralelos aos gerados. **Isso não é violação criada por esta task** — é estado preexistente a toda a linhagem, e esta task o deixa exatamente como encontrou. Convertê-lo em conformidade exige decisão ecossistêmica (o serviço passar a consumir os DTOs gerados), que pertence a um ADR e não a uma task de contrato. Registrado em *Out of Scope* para consideração do usuário, sem entrada de tracker proposta, porque a decisão precede a task.

## Constitution Re-Check (pós-Phase 1)

Design finalizado: uma propriedade em um schema, em 1 arquivo. A Phase 1 confirmou que a feature introduz **uma entidade de documento** (a propriedade) cuja contrapartida gerada é **um campo em classe existente**, não uma classe nova — forma inédita na linhagem, refletida no gate de FR-016/FR-017. Nenhuma pendência constitucional aberta; as cinco pendências da tabela seguem registradas com destinatário nomeado, nenhuma convertida em dívida silenciosa. **Gate final: PASS.**
