# Tasks: Path canônico do endpoint de leitura de saldo de estoque no contrato compartilhado

**Input**: Design documents from `/specs/001-stock-balance-path/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/stock-balance-path.yaml, quickstart.md

**Tests**: Nenhum comportamento Java muda nesta feature — por Constitution Principle III, não há tier de teste de serviço aplicável. O gate de qualidade equivalente é o build do módulo de contratos (`mvn -B install`, decisão R3), tratado como tarefa de validação obrigatória (T006), mais a prova de regressão do consumidor (T008).

**Organization**: Tasks agrupadas por user story. Caminhos de arquivo são relativos ao diretório-pai comum (`.../hubinity/`), pois a feature toca dois repos irmãos.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Artefatos de feature e verificação de consumo**: `hb-catalog-service/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar pré-condições verificadas na Phase 0 (research R3/R4) antes de editar o contrato

- [x] T001 Confirmar pré-condição FR-004: `head -1 platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` retorna `openapi: 3.1.0` e `git -C platform-shared-contracts status --short contracts-catalog/openapi/catalog.yaml` está limpo (R4 — reparo já realizado; se sujo/corrompido, restaurar de HEAD antes de prosseguir)
- [x] T002 Estabelecer baseline verde: `( cd platform-shared-contracts && mvn -B -DskipTests install )` passa ANTES de qualquer edição, provando que falha posterior só pode vir da mudança desta feature

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Preparar o repo de contratos para a mudança — nenhuma edição em `main`

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Criar branch `feature/stock-balance-path` em **ambos os repos** — `git -C platform-shared-contracts checkout -b feature/stock-balance-path` e `git -C hb-catalog-service checkout -b feature/stock-balance-path` — pois os artefatos de feature deste repo (specs/, TASKS.json, CLAUDE.md) também não podem ir direto em `main` (convenção de workflow da constituição)

**Checkpoint**: Foundation ready — user story implementation can now begin

---

## Phase 3: User Story 1 - Consumidor do contrato descobre onde ler o saldo de estoque (Priority: P1) 🎯 MVP

**Goal**: O contrato compartilhado declara o path canônico `/api/v1/products/{productId}/stock`, permanecendo um documento OpenAPI 3.1 válido e estritamente aditivo

**Independent Test**: `catalog.yaml` contém a entrada `/api/v1/products/{productId}/stock` na seção `paths` e `mvn -B -DskipTests install` do repo de contratos passa (quickstart.md, passos 1–4)

### Implementation for User Story 1

- [x] T004 [US1] Inserir a entrada de path na seção `paths:` de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, após `/api/v1/products/{id}`, copiando exatamente o fragmento de `hb-catalog-service/specs/001-stock-balance-path/contracts/stock-balance-path.yaml` (sem o comentário de cabeçalho) — satisfaz FR-001, FR-002, R6
- [x] T005 [US1] Executar as verificações estáticas do quickstart (passos 1–3) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: grep de existência do path, grep de unicidade (`stock:` aparece 1×), `git diff` somente com linhas adicionadas — satisfaz FR-001, FR-003, FR-005
- [x] T006 [US1] Executar a autoridade de validação (R3): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → BUILD SUCCESS satisfaz FR-004/SC-002. Se o parse rejeitar Path Item sem operação (improvável — R2), aplicar contingência FR-006: incluir a operação GET mínima de T-001-2 no mesmo commit e registrar a decisão em `hb-catalog-service/specs/001-stock-balance-path/research.md` (adendo a R2)

**Checkpoint**: User Story 1 completa — contrato válido com o path canônico registrado; MVP entregável

---

## Phase 4: User Story 2 - Contrato e serviço convergem para o mesmo endereço (Priority: P2)

**Goal**: Evidenciar que o path registrado converge com as convenções reais do serviço e que o consumidor existente não regride

**Independent Test**: Comparação documentada path-novo × `StockController` + `mvn -B verify` verde em `hb-catalog-service` com o artefato reinstalado

### Implementation for User Story 2

- [x] T007 [P] [US2] Verificar não-ambiguidade e convergência de convenção: comparar a nova entrada de `catalog.yaml` com os mappings reais de `hb-catalog-service/src/main/java/com/hubinity/catalog/api/StockController.java` (`.../stock/movements`, `/api/v1/stock/reservations*`) confirmando que o novo path é prefixo-pai sem colisão sintática; registrar a evidência na descrição do commit/PR (US2, cenário 2)
- [x] T008 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde com o artefato `contracts-catalog:0.1.0-SNAPSHOT` reinstalado por T006 (quickstart, seção "Verificação de consumo")

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro e rastreabilidade da entrega

- [x] T009 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): declare canonical stock balance path (T-001-1)` — incluindo no corpo a evidência de T007
- [x] T010 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-001-1` (vocabulário observado no arquivo tem apenas `refined`; `done` fica adotado como valor terminal — se o time usar outro, ajustar aqui antes de executar), mantendo o restante do arquivo intacto
- [x] T011 Executar o roteiro completo de `hb-catalog-service/specs/001-stock-balance-path/quickstart.md` de ponta a ponta como validação final e marcar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [x] T012 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path` criada em T003) os artefatos da feature — `specs/001-stock-balance-path/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — com prefixo convencional, sugestão: `docs: add spec artifacts for stock balance path (T-001-1)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências — início imediato
- **Foundational (Phase 2)**: depende de T001–T002 — BLOQUEIA as user stories
- **User Stories (Phase 3–4)**: US1 depende da Phase 2; US2 depende de T006 (artefato reinstalado) — portanto sequencial após US1
- **Polish (Phase 5)**: depende de US1 + US2 completas

### User Story Dependencies

- **US1 (P1)**: independente — só exige Phases 1–2
- **US2 (P2)**: T007 é independente de US1 (só leitura de código); T008 depende de T006 (US1). US2 permanece testável de forma independente: seus critérios não exigem nenhum artefato de US2 anterior

### Within Each User Story

- US1: edição (T004) → checagens estáticas (T005) → build de validação (T006)
- US2: T007 e T008 paralelos entre si

### Parallel Opportunities

- T007 ∥ T008 (repos e arquivos distintos, sem dependência mútua)
- T007 pode inclusive começar durante a Phase 3 (não depende de nenhuma edição)

---

## Parallel Example: User Story 2

```bash
# Após T006 concluída, lançar em paralelo:
Task: "T007 — comparar catalog.yaml × StockController.java e registrar evidência"
Task: "T008 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T002): pré-condições e baseline
2. Phase 2 (T003): branch
3. Phase 3 (T004–T006): editar + validar contrato
4. **STOP and VALIDATE**: quickstart passos 1–4 — se verde, o entregável da T-001-1 está completo e a cadeia T-001-2..5 desbloqueada (SC-004)

### Incremental Delivery

1. US1 → contrato válido com path canônico (MVP — valor entregue aos 12 repos)
2. US2 → evidência de convergência + regressão zero no consumidor
3. Polish → commit convencional, TASKS.json e checklists atualizados

### Parallel Team Strategy

Feature de uma pessoa só — paralelismo relevante apenas em T007 ∥ T008.

---

## Notes

- [P] tasks = arquivos/repos diferentes, sem dependências
- Nenhuma task cria/edita código Java, migração Flyway, evento ou configuração — qualquer necessidade dessas indica fuga de escopo para T-001-2..5 / T-004-x
- Total: 12 tasks (2 setup, 1 foundational, 3 US1, 2 US2, 4 polish)
- Commits apenas na Phase 5 (T009 em `platform-shared-contracts`, T012 em `hb-catalog-service`), após todas as validações
