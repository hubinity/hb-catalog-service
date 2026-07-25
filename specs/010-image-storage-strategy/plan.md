# Implementation Plan: Estratégia de armazenamento de imagens de produto

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-image-storage-strategy/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Encerrar a cadeia T-002 em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-5). Duas edições de texto: **(1)** parágrafo normativo acrescentado a `info.description`, declarando a estratégia **URL-only** e — conteúdo hoje ausente do contrato — a **fronteira de responsabilidade**: o catálogo não verifica que a URL resolve, e uma referência que deixa de resolver não é violação de contrato; **(2)** `description` do Path Item `/api/v1/products/{productId}/images` reescrita para o estado final, retirando o andaime "completed by T-002-3 and T-002-4".

**Primeira task da cadeia que gera zero código** — 6 ↔ 6 DTOs, inalterados. Isso **endurece** o gate de verificação em vez de afrouxá-lo: o critério deixa de ser "exatamente um DTO novo" (T-002-3, T-002-4) e passa a ser **igualdade de checksum nos seis arquivos**.

**Primeira task da cadeia com remoção.** T-002-1, T-002-3 e T-002-4 foram estritamente aditivas; esta reescreve um bloco — pela mesma razão que autorizou T-001-5 a reescrever o seu: só a última task da cadeia pode declará-la encerrada. O escopo da remoção é fechado por predicado verificável (FR-017).

A spec registra **uma contradição downstream** (`T-005-3`, multipart) e **uma lacuna** (enforcement HTTPS → proposta `T-002-8`), ambas com entrada concreta proposta; `TASKS.json` permanece intocado.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` com `generateApis=false`, `generateModels=true` (ADR 0002). **Contagem reverificada empiricamente antes de afirmar** (`contracts-catalog/target/generated-sources/`): hoje **6 schemas ↔ 6 DTOs** (`Product`, `Category`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`). Esta task mantém **6 ↔ 6** — nenhum schema é criado, alterado ou removido.

**Storage**: N/A para o serviço. O objeto desta task **é** uma decisão de armazenamento, mas declarada no contrato: as imagens ficam **fora** do sistema e o catálogo persiste apenas URLs. A coluna que guardará essas URLs é da cadeia T-003 (`text[]`, fixada por T-002-3).

**Testing**: build do módulo + comparação de inventário de DTOs **por checksum** + regressão do consumidor. Os mesmos dois gates da cadeia, com o de geração **endurecido** (igualdade, não incremento) e ambos **reproduzidos nesta execução**, nunca herdados.

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: **Uma adição e uma reescrita**, em dois blocos de `description` nomeados. Toda linha removida no diff MUST pertencer ao bloco `description` do Path Item de imagens (FR-017) — critério objetivo que substitui "seja cuidadoso". As duas linhas preexistentes de `info.description` permanecem intactas (FR-007); `summary` do Path Item intocado (FR-013); `ProductImageRequest`/`ProductImageResponse` intocados (FR-014); **nenhum `pattern` HTTPS** (FR-015); nenhuma operação, parâmetro, desfecho ou schema adicionado ou alterado (FR-016).

**Scale/Scope**: 1 arquivo; ~14 linhas novas em `info.description`, ~8 linhas reescritas no Path Item. Zero arquivos Java escritos à mão; **zero arquivos Java gerados a mais** — e nenhum alterado.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo que o possui. **Divergência conhecida herdada, não agravada**: o princípio exige RFC 7807 `ProblemDetail` em toda resposta de erro **do serviço** — o que segue valendo e será cumprido; o contrato é que ainda não modela esse corpo (`400`/`403`/`404` description-only). Já encaminhada como `T-002-7`, hoje no tracker com status `refined`. Esta task não a toca. | ✅ PASS (divergência registrada, com destinatário) |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. A declaração afirma o que **não** é armazenado; a coluna que guardará URLs é de T-003-2, sob Flyway. | ✅ N/A |
| III. Tiered Testing Discipline | **Argumento mais forte que o das tasks anteriores da cadeia, não mais fraco.** T-002-3 e T-002-4 alegaram "geração ≠ comportamento" para justificar ausência de teste apesar de gerarem DTO. Aqui **não há geração alguma**: a mudança é exclusivamente textual em `description`, que o gerador não lê para produzir modelo. Não existe unidade de comportamento a testar em `hb-catalog-service`. Gates aplicáveis: build do módulo, **igualdade de checksum** nos 6 DTOs e `mvn -B verify`, todos reproduzidos nesta execução com captura prévia própria. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nada de contadores de estoque. A decisão de não exigir `Idempotency-Key` na operação de imagens foi encerrada em T-002-2 e não é reaberta por declarar estratégia de armazenamento. | ✅ N/A |
| VI. Security & Configuration Hygiene | Autenticação e role `admin` declaradas em T-002-2; a reescrita da `description` **preserva** essa informação por exigência explícita (FR-011) — é o requisito que impede a limpeza de virar perda de conteúdo. Nenhum secret entra no documento. **Nota de superfície**: a declaração torna explícito que o catálogo não valida alcançabilidade de URL, o que é postura correta — validar exigiria I/O de rede a partir do servidor sobre uma URL fornecida pelo cliente (vetor SSRF). A não-verificação é decisão de segurança defensável, não omissão. | ✅ PASS |
| Technology Constraints | Nenhuma dependência, framework ou infraestrutura nova. Nenhum ADR necessário: a estratégia ora ratificada é a que a cadeia já implementava. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

**Pendências registradas** (nenhuma é violação; nenhuma é resolvida aqui):

| Item | Natureza | Encaminhamento |
|---|---|---|
| `T-005-3` exige recepção **multipart** | **Contradição direta** com a estratégia ratificada — o serviço nunca recebe bytes | Substituição proposta na spec (§Out of Scope); `TASKS.json` intocado |
| HTTPS sem enforcement | Expectativa textual sem `pattern`; URL `http://` é aceita pelo contrato e só falha no navegador | Entrada proposta `T-002-8` |
| Corpos de erro sem `ProblemDetail` | Herdada de T-002-4 | `T-002-7`, já no tracker |
| `Product` sem `images` | Herdada de T-002-4 | `T-002-6`, já no tracker |

## Project Structure

### Documentation (this feature)

```text
specs/010-image-storage-strategy/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── image-storage-strategy.yaml   # Os 2 blocos-alvo, em estado final
├── checklists/
│   ├── requirements.md   # /speckit-specify quality gate (16/16)
│   └── contract.md       # /speckit-checklist (39 itens; 1 falha → FR-012 reescrito)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado à mão)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # ~ info.description: + parágrafo da estratégia URL-only
                           #   (as 2 linhas existentes permanecem acima, intactas)
                           # ~ /api/v1/products/{productId}/images:
                           #   description reescrita para estado final
                           #   (summary intocado; única remoção da entrega)

# NÃO tocados, por exigência explícita:
#   components/schemas/ProductImageRequest    (T-002-3, done)
#   components/schemas/ProductImageResponse   (T-002-4, done)
#   qualquer operação, parâmetro ou desfecho

# Artefatos GERADOS: os mesmos 6, com checksum idêntico.
#   Nenhum criado, nenhum alterado, nenhum removido.

# hb-catalog-service/ — NENHUM arquivo alterado (apenas medido)
```

**Structure Decision**: Padrão da cadeia — edição no repo irmão, serviço apenas como consumidor onde a regressão é medida. **Quinta e última feature da cadeia T-002.** Após esta, restam no tracker `T-002-6` e `T-002-7` (lacunas já registradas), mais a proposta `T-002-8`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

Registre-se, porém, o ponto que mais se aproximaria de uma: **esta é a primeira task da cadeia a remover linhas**. Não é complexidade nem exceção — é a operação que a cadeia reservou desde T-002-3 para sua última task, com precedente executado em T-001-5 (`68873d5`) e com escopo fechado por predicado verificável em FR-017. O risco correspondente (remover mais do que o autorizado) é endereçado por gate objetivo, não por atenção do implementador.

## Constitution Re-Check (pós-Phase 1)

Design finalizado: dois blocos de `description` em 1 arquivo, nenhum artefato novo de qualquer natureza. A Phase 1 confirmou que esta feature **não introduz entidade alguma** — nem de documento, nem gerada — sendo a primeira da cadeia com essa propriedade; `data-model.md` a registra como deliberadamente vazia de entidades novas. Nenhuma pendência constitucional aberta; as quatro pendências da tabela seguem registradas com destinatário nomeado, nenhuma convertida em dívida silenciosa. **Gate final: PASS.**
