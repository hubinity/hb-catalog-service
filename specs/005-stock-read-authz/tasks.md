# Tasks: Requisito de autorização da operação de leitura de saldo de estoque

**Input**: Design documents from `/specs/005-stock-read-authz/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/stock-read-authz.yaml, quickstart.md

**Tests**: Nenhum comportamento Java muda — por Constitution Principle III, não há tier de teste de serviço aplicável. Gates: build do módulo (T004) + regressão do consumidor (T006).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup/foundational **herdados** da cadeia (branch `feature/stock-balance-path` ativa, T-001-4 commitada). **Última task da cadeia T-001.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fonte de verdade**: `hb-catalog-service/src/main/java/com/hubinity/catalog/config/SecurityConfig.java` (intocado)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado da cadeia antes de editar

- [X] T001 Confirmar pré-condições herdadas: branches `feature/stock-balance-path` ativas nos dois repos; working tree de `platform-shared-contracts` limpo (T-001-4 commitada); documento **sem** `securitySchemes` nem `security` hoje (`grep -c "securitySchemes\|^security:" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` retorna 0)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001 é o único gate.

**Checkpoint**: Foundation herdada confirmada — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor sabe que precisa autenticar para ler o saldo (Priority: P1) 🎯 MVP

**Goal**: Contrato declara `bearerAuth` + `security` global; operação de saldo herda o requisito; documento válido

**Independent Test**: quickstart passos 1–3 (scheme + security raiz + operação sem override) e 5 (build verde)

### Implementation for User Story 1

- [X] T002 [US1] Aplicar os 3 fragmentos de `hb-catalog-service/specs/005-stock-read-authz/contracts/stock-read-authz.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) `securitySchemes/bearerAuth` em `components` (FR-001); (b) bloco `security` no nível raiz (FR-002); (c) substituir a `description` do Path Item pelo fragmento 3 (FR-004) — sem copiar comentários
- [X] T003 [US1] Executar as verificações estáticas do quickstart (passos 1–4) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: `bearerAuth`/`scheme: bearer` presentes, `^security:` no nível raiz, operação de saldo sem `security:` próprio (FR-003), `git diff` restrito aos 3 pontos — satisfaz FR-001..FR-005
- [X] T004 [US1] Executar a autoridade de validação (herdada): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → BUILD SUCCESS (segurança não afeta geração DTO-only — research R5) — satisfaz SC-002

**Checkpoint**: User Story 1 completa — segurança declarada, contrato válido; MVP entregável

---

## Phase 4: User Story 2 - Contrato reflete o modelo real de autenticação sem exagerar o requisito (Priority: P2)

**Goal**: Requisito fiel ao serviço (autenticado, não admin), sem override na operação; regressão zero no consumidor

**Independent Test**: operação sem role/scope além de bearerAuth; comparação com SecurityConfig; `mvn -B verify` verde

### Implementation for User Story 2

- [X] T005 [P] [US2] Preparar a evidência de fidelidade para o corpo do commit (SC-003): comparar o requisito declarado (`bearerAuth` global, sem role) com `hb-catalog-service/src/main/java/com/hubinity/catalog/config/SecurityConfig.java` (`/api/**` authenticated; reads sem @PreAuthorize) — quickstart, seção "Fidelidade ao serviço"
- [X] T006 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde com o artefato reinstalado por T004 (segurança não altera DTOs; nenhuma quebra esperada)

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e fechamento da cadeia T-001

- [X] T007 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): require bearer JWT auth for catalog operations (T-001-5)` — com a evidência de T005 no corpo e nota de encerramento da cadeia T-001
- [X] T008 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-001-5`, mantendo o restante intacto
- [X] T009 Executar o roteiro completo de `hb-catalog-service/specs/005-stock-read-authz/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [X] T010 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/005-stock-read-authz/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for stock read authorization (T-001-5)`
- [X] T011 (Opcional — fechamento da cadeia) Avaliar `superpowers:finishing-a-development-branch` para a branch `feature/stock-balance-path`: com T-001-1..5 concluídas, decidir merge/PR da cadeia inteira (5 commits de contrato + artefatos de spec). Não executar merge sem confirmação do usuário

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: vazia (herdada) — T001 é o único gate
- **User Stories (Phase 3–4)**: US1 depende de T001; T005 depende de T002 (edição feita); T006 depende de T004
- **Polish (Phase 5)**: depende de US1 + US2; T011 depende de todas

### User Story Dependencies

- **US1 (P1)**: independente — só exige T001
- **US2 (P2)**: T005 usa a edição de T002; T006 usa o artefato de T004; critérios de teste próprios (independente)

### Within Each User Story

- US1: edição (T002) → checagens estáticas (T003) → build (T004)
- US2: T005 ∥ T006

### Parallel Opportunities

- T005 ∥ T006 (documentação × build em repos distintos)

---

## Parallel Example: User Story 2

```bash
# Após T004 concluída, lançar em paralelo:
Task: "T005 — evidência de fidelidade requisito × SecurityConfig"
Task: "T006 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001): confirmar herança
2. Phase 3 (T002–T004): editar + validar
3. **STOP and VALIDATE**: quickstart 1–5 — se verde, entregável da T-001-5 completo e **cadeia T-001 encerrada**

### Incremental Delivery

1. US1 → segurança declarada (MVP)
2. US2 → fidelidade + regressão zero
3. Polish → commits nos dois repos, TASKS.json, checklists e decisão de fechamento da branch

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T005 ∥ T006.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task cria/edita código Java, migração, evento ou config — necessidade dessas indica fuga de escopo (T-004-x)
- Total: 11 tasks (1 setup, 0 foundational — herdada, 3 US1, 2 US2, 5 polish incl. T011 opcional de fechamento)
- Commits apenas na Phase 5 (T007 contratos, T010 serviço), após todas as validações
- **Marco**: concluída esta feature, a cadeia T-001 (endereço→verbo→parâmetro→corpo→autorização) fica completa; próximas refined no tracker seguem para T-002-x (upload de imagens)
- T001 foi confirmada com ressalva: branches, commit anterior, arquivo-alvo limpo e baseline sem segurança estavam corretos; o repo de contratos já continha mudanças alheias em `tailwind-preset/.gitignore` e `AGENTS.md`, preservadas e excluídas de T007.
- T007 registrada no commit `68873d5`, restrito a `contracts-catalog/openapi/catalog.yaml`, com evidência de fidelidade ao `SecurityConfig` e fechamento da cadeia T-001.
- T009 concluída em 2026-07-25: verificações estáticas aprovadas, reactor Maven de 6 módulos com `BUILD SUCCESS` e consumidor com 201 testes, 0 falhas.
- T011 avaliada sem ação externa: a skill `superpowers:finishing-a-development-branch` não está instalada; os 5 commits T-001 foram confirmados e merge/PR ficou deferido por exigir confirmação explícita do usuário. Nenhum push, merge ou PR foi executado.
