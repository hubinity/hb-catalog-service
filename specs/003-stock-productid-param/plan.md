# Implementation Plan: Especificação fina do parâmetro productId da operação de leitura de saldo

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia T-001-x) | **Date**: 2026-07-23 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-stock-productid-param/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Completar o parâmetro `productId` da operação `getStockItemByProductId` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-001-3, escopo residual): adicionar `description: Product UUID` (única diferença restante para a paridade com a convenção `getProductById`), ratificar formalmente o bloco mínimo entregue pela contingência FR-006 da feature 002 (evidência no corpo do commit), e atualizar a `description` do Path Item para o texto fixado em FR-003. Diff esperado: 1 linha adicionada + bloco de description reescrito. Validação e workflow herdados (build do módulo; branch da cadeia; commits no polish).

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` (DTO-only, ADR 0002); staging antrun em `${java.io.tmpdir}` (herdado)

**Storage**: N/A

**Testing**: `( cd platform-shared-contracts && mvn -B -DskipTests install )` — autoridade herdada (feature 001, R3); inspeções estáticas por grep/diff

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: Diff restrito a 1 linha no parâmetro + bloco de description do Path Item (FR-004); paridade campo a campo com `getProductById` exceto `name` (FR-001); nenhum refinamento além da convenção (FR-005); textos exatos fixados no spec (FR-003) e evidência de ratificação no corpo do commit (FR-002)

**Scale/Scope**: 1 arquivo, +1 linha e ~5 linhas reescritas (description do Path Item)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo correto. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. | ✅ N/A |
| III. Tiered Testing Discipline | Nenhum comportamento Java; gate = build do módulo (padrão da cadeia). | ✅ PASS |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Operação de leitura; nada tocado. | ✅ N/A |
| VI. Security & Configuration Hygiene | `security` segue explicitamente deferido a T-001-5. | ✅ PASS (deferido) |
| Technology Constraints | Mudança de contrato em platform-shared-contracts; nenhuma dependência nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/003-stock-productid-param/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── productid-param.yaml     # Fragmento-alvo: parâmetro completo + description do Path Item
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── contract.md      # /speckit-checklist revisão de PR (15/15)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + description: Product UUID no parâmetro
                           # ~ description do Path Item reescrita (FR-003)

# hb-catalog-service/ — NENHUM arquivo de código alterado nesta feature
```

**Structure Decision**: Padrão da cadeia T-001-x — edição no repo irmão de contratos, artefatos de feature neste repo, branch compartilhada `feature/stock-balance-path`, commit incremental por task.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado sem elementos novos: 1 linha adicionada + description reescrita, validadas por build Maven offline. **Gate final: PASS.**
