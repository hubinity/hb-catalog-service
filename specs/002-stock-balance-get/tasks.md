# Tasks: Operação GET no path canônico de leitura de saldo de estoque

**Input**: Design documents from `/specs/002-stock-balance-get/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/stock-balance-get.yaml, quickstart.md

**Tests**: Nenhum comportamento Java muda — por Constitution Principle III, não há tier de teste de serviço aplicável. Gate equivalente (herdado da feature 001, R3): build do módulo de contratos como validação obrigatória (T004) + prova de regressão do consumidor (T007).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`), pois a feature toca dois repos irmãos. Setup/foundational da feature 001 **herdados** (branch `feature/stock-balance-path` já ativa nos dois repos — não recriar).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Artefatos de feature e verificação de consumo**: `hb-catalog-service/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado da feature 001 antes de editar

- [x] T001 Confirmar pré-condições herdadas: `git -C platform-shared-contracts branch --show-current` e `git -C hb-catalog-service branch --show-current` retornam `feature/stock-balance-path`; `grep -c "/api/v1/products/{productId}/stock:" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` retorna 1 (T-001-1 entregue); working tree de `platform-shared-contracts` limpo (`git -C platform-shared-contracts status --short` vazio — commit de T-001-1 realizado)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — branch, baseline e autoridade de validação herdados da feature 001 (T002/T003/T006 daquela entrega). Fase intencionalmente vazia; T001 é o único gate.

**Checkpoint**: Foundation herdada confirmada — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor descobre como ler o saldo (verbo e desfechos) (Priority: P1) 🎯 MVP

**Goal**: O Path Item do saldo declara a operação `get` com identidade completa e desfechos `'200'`/`'404'` (404 único), e o documento permanece válido

**Independent Test**: verificações estáticas do quickstart (passos 1–4: greps de operação/tag/textos + diff restrito) passam; `mvn -B -DskipTests install` do repo de contratos verde (quickstart, passo 5)

### Implementation for User Story 1

- [x] T002 [US1] Aplicar os 2 fragmentos de `hb-catalog-service/specs/002-stock-balance-get/contracts/stock-balance-get.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) tag `stock` após a tag `products` na seção `tags`; (b) substituir o bloco do Path Item `/api/v1/products/{productId}/stock` pelo Path Item completo do fragmento 2 (description reescrita FR-007 + operação `get` FR-001/002/003/004) — sem copiar comentários
- [x] T003 [US1] Executar as verificações estáticas do quickstart (passos 1–4) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: grep de `operationId`, tag declarada+usada, textos das respostas, e `git diff` restrito ao Path Item do saldo + seção `tags` — satisfaz FR-001..FR-005 e FR-007
- [x] T004 [US1] Executar a autoridade de validação (herdada, R6): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → BUILD SUCCESS satisfaz FR-005/SC-002. **Se falhar com erro de parâmetro de path não declarado** (gatilho objetivo em research R2), aplicar a contingência FR-006: descomentar o bloco `parameters` do fragmento em `hb-catalog-service/specs/002-stock-balance-get/contracts/stock-balance-get.yaml` dentro da operação, re-rodar o build e registrar o acionamento como adendo a R2 em `hb-catalog-service/specs/002-stock-balance-get/research.md`

**Checkpoint**: User Story 1 completa — operação declarada, contrato válido; MVP entregável

---

## Phase 4: User Story 2 - Contrato permanece coerente com as convenções existentes (Priority: P2)

**Goal**: Evidenciar coerência da operação nova com `getProductById` e regressão zero no consumidor

**Independent Test**: Comparação lado a lado documentada + `mvn -B verify` verde em `hb-catalog-service`

### Implementation for User Story 2

- [x] T005 [P] [US2] Verificar coerência de convenção (SC-003): comparar lado a lado, em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, a operação nova e `getProductById` — formato de `operationId` (`get<Recurso>By<Chave>`), summary imperativo, tag declarada na seção `tags`, códigos de resposta entre aspas simples com `description` — e registrar a evidência para o corpo do commit (US2, cenários 1–2)
- [x] T006 [P] [US2] Confirmar que a seção `tags` tem exatamente 2 entradas (`products`, `stock`), ambas usadas por ≥1 operação, e que `components/schemas` está byte-a-byte intacto (`git diff -- contracts-catalog/openapi/catalog.yaml` sem hunks em `components:`) — invariantes 2–3 de data-model.md
- [x] T007 [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde com o artefato `contracts-catalog:0.1.0-SNAPSHOT` reinstalado por T004 (quickstart, "Verificação de consumo")

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro e rastreabilidade da entrega

- [x] T008 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): declare GET operation for stock balance path (T-001-2)` — incluindo no corpo a evidência de T005 e, se acionada, a nota da contingência FR-006
- [x] T009 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-001-2` (mesmo valor terminal adotado para T-001-1), mantendo o restante do arquivo intacto
- [x] T010 Executar o roteiro completo de `hb-catalog-service/specs/002-stock-balance-get/quickstart.md` de ponta a ponta como validação final e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [x] T011 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/002-stock-balance-get/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for stock balance GET operation (T-001-2)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — início imediato
- **Foundational (Phase 2)**: vazia (herdada) — T001 é o único gate
- **User Stories (Phase 3–4)**: US1 depende de T001; T005/T006 dependem de T002 (edição feita); T007 depende de T004 (artefato reinstalado)
- **Polish (Phase 5)**: depende de US1 + US2 completas

### User Story Dependencies

- **US1 (P1)**: independente — só exige T001
- **US2 (P2)**: T005/T006 dependem da edição de US1 (T002), T007 do build (T004); permanece testável de forma independente (critérios próprios: comparação documentada + verify verde)

### Within Each User Story

- US1: edição (T002) → checagens estáticas (T003) → build de validação com contingência (T004)
- US2: T005 ∥ T006 (leitura/inspeção) → T007 (após T004)

### Parallel Opportunities

- T005 ∥ T006 (inspeções independentes no mesmo arquivo, sem escrita)
- T007 pode rodar em paralelo com T005/T006 (repo distinto), desde que T004 concluída

---

## Parallel Example: User Story 2

```bash
# Após T004 concluída, lançar em paralelo:
Task: "T005 — comparação de convenção getStockItemByProductId × getProductById"
Task: "T006 — invariantes de tags e components intactos (git diff)"
Task: "T007 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001): confirmar herança
2. Phase 3 (T002–T004): editar + validar (contingência FR-006 embutida em T004)
3. **STOP and VALIDATE**: quickstart passos 1–5 — se verde, o entregável da T-001-2 está completo e T-001-3 desbloqueada (SC-004)

### Incremental Delivery

1. US1 → operação declarada, contrato válido (MVP)
2. US2 → evidência de coerência + regressão zero
3. Polish → commits convencionais nos dois repos, TASKS.json e checklists atualizados

### Parallel Team Strategy

Feature de uma pessoa só — paralelismo relevante em T005 ∥ T006 ∥ T007.

---

## Notes

- [P] tasks = inspeções/repos distintos, sem dependências de escrita
- Nenhuma task cria/edita código Java, migração Flyway, evento ou configuração — necessidade dessas indica fuga de escopo para T-001-3/4/5 ou T-004-x
- Total: 11 tasks (1 setup, 0 foundational — herdada, 3 US1, 3 US2, 4 polish)
- Commits apenas na Phase 5 (T008 em `platform-shared-contracts`, T011 em `hb-catalog-service`), após todas as validações
- Se a contingência FR-006 disparar em T004, o commit T008 deve mencionar explicitamente o parâmetro mínimo incluído e que T-001-3 segue dona da especificação fina
