# Implementation Plan: Referenciar ProblemDetail no 404 de getStockItemByProductId

**Branch**: `014-getstockitem-404-ref` (git branch: `feature/stock-balance-path`, execução não-escopada — sem worktree dedicada) | **Date**: 2026-07-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/014-getstockitem-404-ref/spec.md` — `T-002-7-3` (TASKS.json).

## Summary

Referenciar o schema `ProblemDetail` (já declarado em `T-002-7-1`, já usado uma vez em `T-002-7-2`) no `content` do desfecho `404` da operação `getStockItemByProductId`, em `contracts-catalog/openapi/catalog.yaml`. Abordagem idêntica à da task irmã `T-002-7-2`: acréscimo pontual de um bloco `content: application/json: schema: $ref`, sem tocar em `description`, no desfecho `200`, ou em qualquer outro schema/operação. Diferença relevante: esta operação ainda não tem implementação Java (cadeia `T-004`, `refined`) — a mudança é puramente contratual, sem comportamento de runtime a verificar ou regredir.

## Technical Context

**Language/Version**: YAML (OpenAPI 3.1) — nenhuma linguagem de aplicação envolvida diretamente

**Primary Dependencies**: `openapi-generator-maven-plugin` (`contracts-catalog/pom.xml`) — gera `ProblemDetail.java`, já existente desde `T-002-7-1`; nenhuma dependência nova

**Storage**: N/A

**Testing**: Build do módulo `contracts-catalog` (valida OpenAPI 3.1, confirma 7↔7 DTOs); `mvn -B verify` no `hb-catalog-service` — verde por ausência estrutural de código a regredir (a rota `getStockItemByProductId` não existe em `StockController.java` ainda), não apenas por preservação de comportamento existente

**Target Platform**: N/A — mudança de contrato, sem runtime próprio

**Project Type**: Módulo de contratos compartilhados (`platform-shared-contracts/contracts-catalog`), consumido como dependência Maven pelo `hb-catalog-service`

**Performance Goals**: N/A

**Constraints**: A mudança MUST ser estritamente aditiva (nenhuma linha `-` no diff); a contagem de schemas/DTOs gerados MUST permanecer 7↔7

**Scale/Scope**: Uma única modificação em um único desfecho de uma única operação — mesma escala da task irmã `T-002-7-2`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Justificativa |
|---|---|---|
| I. Layered Architecture Boundaries | **PASS** | Mudança inteiramente no contrato compartilhado; nenhum código do `hb-catalog-service` é alterado. Diferente de `T-002-7-2`, aqui não há sequer um handler existente a descrever — o contrato roda à frente da implementação (`T-004`, pendente), mesmo padrão já usado para imagens de produto (`T-002` antes de `T-003`/`T-005`). |
| II. Schema Evolution via Flyway Only | **N/A** | Nenhuma mudança de schema de banco de dados. |
| III. Tiered Testing Discipline | **N/A** | Nenhum comportamento de código muda — e, distintamente de `T-002-7-2`, não há sequer comportamento de código a *não* mudar, porque a rota não existe. Nenhum teste Java é necessário. |
| IV. Events via Transactional Outbox | **N/A** | Sem relação com eventos. |
| V. Concurrency & Idempotency Invariants | **N/A** | Sem relação com estoque/reservas/idempotência — embora a operação seja sobre saldo de estoque, esta task só toca a documentação do erro, não o dado de estoque em si. |
| VI. Security & Configuration Hygiene | **N/A** | Sem mudança de segurança, secret, ou configuração. |

Nenhuma violação — **Complexity Tracking** não se aplica.

**Re-check pós Fase 1 (design)**: `data-model.md` e `quickstart.md` não introduziram nada além do já avaliado acima — todos os princípios permanecem PASS/N/A sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/014-getstockitem-404-ref/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── checklists/
│   └── requirements.md  # Spec quality checklist (/speckit-specify command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

Nenhum `contracts/` — mesma razão de `013-getproductbyid-404-ref`: a interface externa alterada é o próprio arquivo OpenAPI, sem contrato adicional a documentar.

### Source Code (repository root)

**`platform-shared-contracts` é um repositório irmão** de `hb-catalog-service` (ambos sob `hubinity/`), não um subdiretório dele — todos os caminhos abaixo assumem a raiz de `hb-catalog-service` como cwd e usam `../platform-shared-contracts` explicitamente (mesma correção aplicada em `013-getproductbyid-404-ref` após achado do `/speckit-analyze`, H1; convenção já em `hb-catalog-service/CLAUDE.md`):

```text
../platform-shared-contracts/          # repo irmão, um nível acima de hb-catalog-service/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # único arquivo alterado: content do 404 de getStockItemByProductId
```

**Structure Decision**: Nenhuma estrutura nova — uma única linha de mudança em um arquivo já existente. `hb-catalog-service` não precisa de nenhuma alteração de código; apenas reinstala a dependência (`mvn -B -DskipTests install` em `../platform-shared-contracts`) antes de recompilar.

## Complexity Tracking

*Sem violações da Constitution Check — tabela não se aplica (todos os princípios avaliados como PASS ou N/A).*
