# Tasks: Path do endpoint de imagens de produto

**Input**: Design documents from `/specs/006-product-images-path/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/product-images-path.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo. Por Constitution Principle III o tier de teste segue o comportamento alterado — e aqui **nenhum comportamento de runtime muda**, fato verificável: o pom pai fixa `generateApis=false` e esta task não adiciona schema, logo **nenhuma classe Java é gerada** a partir do novo Path Item (research R5). Gates aplicáveis: build do módulo (T004), ausência de código gerado novo (T006) e regressão do consumidor (T007).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup/foundational **herdados** da cadeia (branch `feature/stock-balance-path` ativa, cadeia T-001 encerrada em 005). **Primeira task da cadeia T-002.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmento-fonte**: `hb-catalog-service/specs/006-product-images-path/contracts/product-images-path.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/006-product-images-path/quickstart.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar o estado herdado da cadeia antes de editar

- [X] T001 Confirmar pré-condições herdadas: branch `feature/stock-balance-path` ativa nos dois repos; working tree de `platform-shared-contracts` limpo (cadeia T-001 commitada, último commit de contrato `68873d5`); baseline **sem** o path de imagens — `grep -c "products/{productId}/images" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` retorna `0`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001 é o único gate.

**Checkpoint**: Foundation herdada confirmada — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor localiza o endereço canônico das imagens de um produto (Priority: P1) 🎯 MVP

**Goal**: O contrato declara o Path Item `/api/v1/products/{productId}/images`, com a semântica URL-only explícita na `description`, e permanece um documento válido

**Independent Test**: quickstart passos 1–3 — path presente, `description` declara URL-only, diff estritamente aditivo, build do módulo verde

### Implementation for User Story 1

- [X] T002 [US1] Inserir o bloco de `hb-catalog-service/specs/006-product-images-path/contracts/product-images-path.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, sob `paths:`, imediatamente após o Path Item `/api/v1/products/{productId}/stock` e antes de `components:` — **sem copiar as linhas de comentário do fragmento** — satisfaz FR-001 e FR-004
- [X] T003 [US1] Executar as verificações estáticas do quickstart (passos 2 e 6) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) `git diff -U0 … | grep '^-' | grep -v '^---'` **sem nenhuma saída** — prova de aditividade estrita (FR-007); (b) o Path Item não contém `post:`/`get:`/`requestBody:`/`responses:` (FR-006), nem `tags:` (FR-010), nem `security:` próprio (FR-005); (c) a `description` menciona a estratégia URL-only e as tasks restantes de T-002 (FR-004)
- [X] T004 [US1] Executar a autoridade de validação (herdada): `( cd platform-shared-contracts && mvn -B -DskipTests install )` → `BUILD SUCCESS` — prova FR-008 e SC-002, e reinstala o artefato consumido por T007

**Checkpoint**: User Story 1 completa — endereço declarado e documento válido; MVP entregável

---

## Phase 4: User Story 2 - O parâmetro de identificação do produto é declarado uma única vez para o path (Priority: P2)

**Goal**: `productId` declarado no nível do Path Item, casando com o template, herdável pelas operações futuras; regressão zero no consumidor

**Independent Test**: bloco `parameters` inspecionável isoladamente (sem operação); `mvn -B verify` verde no consumidor

### Implementation for User Story 2

- [X] T005 [P] [US2] Verificar em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` que o bloco `parameters` está no **nível do Path Item** (não dentro de uma operação) e contém `productId` com `in: path`, `required: true`, `description` e `schema` `type: string` / `format: uuid` (FR-002), e que o nome do parâmetro é idêntico ao token `{productId}` do endereço (FR-003)
- [X] T006 [P] [US2] Executar o quickstart passo 4 — listar `platform-shared-contracts/contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/` e confirmar que os modelos são **exatamente os mesmos de antes** (nenhum artefato novo, nenhum relacionado a imagens): é a evidência que sustenta o Constitution Check do Princípio III
- [X] T007 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde, com a mesma contagem de testes do baseline (201 testes, 0 falhas, conforme registrado em 005) — prova FR-009 e SC-005

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e abertura formal da cadeia T-002

- [X] T008 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): add product images path (T-002-1)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo a premissa URL-only e sua motivação (CDN é PRD §12, pós-MVP)
- [X] T009 Executar o roteiro completo de `hb-catalog-service/specs/006-product-images-path/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/api.md`) conforme o resultado
- [X] T010 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/006-product-images-path/` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for product images path (T-002-1)`
- [ ] T011 **Decisão do usuário — não executar sem confirmação**: atualizar `hb-catalog-service/TASKS.json` marcando `T-002-1` como concluída. O pipeline deixou o tracker intocado de propósito; a transição de status é do usuário
- [ ] T012 **Decisão do usuário — não executar sem confirmação**: reavaliar `T-002-3` no tracker. Sob a decisão URL-only ela está **invalidada como redigida** (especifica requisição *multipart*, que deixou de existir) e precisa ser reescrita ou descartada antes de chegar à sua vez na fila

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: sem dependências
- **Foundational (Phase 2)**: vazia (herdada) — T001 é o único gate
- **User Stories (Phase 3–4)**: US1 depende de T001; US2 depende de T002 (edição aplicada) e, para T006/T007, de T004 (artefato construído/instalado)
- **Polish (Phase 5)**: depende de US1 + US2

### User Story Dependencies

- **US1 (P1)**: independente — só exige T001
- **US2 (P2)**: inspeciona a edição de T002 e o artefato de T004; critérios de teste próprios (independente)

### Within Each User Story

- US1: edição (T002) → checagens estáticas (T003) → build (T004)
- US2: T005 ∥ T006 ∥ T007

### Parallel Opportunities

- T005 ∥ T006 ∥ T007 — inspeção de YAML × inspeção de `target/` × build do consumidor; atividades e repos distintos, sem dependência de escrita entre si

---

## Parallel Example: User Story 2

```bash
# Após T004 concluída (artefato instalado), lançar em paralelo:
Task: "T005 — inspecionar bloco parameters no nível do Path Item"
Task: "T006 — confirmar ausência de código gerado novo em target/generated-sources"
Task: "T007 — ( cd hb-catalog-service && mvn -B verify )"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001): confirmar herança
2. Phase 3 (T002–T004): editar + validar
3. **STOP and VALIDATE**: quickstart 1–3 — se verde, o entregável da T-002-1 está completo e a cadeia T-002 está **aberta**

### Incremental Delivery

1. US1 → endereço declarado com semântica URL-only (MVP)
2. US2 → parâmetro herdável + prova de inércia (nenhum código gerado) + regressão zero
3. Polish → commits nos dois repos, quickstart de ponta a ponta, e as duas decisões de tracker deferidas ao usuário

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T005 ∥ T006 ∥ T007.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task cria ou edita código Java, migração, entidade, DTO, mapper, evento ou config — a necessidade de qualquer uma dessas indica fuga de escopo para as cadeias T-003 (atributo `images`) ou T-005 (implementação)
- Nenhuma task declara operação, `requestBody` ou schema de resposta — são T-002-2, T-002-3 e T-002-4
- Total: **12 tasks** (1 setup, 0 foundational — herdada, 3 US1, 3 US2, 5 polish, das quais 2 são decisões deferidas ao usuário)
- Commits apenas na Phase 5 (T008 contratos, T010 serviço), após todas as validações
- **Marco**: concluída esta feature, a cadeia T-002 fica aberta — T-002-2 pode declarar a operação POST sobre um Path Item existente com o parâmetro já resolvido
- **Pendência viva registrada (Princípio VI — autorização)**: o Princípio VI exige `hasRole('admin')` em endpoints de mutação. Este Path Item não é lugar para expressar role, mas as operações futuras **são** mutações — a obrigação recai sobre T-002-2 (contrato) e a cadeia T-005 (serviço). Ver Edge Cases da spec e o Constitution Check do plan.
- **Pendência viva registrada (Princípio V — idempotência)**: o Princípio V torna `Idempotency-Key` obrigatório nos POST mutantes **de estoque** (movements, reserve, release, commit). As operações futuras deste path são POST mutantes, mas de **produto**, não de estoque — a exigência literal não se aplica automaticamente. Exigir ou dispensar a chave aqui é **decisão consciente a registrar em T-002-2**, não um silêncio. Ver o Constitution Check do plan (linha do Princípio V).
