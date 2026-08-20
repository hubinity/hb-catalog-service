---

description: "Task list template for feature implementation"
---

# Tasks: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

**Input**: Design documents from `specs/015-addproductimage-errors-ref/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md — todos completos e consistentes (checklists `requirements.md` e `api.md` resolvidas antes desta geração)

**Tests**: Não se aplica — nenhum código Java muda; `addProductImage` nem sequer tem implementação ainda (cadeias `T-002`/`T-003`, pendentes). "Tests" aqui significa validação de build do contrato, não `*Test.java`/`*IT.java`.

**Organization**: Três user stories (P1/P2/P3), uma por desfecho de erro (`400`/`403`/`404`) da mesma operação — mesmo padrão de tasks das features irmãs (`013`/`014`), repetido três vezes dentro de uma única feature.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos/comandos diferentes, sem dependência pendente)
- **[Story]**: US1 (400), US2 (403), US3 (404)

## Path Conventions

`../../../../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` — repo irmão de `hb-catalog-service`, quatro níveis acima a partir da raiz da worktree desta feature (`.agents/worktrees/t-002-7-product-image-errors`, assumida como cwd abaixo). Diferente de `013`/`014` (`../platform-shared-contracts`, um nível, rodando na raiz de `hb-catalog-service`) — a worktree fica três níveis mais funda, daí os quatro níveis aqui.

---

## Phase 1: Setup

**Não se aplica.**

## Phase 2: Foundational

**Não se aplica.** A única dependência externa à cadeia (`T-002-7-3`, done) já está satisfeita; as demais dependências (`T-002-7-4→T-002-7-5→T-002-7-6`) são internas a esta feature e resolvidas pela ordem das fases abaixo.

---

## Phase 3: User Story 1 - Consumidor sabe o formato do corpo de erro ao registrar uma imagem inválida ou malformada (Priority: P1) 🎯 MVP

**Goal**: O desfecho `400` de `addProductImage` declara `content` referenciando `ProblemDetail`, preservando a `description` existente.

**Independent Test**: `content.application/json.schema` do desfecho `400` de `addProductImage` referencia `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde (ver spec, User Story 1).

- [X] T001 [US1] Adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `400` da operação `addProductImage` em `../../../../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, preservando a `description` "Malformed request body, or url absent, not a valid URI, or too long" existente
- [X] T002 [P] [US1] Confirmar via `git -C ../../../../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml` que a mudança é estritamente aditiva (nenhuma linha `-`) — se houver remoção de conteúdo pré-existente, reverter e refazer (depends on T001)
- [X] T003 [US1] A partir da raiz da worktree, rodar `( cd ../../../../platform-shared-contracts && mvn -B -DskipTests install )` e confirmar que `find ../../../../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l` permanece estável (nenhum schema novo, nenhum removido) (depends on T001)
- [X] T004 [US1] Na raiz da worktree, rodar `mvn -B verify` e confirmar build verde — trivialmente, já que `addProductImage` não tem implementação Java ainda (depends on T003)

**Checkpoint**: US1 completa — desfecho `400` referenciando `ProblemDetail`, build verde.

---

## Phase 4: User Story 2 - Consumidor sabe o formato do corpo de erro ao tentar registrar imagem sem a role de admin (Priority: P2)

**Goal**: O desfecho `403` de `addProductImage` declara `content` referenciando `ProblemDetail`, preservando a `description` existente.

**Independent Test**: `content.application/json.schema` do desfecho `403` de `addProductImage` referencia `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde (ver spec, User Story 2).

- [X] T005 [US2] Adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `403` da operação `addProductImage` em `../../../../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, preservando a `description` "Authenticated principal lacks the required admin role" existente (depends on T004 — sequencial, mesma cadeia `depends_on` de `TASKS.json`)
- [X] T006 [P] [US2] Confirmar via `git -C ../../../../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml` que a mudança acumulada (400 + 403) é estritamente aditiva (nenhuma linha `-`) (depends on T005)
- [X] T007 [US2] A partir da raiz da worktree, rodar `( cd ../../../../platform-shared-contracts && mvn -B -DskipTests install )` e confirmar contagem de DTOs estável (depends on T005)
- [X] T008 [US2] Na raiz da worktree, rodar `mvn -B verify` e confirmar build verde (depends on T007)

**Checkpoint**: US2 completa — desfechos `400` e `403` referenciando `ProblemDetail`, build verde.

---

## Phase 5: User Story 3 - Consumidor sabe o formato do corpo de erro ao registrar imagem para um produto inexistente (Priority: P3)

**Goal**: O desfecho `404` de `addProductImage` declara `content` referenciando `ProblemDetail`, preservando a `description` existente.

**Independent Test**: `content.application/json.schema` do desfecho `404` de `addProductImage` referencia `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde; desfecho `201` da mesma operação permanece intocado (ver spec, User Story 3).

- [X] T009 [US3] Adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` da operação `addProductImage` em `../../../../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, preservando a `description` "Product not found" existente (depends on T008 — sequencial, mesma cadeia `depends_on` de `TASKS.json`)
- [X] T010 [P] [US3] Confirmar via `git -C ../../../../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml` que a mudança acumulada (400 + 403 + 404) é estritamente aditiva (nenhuma linha `-`) e que o desfecho `201` da mesma operação não aparece no diff (depends on T009)
- [X] T011 [US3] A partir da raiz da worktree, rodar `( cd ../../../../platform-shared-contracts && mvn -B -DskipTests install )` e confirmar contagem de DTOs estável (depends on T009)
- [X] T012 [US3] Na raiz da worktree, rodar `mvn -B verify` e confirmar build verde (depends on T011)

**Checkpoint**: US3 completa — todos os três desfechos de `addProductImage` (400/403/404) referenciando `ProblemDetail`, build verde. Feature pronta.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Fechar a feature com o mesmo padrão de dois commits do precedente real desta cadeia (`faf5518`, T-002-7-3: um commit no repo de contratos, um commit em `hb-catalog-service` empacotando artefatos + tracker) e atualizar o tracker — passos que a execução literal desta lista deve cobrir, não deixar implícitos.

- [X] T013 Commit em `../../../../platform-shared-contracts` contendo apenas `contracts-catalog/openapi/catalog.yaml`: `git -C ../../../../platform-shared-contracts add contracts-catalog/openapi/catalog.yaml && git -C ../../../../platform-shared-contracts commit -m "feat(contracts): reference ProblemDetail in addProductImage 400/403/404 (T-002-7-4..6)"` (depends on T012)
- [X] T014 Atualizar `TASKS.json` (raiz da worktree): alterar o campo `status` das tasks `T-002-7-4`, `T-002-7-5` e `T-002-7-6` de `"refined"` para `"done"` (depends on T013)
- [X] T015 Confirmar que o marcador `<!-- SPECKIT START -->`/`<!-- SPECKIT END -->` em `CLAUDE.md` (raiz da worktree) aponta a `specs/015-addproductimage-errors-ref/plan.md` (já atualizado durante `/speckit-plan`; este passo apenas verifica, sem exigir nova edição) (depends on T014)
- [X] T016 Commit em `hb-catalog-service` empacotando `CLAUDE.md`, `TASKS.json` e `specs/015-addproductimage-errors-ref/**` (todo o diretório da feature, incluindo este `tasks.md` com os checkboxes marcados) com mensagem `docs(spec): complete addProductImage error refs contract task` (depends on T015)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup/Foundational**: N/A
- **User Story 1 (400)**: T001 é o único passo com edição real; T002, T003, T004 são verificações dependentes de T001
- **User Story 2 (403)**: depende de US1 completa (T004) — mesma cadeia `depends_on` de `TASKS.json` (`T-002-7-5 → T-002-7-4`); T005 é a edição, T006–T008 são verificações
- **User Story 3 (404)**: depende de US2 completa (T008) — mesma cadeia `depends_on` de `TASKS.json` (`T-002-7-6 → T-002-7-5`); T009 é a edição, T010–T012 são verificações
- **Final Phase**: T013–T016 são estritamente sequenciais e dependem de US3 completa (T012) — cada commit/atualização depende do passo anterior ter sido feito com sucesso

### Parallel Opportunities

- Dentro de cada user story, a tarefa de diff ([P]) pode rodar em paralelo à tarefa de build/install, ambas após a tarefa de edição da story
- As três user stories em si NÃO são paralelizáveis entre si — editam o mesmo arquivo em sequência, seguindo a cadeia `depends_on` real de `TASKS.json`
- T013–T016 não têm oportunidade de paralelismo — são uma cadeia estrita de commit → atualizar tracker → verificar CLAUDE.md → commit final

---

## Parallel Example: User Story 1

```bash
# Após T001 (edição do YAML, bloco 400):
Task: "Confirmar diff aditivo (T002)"
Task: "Build de contracts-catalog e confirmar contagem de DTOs estável (T003)"

# Depois de T003:
Task: "mvn -B verify em hb-catalog-service (T004)"
```

O mesmo padrão se repete para US2 (T005→T006∥T007→T008) e US3 (T009→T010∥T011→T012).

---

## Implementation Strategy

### MVP First (US1 — desfecho 400)

1. T001 (edição) → T002 ∥ T003 (verificação) → T004 (regressão)
2. **PARAR e VALIDAR**: diff aditivo, build verde, contagem de DTOs estável, `mvn -B verify` verde

### Incremental Delivery

3. US2 (403): T005 → T006 ∥ T007 → T008 — mesma validação, incremental sobre US1
4. US3 (404): T009 → T010 ∥ T011 → T012 — mesma validação, incremental sobre US1+US2
5. T013 (commit do contrato) → T014 (tracker) → T015 (verificação CLAUDE.md) → T016 (commit final) — feature completa e fechada

---

## Notes

- [P] tasks = comandos/arquivos diferentes, sem dependência pendente entre si
- Nenhuma tarefa de teste Java é gerada — não se aplica; a operação nem tem implementação ainda (cadeias `T-002`/`T-003`)
- **Diferença das features irmãs (`013`/`014`)**: esta feature cobre três desfechos de uma única operação em três user stories sequenciais, em vez de uma única user story por feature — refletindo o agrupamento de `T-002-7-4/5/6` decidido em `research.md` item 3
- **T013–T016 (commits e atualização de `TASKS.json`/`CLAUDE.md`) existem porque esta lista é executada por um agente que segue cada tarefa estritamente como descrita** — deixar esses passos implícitos significaria que ninguém os executa. Mensagens de commit e o padrão de dois commits (contrato / hb-catalog-service) seguem o precedente real já registrado no histórico desta cadeia (commit `faf5518`, T-002-7-3; commit `29a9570`, T-002-7-2)
- **Execução escopada**: esta feature roda em uma worktree dedicada (`.agents/worktrees/t-002-7-product-image-errors`, branch `feature/t-002-7-product-image-errors`) — os commits T013/T016 acontecem nessa worktree, não no checkout principal; integrar de volta (merge) é decisão do usuário, fora do escopo desta lista
