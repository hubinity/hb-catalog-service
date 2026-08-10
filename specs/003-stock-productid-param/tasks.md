# Tasks: Especificação fina do parâmetro productId da operação de leitura de saldo

**Input**: Design documents from `/specs/003-stock-productid-param/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/productid-param.yaml, quickstart.md

**Tests**: Nenhum comportamento Java muda — por Constitution Principle III, não há tier de teste de serviço aplicável. Gate equivalente (padrão da cadeia): build do módulo de contratos (T004) + regressão do consumidor (T006).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup/foundational **herdados** da cadeia (branch `feature/stock-balance-path` ativa, T-001-2 commitada em `e32df53`).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Artefatos de feature e verificação de consumo**: `hb-catalog-service/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado da cadeia antes de editar

- [x] T001 Confirmar pré-condições herdadas: branches `feature/stock-balance-path` ativas nos dois repos (`git -C <repo> branch --show-current`); working tree de `platform-shared-contracts` limpo; operação `getStockItemByProductId` presente com bloco `parameters` da contingência (`grep -A 6 "operationId: getStockItemByProductId" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` mostra `name: productId` sem `description`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia (features 001/002). T001 é o único gate.

**Checkpoint**: Foundation herdada confirmada — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor entende o parâmetro sem ler outras operações (Priority: P1) 🎯 MVP

**Goal**: Parâmetro `productId` com os 5 campos da convenção (`description: Product UUID` adicionada) e documento válido

**Independent Test**: quickstart passos 1–2 (grep de paridade) + passo 5 (build verde)

### Implementation for User Story 1

- [x] T002 [US1] Aplicar os 2 fragmentos de `hb-catalog-service/specs/003-stock-productid-param/contracts/productid-param.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) inserir `description: Product UUID` no parâmetro da operação `getStockItemByProductId`, entre `required` e `schema` (ordem dos campos de `getProductById`); (b) substituir a `description` do Path Item pelo texto do fragmento 2 (FR-003) — sem copiar comentários
- [x] T003 [US1] Executar as verificações estáticas do quickstart (passos 1–4) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: 2 ocorrências de `description: Product UUID`, paridade campo a campo dos dois blocos `parameters` (evidência para o commit — FR-002), texto antigo `T-001-3..` ausente, `git diff` restrito à linha do parâmetro + bloco de description do Path Item — satisfaz FR-001..FR-005
- [x] T004 [US1] Executar a autoridade de validação (herdada): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → BUILD SUCCESS satisfaz FR-004/SC-002 (sem contingência prevista — research R4; falha indicaria problema externo à mudança e deve ser reportada, não contornada)

**Checkpoint**: User Story 1 completa — parâmetro em paridade, contrato válido; MVP entregável

---

## Phase 4: User Story 2 - Registro formal encerra a pendência da contingência (Priority: P2)

**Goal**: Pendência do adendo R2 (feature 002) formalmente encerrada, com evidência de ratificação preparada para o commit

**Independent Test**: description do Path Item menciona apenas T-001-4/T-001-5 como pendências; diff contém somente as duas mudanças previstas

### Implementation for User Story 2

- [x] T005 [P] [US2] Preparar a evidência de ratificação (FR-002) para o corpo do commit: comparação lado a lado dos dois blocos `parameters` (saída do quickstart passo 2) + nota de que o bloco da contingência FR-006/feature 002 foi ratificado sem alteração estrutural, encerrando a pendência do adendo R2 de `hb-catalog-service/specs/002-stock-balance-get/research.md` (que permanece intacto — SC-003)
- [x] T006 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde com o artefato `contracts-catalog:0.1.0-SNAPSHOT` reinstalado por T004

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro e rastreabilidade da entrega

- [x] T007 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): complete productId parameter spec for stock balance (T-001-3)` — com a evidência de T005 no corpo
- [x] T008 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-001-3` (valor terminal da cadeia), mantendo o restante intacto
- [x] T009 Executar o roteiro completo de `hb-catalog-service/specs/003-stock-productid-param/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [x] T010 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/003-stock-productid-param/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for productId parameter spec (T-001-3)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: vazia (herdada) — T001 é o único gate
- **User Stories (Phase 3–4)**: US1 depende de T001; T005 depende de T003 (saída do passo 2); T006 depende de T004
- **Polish (Phase 5)**: depende de US1 + US2

### User Story Dependencies

- **US1 (P1)**: independente — só exige T001
- **US2 (P2)**: T005 usa a saída de T003; T006 usa o artefato de T004; critérios de teste próprios (independente)

### Within Each User Story

- US1: edição (T002) → checagens estáticas (T003) → build (T004)
- US2: T005 ∥ T006

### Parallel Opportunities

- T005 ∥ T006 (documentação × build em repos distintos)

---

## Parallel Example: User Story 2

```bash
# Após T004 concluída, lançar em paralelo:
Task: "T005 — evidência de ratificação para o corpo do commit"
Task: "T006 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001): confirmar herança
2. Phase 3 (T002–T004): editar + validar
3. **STOP and VALIDATE**: quickstart 1–5 — se verde, entregável da T-001-3 completo, T-001-4 desbloqueada

### Incremental Delivery

1. US1 → parâmetro em paridade (MVP)
2. US2 → ratificação formal + regressão zero
3. Polish → commits nos dois repos, TASKS.json e checklists

### Parallel Team Strategy

Feature de uma pessoa — paralelismo apenas em T005 ∥ T006.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task cria/edita código Java, migração, evento ou config — necessidade dessas indica fuga de escopo (T-001-4/5, T-004-x)
- Total: 10 tasks (1 setup, 0 foundational — herdada, 3 US1, 2 US2, 4 polish)
- Commits apenas na Phase 5 (T007 contratos, T010 serviço), após todas as validações
- Lembrete registrado para o próximo ciclo (T-001-4): resolver vocabulário `StockItemResponse` (TASKS.json) × `StockItem` (contrato) — achado L2 da feature 002
- T001: branches e baseline do contrato foram confirmados; o working tree do repo irmão já continha mudanças fora do escopo (`tailwind-preset/.gitignore` e `AGENTS.md`), preservadas e excluídas do commit T007.
