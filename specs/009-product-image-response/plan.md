# Implementation Plan: Schema de resposta do registro de imagem de produto

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-product-image-response/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Preencher o corpo do desfecho `201` da operação `addProductImage` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-4). Duas adições: **(1)** bloco `content` (`application/json`, via `$ref`) acrescentado ao `201` **já existente**, cuja `description` permanece intocada; **(2)** schema `ProductImageResponse` em `components/schemas`, com `productId` e a **coleção resultante completa, em ordem**.

**Segunda task da cadeia a gerar código** — 5 → 6 DTOs. O argumento do Princípio III é o já reformulado em T-002-3, não o caduco de T-002-1/T-002-2.

A spec registra **duas lacunas de backlog** com entradas propostas ao usuário (`T-002-6`, `T-002-7`); `TASKS.json` permanece intocado.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` com `generateApis=false`, `generateModels=true` (ADR 0002). **Contagem reverificada empiricamente antes de afirmar**: hoje **5 schemas ↔ 5 DTOs** (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`) — a correspondência 1:1 seguiu válida após T-002-3, como previsto. Esta task leva a **6 ↔ 6**.

**Storage**: N/A. A resposta descreve a coleção resultante; a persistência é da cadeia T-003 (coluna `text[]`, fixada por T-002-3).

**Testing**: build do módulo + comparação de inventário de DTOs + regressão do consumidor — os **mesmos dois gates de T-002-3, reproduzidos e não assumidos**.

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: **Estritamente aditiva** — zero remoções, como em T-002-1 e T-002-3. O `content` é acrescentado **abaixo** da `description` do `201`, que não é tocada (FR-003); a `description` do Path Item também não (FR-012 — limpeza final é de T-002-5). Sem `minItems` (FR-009), sem `additionalProperties: false` (FR-010), `400`/`403`/`404` seguem sem `content` (FR-011), mídia `application/json` exclusiva (FR-001).

**Scale/Scope**: 1 arquivo; ~22 linhas novas em dois pontos. Zero arquivos Java escritos à mão; **1 arquivo Java gerado**.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo que o possui. **Divergência conhecida, registrada — não violação**: o princípio exige que **as respostas de erro do serviço** sejam RFC 7807 `ProblemDetail` via `ApiExceptionHandler`. Isso continuará valendo e será cumprido pelo serviço; o que falta é o **contrato documentar** esse formato — hoje `400`/`403`/`404` são description-only e nenhuma task modela `ProblemDetail`. O princípio governa o comportamento do serviço, que não muda aqui; o contrato apenas o descreve de forma incompleta. Encaminhado como entrada proposta `T-002-7`. | ✅ PASS (divergência documentada) |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. | ✅ N/A |
| III. Tiered Testing Discipline | **Argumento herdado de T-002-3, não o de T-002-1/T-002-2.** Há geração (`ProductImageResponse` → DTO, 5 → 6), mas geração ≠ comportamento: o artefato é portador de dados sem lógica, que o serviço **ainda não referencia** (cadeia T-005). Sem unidade de comportamento a testar em `hb-catalog-service`. Gates: build do módulo, comparação de inventário de DTOs e `mvn -B verify` — os dois últimos **reproduzidos**, com captura prévia própria, não herdados da execução anterior. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nada de contadores. A decisão de não exigir `Idempotency-Key` foi encerrada em T-002-2 e não é reaberta por declarar um corpo de resposta. | ✅ N/A |
| VI. Security & Configuration Hygiene | Autenticação e role `admin` já declaradas em T-002-2; intocadas. A resposta não expõe dado sensível — devolve URLs que o próprio chamador forneceu. Nenhum secret entra no documento. | ✅ PASS |
| Technology Constraints | Nenhuma dependência ou infraestrutura nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

**Duas lacunas de backlog registradas** (nenhuma é violação; ambas são incompletude de **documentação** do contrato, com encaminhamento acionável e `TASKS.json` intocado):

| Lacuna | Natureza | Encaminhamento |
|---|---|---|
| Schema `Product` sem `images` | Após T-003-4 o serviço devolverá imagens que o contrato não declara | Entrada proposta `T-002-6` |
| Corpos de erro sem `ProblemDetail` | Princípio I exige RFC 7807 do serviço; o contrato não o modela | Entrada proposta `T-002-7` |

## Project Structure

### Documentation (this feature)

```text
specs/009-product-image-response/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── product-image-response.yaml   # Os 2 fragmentos-alvo
├── checklists/
│   ├── requirements.md      # /speckit-specify quality gate (16/16)
│   └── response-schema.md   # /speckit-checklist (40 itens; 3 falhas → correções na spec)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado à mão)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + content no desfecho '201' existente
                           #   (description do 201 intocada acima dele)
                           # + schema ProductImageResponse em components/schemas
                           #   (anexado após ProductImageRequest)

# Artefato GERADO (não escrito à mão, não versionado):
# .../dto/ProductImageResponse.java   → segundo código produzido pela cadeia T-002

# hb-catalog-service/ — NENHUM arquivo alterado
```

**Structure Decision**: Padrão da cadeia. Quarta feature da T-002; resta apenas T-002-5, que também encerra a `description` do Path Item.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia. As duas lacunas registradas são incompletude de documentação do contrato, com destinatário nomeado, e não violam princípio algum: o Princípio I governa o que o **serviço** responde, e isso não muda aqui.

## Constitution Re-Check (pós-Phase 1)

Design finalizado: dois fragmentos aditivos em 1 arquivo. A Phase 1 confirmou que a única entidade nova é de documento (um schema) e sua contrapartida é um DTO gerado sem lógica. Nenhuma pendência constitucional aberta; as duas lacunas seguem registradas com encaminhamento, não convertidas em dívida silenciosa. **Gate final: PASS.**
