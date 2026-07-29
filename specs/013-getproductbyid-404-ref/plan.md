# Implementation Plan: Referenciar ProblemDetail no 404 de getProductById

**Branch**: `013-getproductbyid-404-ref` (git branch: `feature/stock-balance-path`, execução não-escopada — sem worktree dedicada) | **Date**: 2026-07-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/013-getproductbyid-404-ref/spec.md` — `T-002-7-2` (TASKS.json).

## Summary

Referenciar o schema `ProblemDetail` (já declarado em `T-002-7-1`) no `content` do desfecho `404` da operação `getProductById`, em `contracts-catalog/openapi/catalog.yaml`. Abordagem: acréscimo pontual de um bloco `content: application/json: schema: $ref` ao desfecho existente, sem tocar em `description`, no desfecho `200`, ou em qualquer outro schema/operação do documento.

## Technical Context

**Language/Version**: YAML (OpenAPI 3.1) — nenhuma linguagem de aplicação envolvida diretamente; a mudança é só no contrato

**Primary Dependencies**: `openapi-generator-maven-plugin` (`contracts-catalog/pom.xml`, `generatorName: spring`) — gera `ProblemDetail.java` a partir do schema já existente; nenhuma dependência nova

**Storage**: N/A

**Testing**: Build do módulo `contracts-catalog` (valida OpenAPI 3.1 e confirma contagem de DTOs gerados permanece 7↔7); `mvn -B verify` no `hb-catalog-service` para confirmar regressão zero no consumidor (nenhum teste Java precisa mudar — o serviço já produz `ProblemDetail` em runtime via `ApiExceptionHandler`, independente do contrato)

**Target Platform**: N/A — mudança de contrato, sem runtime próprio

**Project Type**: Módulo de contratos compartilhados (`platform-shared-contracts/contracts-catalog`), consumido como dependência Maven pelo `hb-catalog-service`

**Performance Goals**: N/A

**Constraints**: A mudança MUST ser estritamente aditiva (nenhuma linha `-` no diff, exceto a reformatação mínima do bloco `404` para acomodar `content`); a contagem de schemas/DTOs gerados MUST permanecer 7↔7 (nenhum schema novo, nenhuma remoção)

**Scale/Scope**: Uma única modificação em um único desfecho de uma única operação — a menor unidade de mudança possível no documento

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Justificativa |
|---|---|---|
| I. Layered Architecture Boundaries | **PASS** | Mudança inteiramente no contrato compartilhado (`platform-shared-contracts`); nenhum código do `hb-catalog-service` é alterado. `ApiExceptionHandler` já produz `ProblemDetail` em runtime — o contrato apenas passa a descrever o que o código já faz. |
| II. Schema Evolution via Flyway Only | **N/A** | Nenhuma mudança de schema de banco de dados. |
| III. Tiered Testing Discipline | **N/A** | Nenhum comportamento de código muda — apenas a documentação/contrato de um erro já emitido. Nenhum teste Java novo é necessário; a verificação é o build do módulo de contratos + `mvn -B verify` de regressão. |
| IV. Events via Transactional Outbox | **N/A** | Sem relação com eventos. |
| V. Concurrency & Idempotency Invariants | **N/A** | Sem relação com estoque/reservas/idempotência. |
| VI. Security & Configuration Hygiene | **N/A** | Sem mudança de segurança, secret, ou configuração. |

Nenhuma violação — **Complexity Tracking** não se aplica.

**Re-check pós Fase 1 (design)**: `data-model.md` e `quickstart.md` não introduziram nada além do já avaliado acima — todos os princípios permanecem PASS/N/A sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/013-getproductbyid-404-ref/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── checklists/
│   └── requirements.md  # Spec quality checklist (/speckit-specify command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

Nenhum `contracts/` — a "interface externa" alterada aqui é o próprio arquivo de contrato OpenAPI (`platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`); não há um contrato adicional a documentar separadamente.

### Source Code (repository root)

Repositório de contratos compartilhados já existente (`platform-shared-contracts`), consumido pelo `hb-catalog-service` — mudança pontual em um arquivo, nenhum módulo/projeto novo. **`platform-shared-contracts` é um repositório irmão de `hb-catalog-service`** (ambos sob o diretório guarda-chuva `hubinity/`), não um subdiretório dele — a partir da raiz de `hb-catalog-service` (onde este `plan.md` e `TASKS.json` vivem), o caminho é `../platform-shared-contracts/...` (achado do `/speckit-analyze`, H1; convenção já estabelecida em `hb-catalog-service/CLAUDE.md`, seção Commands):

```text
../platform-shared-contracts/          # repo irmão, um nível acima de hb-catalog-service/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # único arquivo alterado: content do 404 de getProductById
```

**Structure Decision**: Nenhuma estrutura nova — uma única linha de mudança (bloco `content`) em um arquivo YAML já existente, no módulo de contratos já existente. `hb-catalog-service` não precisa de nenhuma alteração de código (`ApiExceptionHandler` já produz o shape que o contrato passa a descrever); apenas reinstala a dependência (`mvn -B -DskipTests install` em `../platform-shared-contracts`, a partir da raiz de `hb-catalog-service`) antes de recompilar, fluxo já estabelecido pela cadeia `T-002`.

## Complexity Tracking

*Sem violações da Constitution Check — tabela não se aplica (todos os princípios avaliados como PASS ou N/A).*
