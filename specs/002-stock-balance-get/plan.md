# Implementation Plan: Operação GET no path canônico de leitura de saldo de estoque

**Branch**: `feature/stock-balance-path` (já ativa em ambos os repos — herdada da feature 001) | **Date**: 2026-07-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-stock-balance-get/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Declarar a operação `get` no Path Item `/api/v1/products/{productId}/stock` de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-001-2): identidade (`operationId: getStockItemByProductId`, `summary`, `tags: [stock]` com a tag declarada), desfechos `'200'` (sem content) e `'404'` único (produto inexistente ou sem registro de saldo — decisão do usuário), mais ajuste da `description` do Path Item herdada de T-001-1 (FR-007). Edição aditiva validada pela autoridade herdada da feature 001 (research 001/R3): parse do `openapi-generator-maven-plugin` durante `mvn -B install`. Risco central: validador exigir o parâmetro `{productId}` não declarado (escopo de T-001-3) — contingência FR-006 de entrega conjunta no mesmo commit.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` (generator `spring`, DTO-only — `generateApis=false`/`generateModels=true`, ADR 0002); staging antrun em `${java.io.tmpdir}` (workaround de caminho não-ASCII, herdado)

**Storage**: N/A (nenhuma mudança de banco; Flyway não envolvido)

**Testing**: `( cd platform-shared-contracts && mvn -B -DskipTests install )` — autoridade de validação herdada (feature 001, R3); inspeções estáticas por grep/diff

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local apenas (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão `platform-shared-contracts`)

**Performance Goals**: N/A (mudança documental)

**Constraints**: Mudança aditiva restrita ao Path Item do saldo + seção `tags` (FR-005); exatamente 1 operação `get` (FR-001); respostas `'200'`/`'404'` com textos fixados no spec (FR-004); `'200'` sem `content` (corpo é T-001-4); descrição do Path Item atualizada (FR-007); contingência FR-006 se o parse exigir o parâmetro de path

**Scale/Scope**: 1 arquivo, ~15 linhas adicionadas (operação + tag) e ~4 linhas reescritas (description do Path Item)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; mudança de contrato feita no repo correto (platform-shared-contracts). | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de schema de banco. | ✅ N/A |
| III. Tiered Testing Discipline | Nenhum comportamento Java muda; gate equivalente = build do módulo de contratos (offline, sem Docker), como estabelecido na feature 001. | ✅ PASS |
| IV. Events via Transactional Outbox | Nenhum evento criado/alterado. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Operação de leitura (GET) — fora do conjunto de mutações com Idempotency-Key; nenhum contador tocado. | ✅ N/A |
| VI. Security & Configuration Hygiene | `security` da operação é escopo explícito de T-001-5 (Out of Scope + Assumption registradas); nenhum secret/config. | ✅ PASS (deferido explicitamente) |
| Technology Constraints | Mudança de contrato em platform-shared-contracts consumida como artefato Maven — exatamente a regra; nenhuma dependência nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/002-stock-balance-get/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── stock-balance-get.yaml    # Fragmento-alvo: Path Item completo pós-edição
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
        └── catalog.yaml   # + operação get no Path Item do saldo
                           # + tag `stock` na seção tags
                           # ~ description do Path Item reescrita (FR-007)

# hb-catalog-service/ — NENHUM arquivo de código alterado nesta feature
```

**Structure Decision**: Mesma estrutura da feature 001 — edição no repo irmão de contratos, artefatos de feature neste repo; branch `feature/stock-balance-path` compartilhada pela cadeia T-001-x (uma branch para a cadeia inteira, commits incrementais por task).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado (research.md, data-model.md, contracts/, quickstart.md) não introduziu elementos novos: segue 1 edição aditiva em 1 arquivo YAML validada por build Maven offline; contingência FR-006 não altera o veredito (a operação GET mínima + parâmetro continuariam dentro dos mesmos princípios). **Gate final: PASS.**
