# Tasks: Schema de resposta da operação de leitura de saldo de estoque

**Input**: Design documents from `/specs/004-stock-item-schema/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/stock-item-schema.yaml, quickstart.md

**Tests**: Nenhum comportamento Java muda — por Constitution Principle III, não há tier de teste de serviço aplicável. Gates: build do módulo (T005) + **verificação de consumo obrigatória** (T007 — FR-006, breaking change no schema).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup/foundational **herdados** da cadeia (branch `feature/stock-balance-path` ativa, T-001-3 commitada em `4ddfd2c`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fonte de verdade dos campos**: `hb-catalog-service/src/main/java/com/hubinity/catalog/api/dto/StockItemResponse.java` (intocado)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado da cadeia e a pré-condição da breaking change

- [x] T001 Confirmar pré-condições herdadas: branches `feature/stock-balance-path` ativas nos dois repos; working tree de `platform-shared-contracts` limpo (T-001-3 commitada); resposta `'200'` da operação `getStockItemByProductId` ainda **sem** `content` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`
- [x] T002 Confirmar segurança da breaking change (pré-condição de FR-006): `grep -rn "com.hubinity.contracts.catalog.dto.StockItem" hb-catalog-service/src/` retorna vazio (nenhum código do serviço referencia o DTO gerado atual)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001–T002 são os gates.

**Checkpoint**: Foundation herdada confirmada — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor sabe exatamente o que a leitura de saldo retorna (Priority: P1) 🎯 MVP

**Goal**: Schema `StockItem` em paridade com o record real do serviço, referenciado pelo `content` da `'200'`; documento válido e DTO regenerado

**Independent Test**: quickstart passos 1–2 (ref + campos novos) e 5 (build verde + DTO com campos novos)

### Implementation for User Story 1

- [x] T003 [US1] Aplicar os 3 fragmentos de `hb-catalog-service/specs/004-stock-item-schema/contracts/stock-item-schema.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) substituir o bloco `components/schemas/StockItem` pelo fragmento 1 (5 campos reais — FR-001); (b) substituir o bloco `'200'` da operação `getStockItemByProductId` pelo fragmento 2 (`content` + `$ref` — FR-002); (c) substituir a `description` do Path Item pelo fragmento 3 (FR-004) — sem copiar comentários
- [x] T004 [US1] Executar as verificações estáticas do quickstart (passos 1–4) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: `$ref` presente na `'200'`, 4 campos novos presentes, 0 ocorrências de `quantityOnHand|reorderLevel|lastMovementAt`, `git diff` restrito aos 3 blocos — satisfaz FR-001..FR-005
- [x] T005 [US1] Executar a autoridade de validação (herdada): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → BUILD SUCCESS; confirmar DTO regenerado com campos novos (`grep available platform-shared-contracts/contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/StockItem.java`) — satisfaz SC-002 (falha do build indicaria problema externo; reportar, não contornar)

**Checkpoint**: User Story 1 completa — schema fiel, '200' completa, DTO regenerado; MVP entregável

---

## Phase 4: User Story 2 - Divergência histórica do schema encerrada sem resíduos (Priority: P2)

**Goal**: Campos especulativos eliminados sem resíduo, regressão zero comprovada no consumidor, description do Path Item apontando só T-001-5

**Independent Test**: grep dos nomes antigos = 0; `mvn -B verify` verde em `hb-catalog-service`; description menciona apenas T-001-5

### Implementation for User Story 2

- [x] T006 [P] [US2] Preparar a evidência de paridade para o corpo do commit (SC-001): comparação lado a lado do schema reescrito × `hb-catalog-service/src/main/java/com/hubinity/catalog/api/dto/StockItemResponse.java` (quickstart, seção "Comparação de paridade") + nota do encerramento da divergência da feature 001 e do achado L2 da feature 002
- [x] T007 [P] [US2] **FR-006 (obrigatória)**: provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde com o artefato reinstalado por T005. Se falhar por referência ao DTO antigo (improvável — T002 verificou ausência), corrigir o ponto de uso **nesta entrega** e registrar em research.md (adendo a R3)

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro e rastreabilidade da entrega

- [x] T008 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts)!: align StockItem schema with service and complete stock balance 200 response (T-001-4)` (marcador `!` de breaking change) — com a evidência de T006 no corpo
- [x] T009 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-001-4`, mantendo o restante intacto
- [x] T010 Executar o roteiro completo de `hb-catalog-service/specs/004-stock-item-schema/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [x] T011 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/004-stock-item-schema/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for stock item response schema (T-001-4)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; T001 ∥ T002
- **Foundational (Phase 2)**: vazia (herdada) — T001–T002 são os gates
- **User Stories (Phase 3–4)**: US1 depende de T001–T002; T006 depende de T003; T007 depende de T005
- **Polish (Phase 5)**: depende de US1 + US2

### User Story Dependencies

- **US1 (P1)**: independente — só exige os gates da Phase 1
- **US2 (P2)**: T006 usa a edição de T003; T007 usa o artefato de T005; critérios de teste próprios (independente)

### Within Each User Story

- US1: edição (T003) → checagens estáticas (T004) → build + DTO (T005)
- US2: T006 ∥ T007

### Parallel Opportunities

- T001 ∥ T002 (inspeções em repos distintos)
- T006 ∥ T007 (documentação × build em repos distintos)

---

## Parallel Example: User Story 2

```bash
# Após T005 concluída, lançar em paralelo:
Task: "T006 — evidência de paridade schema × record para o commit"
Task: "T007 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T002): gates de herança + segurança da breaking change
2. Phase 3 (T003–T005): editar + validar + DTO regenerado
3. **STOP and VALIDATE**: quickstart 1–5 — se verde, entregável da T-001-4 completo

### Incremental Delivery

1. US1 → schema fiel + '200' completa (MVP)
2. US2 → resíduo zero + regressão zero (FR-006)
3. Polish → commits nos dois repos (contrato com marcador `!`), TASKS.json e checklists

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T001 ∥ T002 e T006 ∥ T007.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task cria/edita código Java, migração, evento ou config — necessidade dessas (fora do fallback de T007) indica fuga de escopo (T-001-5, T-004-x)
- Total: 11 tasks (2 setup, 0 foundational — herdada, 3 US1, 2 US2, 4 polish)
- Commits apenas na Phase 5 (T008 contratos com `feat!`, T011 serviço), após todas as validações
- Próximo ciclo (T-001-5): autorização da operação — última pendência estrutural; considerar convenção `bearerAuth`/securitySchemes inexistente no contrato hoje (decisão nova, sem precedente interno)
- T001 foi confirmada com ressalva: os dois repos estavam na branch esperada, a `'200'` ainda não possuía `content` e o arquivo-alvo não tinha diff; o repo de contratos já continha mudanças alheias em `tailwind-preset/.gitignore` e `AGENTS.md`, preservadas e excluídas do commit.
- T008 registrada no commit `8e0f218`, restrito a `contracts-catalog/openapi/catalog.yaml`, com evidência de paridade e marcador de breaking change.
- T010 concluída em 2026-07-23: verificações estáticas aprovadas, reactor Maven de 6 módulos com `BUILD SUCCESS`, DTO regenerado consistente e consumidor com 201 testes, 0 falhas.
