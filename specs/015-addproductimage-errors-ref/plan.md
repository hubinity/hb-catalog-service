# Implementation Plan: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

**Branch**: `015-addproductimage-errors-ref` (git branch: `feature/t-002-7-product-image-errors`, worktree dedicada: `.agents/worktrees/t-002-7-product-image-errors`, execução escopada via `--task-scope T-002-7-4,T-002-7-5,T-002-7-6`) | **Date**: 2026-07-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/015-addproductimage-errors-ref/spec.md` — `T-002-7-4`, `T-002-7-5`, `T-002-7-6` (TASKS.json).

## Summary

Referenciar o schema `ProblemDetail` (já declarado em `T-002-7-1`, já usado duas vezes em `T-002-7-2`/`T-002-7-3`) no `content` dos três desfechos de erro (`400`, `403`, `404`) da operação `addProductImage`, em `contracts-catalog/openapi/catalog.yaml`. Abordagem idêntica à das tasks irmãs (`T-002-7-2`/`T-002-7-3`): acréscimo pontual de um bloco `content: application/json: schema: $ref` a cada desfecho já existente, sem tocar em nenhuma `description`, no desfecho `201`, ou em qualquer outro schema/operação. Diferença relevante: três desfechos de uma mesma operação são cobertos nesta única feature (em vez de um desfecho por feature, padrão de `013`/`014`), porque compartilham operação, schema-alvo e padrão de mudança. Assim como `getStockItemByProductId`, `addProductImage` ainda não tem implementação Java (cadeias `T-002`/`T-003`, pendentes) — a mudança é puramente contratual, sem comportamento de runtime a verificar ou regredir.

## Technical Context

**Language/Version**: YAML (OpenAPI 3.1) — nenhuma linguagem de aplicação envolvida diretamente

**Primary Dependencies**: `openapi-generator-maven-plugin` (`contracts-catalog/pom.xml`) — gera `ProblemDetail.java`, já existente desde `T-002-7-1`; nenhuma dependência nova

**Storage**: N/A

**Testing**: Build do módulo `contracts-catalog` (valida OpenAPI 3.1, confirma 7↔7 DTOs); `mvn -B verify` no `hb-catalog-service` — verde por ausência estrutural de código a regredir (a operação `addProductImage` não existe em `ProductController.java` ainda), não apenas por preservação de comportamento existente

**Target Platform**: N/A — mudança de contrato, sem runtime próprio

**Project Type**: Módulo de contratos compartilhados (`platform-shared-contracts/contracts-catalog`), consumido como dependência Maven pelo `hb-catalog-service`

**Performance Goals**: N/A

**Constraints**: A mudança MUST ser estritamente aditiva (nenhuma linha `-` no diff); a contagem de schemas/DTOs gerados MUST permanecer estável; as três `description` existentes MUST permanecer idênticas

**Scale/Scope**: Três modificações pontuais, uma por desfecho de erro, na mesma operação — escala levemente maior que as tasks irmãs (`T-002-7-2`/`T-002-7-3`, um desfecho cada), mas mesmo padrão de mudança repetido três vezes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Justificativa |
|---|---|---|
| I. Layered Architecture Boundaries | **PASS** | Mudança inteiramente no contrato compartilhado; nenhum código do `hb-catalog-service` é alterado. Assim como `T-002-7-3`, não há sequer um handler existente a descrever para nenhum dos três desfechos — o contrato roda à frente da implementação (`T-002`/`T-003`, pendentes), mesmo padrão já usado para imagens de produto e para leitura de saldo de estoque. |
| II. Schema Evolution via Flyway Only | **N/A** | Nenhuma mudança de schema de banco de dados. |
| III. Tiered Testing Discipline | **N/A** | Nenhum comportamento de código muda — e, como em `T-002-7-3`, não há sequer comportamento de código a *não* mudar, porque a operação não existe. Nenhum teste Java é necessário. |
| IV. Events via Transactional Outbox | **N/A** | Sem relação com eventos. |
| V. Concurrency & Idempotency Invariants | **N/A** | Sem relação com estoque/reservas/idempotência — `addProductImage` é sobre metadados de produto (URL de imagem), não sobre contadores de estoque. |
| VI. Security & Configuration Hygiene | **N/A** | Sem mudança de segurança, secret, ou configuração — embora o `403` documente a exigência de role `admin`, esta task só referencia o *shape* do corpo de erro, não implementa a checagem de autorização em si. |

Nenhuma violação — **Complexity Tracking** não se aplica.

**Re-check pós Fase 1 (design)**: `data-model.md` e `quickstart.md` não introduziram nada além do já avaliado acima — todos os princípios permanecem PASS/N/A sem mudança.

## Project Structure

### Documentation (this feature)

```text
specs/015-addproductimage-errors-ref/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── checklists/
│   ├── requirements.md  # Spec quality checklist (/speckit-specify command)
│   └── api.md            # Contract review checklist (/speckit-checklist command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

Nenhum `contracts/` — mesma razão de `013-getproductbyid-404-ref`/`014-getstockitem-404-ref`: a interface externa alterada é o próprio arquivo OpenAPI, sem contrato adicional a documentar.

### Source Code (repository root)

**`platform-shared-contracts` é um repositório irmão** de `hb-catalog-service` (ambos sob `hubinity/`), não um subdiretório dele — mesma relação já registrada em `hb-catalog-service/CLAUDE.md` e usada por `013`/`014`. **Correção de caminho específica desta execução escopada**: `013`/`014` rodaram na raiz de `hb-catalog-service` (onde `../platform-shared-contracts`, um nível acima, resolve corretamente); esta feature roda dentro da worktree dedicada `.agents/worktrees/t-002-7-product-image-errors`, três níveis mais funda (`hb-catalog-service/.agents/worktrees/<nome>/`) — o mesmo `../platform-shared-contracts` de um nível resolveria para um caminho inexistente dentro de `.agents/worktrees/`. Todos os caminhos abaixo assumem a raiz da worktree como cwd e usam `../../../../platform-shared-contracts` (quatro níveis: `<nome>` → `worktrees` → `.agents` → `hb-catalog-service` → `hubinity/`), verificado nesta execução antes de qualquer edição.

```text
../../../../platform-shared-contracts/          # repo irmão, quatro níveis acima a partir desta worktree
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # único arquivo alterado: content dos desfechos 400/403/404 de addProductImage
```

**Structure Decision**: Nenhuma estrutura nova — três blocos de mudança pontual em um único arquivo já existente. `hb-catalog-service` não precisa de nenhuma alteração de código; apenas reinstala a dependência (`mvn -B -DskipTests install` em `../../../../platform-shared-contracts`) antes de recompilar.

## Complexity Tracking

*Sem violações da Constitution Check — tabela não se aplica (todos os princípios avaliados como PASS ou N/A).*
