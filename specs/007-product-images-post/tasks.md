# Tasks: Operação POST de registro de imagem de produto

**Input**: Design documents from `/specs/007-product-images-post/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/product-images-post.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo. O Princípio III liga o tier de teste ao *comportamento alterado*, e aqui **nenhum comportamento de runtime muda** — fato **verificado empiricamente**, não assumido: com `generateApis=false` e nenhum schema novo, declarar uma operação não emite código; o diretório `catalog/api/` sequer existe em `target/generated-sources`, e os 4 DTOs correspondem 1:1 aos 4 schemas. Gates: build do módulo (T006), ausência de código gerado (T008) e regressão do consumidor (T009).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup/foundational **herdados** da cadeia (branch `feature/stock-balance-path` ativa; T-002-1 concluída no commit `fd9b905`). **Segunda task da cadeia T-002 — e a primeira mutação do contrato inteiro.**

**⚠️ Diferença crítica em relação a 006**: esta mudança **não é estritamente aditiva**. A `description` do Path Item é **substituída**, então o diff **terá** linhas removidas legítimas. O critério de aceitação mudou de "zero remoções" para "toda remoção pertence ao bloco `description:` do Path Item de imagens" (T005).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmentos-fonte**: `hb-catalog-service/specs/007-product-images-post/contracts/product-images-post.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/007-product-images-post/quickstart.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado e **medir** o baseline antes de qualquer edição

- [ ] T001 Confirmar pré-condições: branch `feature/stock-balance-path` ativa nos dois repos; working tree de `platform-shared-contracts` limpo com HEAD em `fd9b905`; o Path Item de imagens existe (`grep -c "products/{productId}/images" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` → `1`); a frase a ser removida ainda está presente (`grep -c "declared by the remaining T-002 tasks" …` → `1`); nenhuma operação de mutação existe ainda (`grep -cE "^\s+(post|put|patch|delete):" …` → `0`)
- [ ] T002 **Medir** o baseline de testes do consumidor **antes** de editar, conforme quickstart passo 1: `( cd hb-catalog-service && mvn -B verify )` e registrar a linha `Tests run:`. **Não** herdar a contagem de 005/006 — o número é medido agora e é o alvo de comparação de T009

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001 e T002 são os gates.

**Checkpoint**: Estado herdado confirmado e baseline medido — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor descobre como registrar uma imagem e o que esperar de volta (Priority: P1) 🎯 MVP

**Goal**: A operação `addProductImage` existe no contrato com identidade e três desfechos declarados; o documento segue válido e a `description` do Path Item deixa de mentir

**Independent Test**: quickstart passos 3–4 — bloco `post` presente com `201`/`403`/`404`, diff delimitado, build do módulo verde

### Implementation for User Story 1

- [ ] T003 [US1] Aplicar o **fragmento 2** de `hb-catalog-service/specs/007-product-images-post/contracts/product-images-post.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: inserir o bloco `post` **após** `parameters`, como último elemento do Path Item de imagens — **sem copiar as linhas de comentário**. Satisfaz FR-001, FR-002, FR-004, FR-005 e FR-008; e, por omissão deliberada, FR-003 (não redeclarar `productId`), FR-009 (sem `400`/`401`), FR-010 (sem `requestBody`), FR-011 (sem `security`), FR-012 (sem `Idempotency-Key`)
- [ ] T004 [US1] Aplicar o **fragmento 1**: substituir o bloco `description:` do Path Item de imagens pelo texto fixado, mantendo `summary` e `parameters` intocados — satisfaz FR-013
- [ ] T005 [US1] Executar as verificações de delimitação do quickstart passo 3 sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) inspecionar cada linha `-` do `git diff` e confirmar que **todas** pertencem ao bloco `description:` do Path Item de imagens; (b) o grep dirigido a `getProductById|getStockItemByProductId|securitySchemes|summary|parameters` entre as linhas removidas **não** retorna nada; (c) `grep -c "declared by the remaining T-002 tasks"` → `0` — satisfaz FR-014
- [ ] T006 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-015 e SC-005, e reinstala o artefato consumido por T009

**Checkpoint**: User Story 1 completa — operação declarada, documento válido e honesto; MVP entregável

---

## Phase 4: User Story 2 - Consumidor sabe, antes de integrar, que precisa de privilégio de administrador (Priority: P1)

**Goal**: A exigência de role `admin` está declarada da única forma que o contrato permite (prosa + `403`); regressão zero no consumidor

**Independent Test**: `description` da operação afirma o requisito e `403` existe com causa correta — inspecionáveis isoladamente; `mvn -B verify` verde

### Implementation for User Story 2

- [ ] T007 [P] [US2] Verificar em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` que (a) o desfecho `'403'` está declarado e sua `description` atribui a causa à **falta da role `admin`**, não a falha de autenticação (FR-006); (b) a `description` da operação afirma que a role `admin` é exigida e que um JWT válido sozinho não basta (FR-007); (c) a `description` do `201` registra a ausência de `Location` e a razão, e **não** existe bloco `headers` (FR-005)
- [ ] T008 [P] [US2] Executar o quickstart passo 5 — confirmar que `platform-shared-contracts/contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/` contém os mesmos 4 DTOs e que **não existe** diretório `.../catalog/api/`. É a evidência que sustenta o Constitution Check do Princípio III, e **mais informativa aqui do que em T-002-1**: lá o path não tinha operação; aqui declara-se uma operação real e ainda assim nada é gerado
- [ ] T009 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem de testes **idêntica à medida em T002** — satisfaz FR-016 e SC-006

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e encerramento das pendências constitucionais

- [ ] T010 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): declare product image POST operation (T-002-2)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo (a) que é a **primeira operação de mutação do contrato**, (b) o encerramento das pendências dos Princípios V e VI e (c) por que a role `admin` só pode ser expressa em prosa
- [ ] T011 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-2`, mantendo o restante intacto
- [ ] T012 Executar o roteiro completo de `hb-catalog-service/specs/007-product-images-post/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/operation.md`) conforme o resultado
- [ ] T013 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/007-product-images-post/`, `TASKS.json`, bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for product image POST operation (T-002-2)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências; T002 **deve** rodar antes de qualquer edição, senão o baseline fica contaminado
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–4)**: US1 depende de T001+T002; US2 depende de T003/T004 (edição aplicada) e, para T008/T009, de T006 (artefato construído/instalado)
- **Polish (Phase 5)**: depende de US1 + US2

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup
- **US2 (P1)**: inspeciona a edição de T003/T004 e o artefato de T006; critérios de teste próprios (independente)

### Within Each User Story

- US1: fragmento 2 (T003) → fragmento 1 (T004) → delimitação do diff (T005) → build (T006)
- US2: T007 ∥ T008 ∥ T009

### Parallel Opportunities

- T007 ∥ T008 ∥ T009 — inspeção de YAML × inspeção de `target/` × build do consumidor; atividades e repos distintos, sem dependência de escrita entre si
- T003 e T004 **não** são paralelizáveis: editam o mesmo Path Item do mesmo arquivo

---

## Parallel Example: User Story 2

```bash
# Após T006 concluída (artefato instalado), lançar em paralelo:
Task: "T007 — inspecionar 403, prosa de admin e ausência de Location"
Task: "T008 — confirmar ausência de código gerado (sem diretório api/)"
Task: "T009 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T002"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T002): confirmar herança e **medir** baseline
2. Phase 3 (T003–T006): aplicar os dois fragmentos + validar
3. **STOP and VALIDATE**: quickstart 3–4 — se verde, a operação está declarada e o contrato deixou de conter afirmação falsa

### Incremental Delivery

1. US1 → operação declarada com desfechos (MVP)
2. US2 → requisito de privilégio visível + prova de inércia + regressão zero
3. Polish → commits nos dois repos, quickstart de ponta a ponta, decisão de tracker deferida ao usuário

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T007 ∥ T008 ∥ T009.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task cria ou edita código Java, migração, entidade, DTO, mapper, evento ou config — a necessidade de qualquer uma indica fuga de escopo para T-003 (atributo `images`) ou T-005 (implementação)
- Nenhuma task declara `requestBody` (T-002-3), `content`/schema (T-002-4) ou estratégia de armazenamento (T-002-5)
- Total: **13 tasks** (2 setup, 0 foundational — herdada, 4 US1, 3 US2, 4 polish, das quais 1 é decisão deferida ao usuário)
- Commits apenas na Phase 5 (T010 contratos, T012 serviço), após todas as validações
- **Pendências constitucionais ENCERRADAS por esta feature** — nenhuma é repassada adiante:
  - **Princípio VI (autorização)**: role `admin` declarada via `403` + prosa. Encerrada porque o contrato não consegue mais do que isso — `bearerAuth` é `http`/`bearer` (sem scopes) e as roles vêm de `realm_access.roles`. A aplicação real fica com `@PreAuthorize` em T-005-2.
  - **Princípio V (idempotência)**: decidido **não** exigir `Idempotency-Key`, por evidência (`IdempotencyFilter` cobre só os 4 paths de estoque; nenhuma mutação de `ProductController` exige a chave).
- **Marco**: esta é a **primeira operação de mutação declarada no contrato**. As escolhas aqui (prosa para role, `403` declarado, sem `401`, sem idempotência em produto) viram precedente para as mutações futuras — registrado para que a repetição seja consciente
