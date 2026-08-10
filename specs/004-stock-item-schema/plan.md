# Implementation Plan: Schema de resposta da operação de leitura de saldo de estoque

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia T-001-x) | **Date**: 2026-07-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-stock-item-schema/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Reescrever `components/schemas/StockItem` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` para paridade com o record real do serviço (`api/dto/StockItemResponse.java`: productId/available/reserved/reorderPoint/updatedAt), adicionar `content: application/json` com `$ref` na `'200'` de `getStockItemByProductId`, e atualizar a description do Path Item (pendência restante: T-001-5). Mudança breaking segura (ADR 0006, build-only) com prova de regressão obrigatória no consumidor (FR-006). Decisões estruturais tomadas pelo usuário na confirmação do pipeline: reusar `StockItem` (sem schema novo) e alinhar campos ao serviço.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` (DTO-only, ADR 0002 — o DTO `StockItem` será **regenerado com campos novos**); staging antrun (herdado)

**Storage**: N/A

**Testing**: build do módulo (`mvn -B -DskipTests install`) + regressão do consumidor (`mvn -B verify` em hb-catalog-service) — FR-006 eleva a verificação de consumo de opcional para obrigatória nesta feature (breaking change)

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: Mudança restrita a: schema `StockItem`, bloco `'200'` da operação de saldo, description do Path Item (FR-005); 0 ocorrências dos campos antigos (FR-003); schema-alvo fixado no spec (FR-001); regressão zero comprovada (FR-006)

**Scale/Scope**: 1 arquivo de contrato; ~25 linhas do schema reescritas, ~4 linhas na resposta, description do Path Item

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; "cross-service payloads MUST come from platform-shared-contracts" — esta feature torna o payload compartilhado fiel à realidade, viabilizando a adoção futura do DTO gerado (T-004-x). | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco (o "schema" aqui é de contrato, não DDL). | ✅ N/A |
| III. Tiered Testing Discipline | Nenhum comportamento Java muda; gates: build do módulo + `mvn -B verify` do consumidor (obrigatório por FR-006 — breaking change). | ✅ PASS |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Leitura; nada tocado. `minimum: 0` apenas documenta o invariante já garantido pelos conditional UPDATEs. | ✅ N/A |
| VI. Security & Configuration Hygiene | `security` segue deferido a T-001-5 (última pendência da operação). | ✅ PASS (deferido) |
| Technology Constraints | Mudança de contrato no repo correto; nenhuma dependência nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/004-stock-item-schema/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── stock-item-schema.yaml   # Fragmentos-alvo: schema reescrito + content da '200' + description
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── contract.md      # /speckit-checklist revisão de PR (21/21)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # ~ schema StockItem reescrito (5 campos reais)
                           # + content application/json na '200' da operação de saldo
                           # ~ description do Path Item (pendência restante: T-001-5)

# hb-catalog-service/ — NENHUM arquivo de código alterado
# (StockItemResponse.java é fonte de verdade, intocado; mvn -B verify prova regressão zero)
```

**Structure Decision**: Padrão da cadeia T-001-x — edição no repo irmão, artefatos aqui, branch compartilhada, commit incremental por task.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado sem elementos novos: edição em 1 arquivo YAML validada por build offline + prova de regressão no consumidor. A natureza breaking da mudança está mitigada por ADR 0006 + FR-006, não por exceção a princípio. **Gate final: PASS.**
