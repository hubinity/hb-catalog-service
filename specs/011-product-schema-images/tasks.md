# Tasks: Propriedade images no schema Product

**Input**: Design documents from `/specs/011-product-schema-images/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/product-images-property.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo. Há geração — `Product.java` ganha um campo —, mas **geração ≠ comportamento**: o artefato é portador de dados sem lógica e, neste repositório, **sem consumidor** (nenhum arquivo de `hb-catalog-service/src/` importa `com.hubinity.contracts`). Não existe unidade de comportamento a testar. Gates: aditividade (T005), build (T006), geração dirigida em duas camadas — quantos/quais (T009) e o quê (T010) —, conjunto fechado e `required` intacta (T011) e regressão do consumidor (T012).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup herdado da linhagem (branch `feature/stock-balance-path`; `T-002-5` concluída em `c6eaccb`). **Primeira task da série T-002 posterior ao encerramento da cadeia original** — nasceu de lacuna identificada pela spec 009 e inserida pelo usuário no tracker.

**Lição aplicada da linhagem**: o `/speckit-analyze` da 008 encontrou o defeito **O1** — fragmento aplicado numa fase de *verificação*, depois do build e da regressão, de modo que o estado entregue nunca passava por gate. Aqui **toda a escrita está em T004**, uma edição única antes de qualquer gate. Nenhuma task de T005 em diante edita o contrato.

**Novidade de gate nesta task**: a linhagem já usou *incremento* (T-002-3/T-002-4: um arquivo novo) e *inércia* (T-002-5: nada muda). Esta é a terceira forma — **alteração dirigida**: nenhum arquivo criado, exatamente um alterado. Por isso o gate se divide em **T009** (quantos e quais) e **T010** (o quê, dentro do arquivo): checksum diferente prova que algo mudou, não o quê.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmento-fonte**: `hb-catalog-service/specs/011-product-schema-images/contracts/product-images-property.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/011-product-schema-images/quickstart.md`
- **`hb-catalog-service` não tem arquivo de produção alterado** — participa apenas como medidor de regressão

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar estado herdado e capturar os **três** baselines — obrigatoriamente antes de qualquer edição

- [X] T001 Confirmar pré-condições em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: branch `feature/stock-balance-path` ativa nos dois repos; working tree limpo com HEAD em `c6eaccb`; o schema `Product` **não** tem `images` (`sed -n '/^    Product:/,/^    Category:/p' … | grep -c images` → `0`); `required` de `Product` é `[id, sku, name, price, categoryId, active]`; há 6 schemas (`grep -cE "^    [A-Z][A-Za-z]+:$" …` → `6`)
- [X] T002 **Capturar o inventário de DTOs** conforme quickstart §Gate 0, gravando caminho + `sha256sum` em `/tmp/dto-baseline-011.txt`. **Esperado: 6 linhas**. **Capturado nesta execução** — reaproveitar `/tmp/dto-baseline-010.txt` invalidaria a comparação, porque `target/` é regenerado a cada build (FR-016)
- [X] T003 [P] Confirmar que o campo **ainda não existe** no artefato gerado: `grep -c 'images' platform-shared-contracts/contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/Product.java` → `0`. Sem esta captura, T010 não conseguiria distinguir "o campo foi criado agora" de "já estava lá"
- [X] T004 [P] **Medir** o baseline de testes do consumidor conforme quickstart §Gate 0: `( cd hb-catalog-service && mvn -B verify )`, registrando a linha `Tests run:` em `/tmp/verify-before-011.log`. Medido agora, não herdado da 010 (FR-018)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da linhagem. T001–T004 são os gates.

**Checkpoint**: Estado confirmado e os três baselines capturados — user stories podem começar

---

## Phase 3: User Story 1 - Quem lê um produto recebe as imagens dele (Priority: P1) 🎯 MVP

**Goal**: O schema `Product` passa a declarar `images`, fechando a divergência apontada pela spec 009 — sem ela, o serviço devolveria (após `T-003-4`) imagens que o contrato nunca declarou.

**Independent Test**: quickstart §Gate 2 e §Gate 6 — propriedade presente com a forma exigida, `required` intacta, documento válido pelo build

### Implementation for User Story 1

- [X] T005 [US1] Aplicar o **bloco único** de `hb-catalog-service/specs/011-product-schema-images/contracts/product-images-property.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: acrescentar a propriedade `images` ao **fim** de `components/schemas/Product/properties`, após `updatedAt` — `type: array`, itens `type: string` / `format: uri` / `maxLength: 2048` e **nenhuma outra restrição**, com a `description` exigida (FR-001 a FR-005) — **sem copiar as linhas de comentário do arquivo-fonte**. `required` **não** é tocada (FR-006); as nove propriedades preexistentes **não** são tocadas (FR-007)
  > **Toda a escrita termina aqui.** T006 em diante é verificação. Aplicar qualquer coisa depois repetiria o defeito O1 da 008: o build validaria estado intermediário e o artefato medido por T009/T010 não conteria a mudança inteira.
- [X] T006 [US1] Executar o gate de **aditividade e escopo** (quickstart §Gate 1) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) `git diff -U0 … | grep '^-' | grep -v '^---'` **sem nenhuma saída** — zero remoções (FR-014); (b) `git diff -U0 … | grep '^+' | grep -v '^+++'` — **todas** as linhas exibidas devem pertencer ao bloco `images` (FR-007); (c) `git status --porcelain` sem nenhum arquivo além do `catalog.yaml`
  > **Duas metades, não uma.** O encadeamento `grep '^-' | grep -v '^---'` — e **não** `grep '^-[^-]'` — cobre remoção de linha em branco (defeito C1 da spec 010). Mas numa task **aditiva** o risco real é o oposto: uma adição fora do alvo, como `maxLength` acrescentado a `name`, não remove nada e passaria por (a), pelo build e até pelo gate de geração, que já espera `Product.java` diferente. Por isso (b) delimita **onde** as adições podem cair.
- [X] T007 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -pl contracts-catalog -am -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-015 e SC-005, e regenera `target/` para T009 e T010

**Checkpoint**: User Story 1 completa — propriedade declarada e documento válido; MVP entregável

---

## Phase 4: User Story 2 - Quem lê um produto descobre qual imagem é a principal (Priority: P2)

**Goal**: Confirmar que a `description` torna a convenção posicional utilizável por quem lê **apenas** produto e talvez nunca chame o endpoint de registro de imagem, onde ela estava escrita até agora. **Nenhuma task desta fase edita o contrato**

**Independent Test**: a `description` de `images` é inspecionável isoladamente, sem necessidade de build

### Implementation for User Story 2

- [X] T008 [P] [US2] **Verificação pura.** Inspecionar a `description` de `Product.images` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) afirma que a coleção vem **em ordem** e que o **primeiro elemento é a imagem principal** (FR-004a); (b) afirma que a propriedade pode estar **ausente ou vazia** quando não há imagem (FR-004b); (c) **remete** à estratégia URL-only declarada em `info.description` — entregue por `T-002-5` — sem redeclará-la por extenso (FR-005); (d) confrontar com `contracts/product-images-property.yaml`, já que os itens (a)–(c) são afirmações semânticas que `grep` não prova

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Gates transversais de geração e regressão, registro e encaminhamento das pendências

- [X] T009 [P] Executar o gate de **geração dirigida — quantos e quais** (quickstart §Gate 3): gerar `/tmp/dto-after-011.txt` e comparar com `/tmp/dto-baseline-011.txt`. **Esperado**: mesmos **6** nomes de arquivo (nenhum criado, nenhum removido) e **exatamente uma** linha de checksum divergente, a de `Product.java`. Qualquer outro arquivo divergente **reprova** — indica schema tocado fora do escopo (FR-016, SC-006)
- [X] T010 [P] Executar o gate de **geração dirigida — o quê** (quickstart §Gate 4): `grep -n 'images' …/generated-sources/…/dto/Product.java` deve mostrar o campo (e acessores/anotações). **Esperado**: presente, contra o `0` registrado em T003. **Reprova se o checksum mudou (T009) mas o campo não aparece** — seria prova de que a alteração não foi a pretendida (FR-017)
  > **T009 e T010 são deliberadamente separadas.** T009 prova que *algo* mudou e que foi só num arquivo; T010 prova que o que mudou é o campo esperado. Nenhuma das duas sozinha é suficiente.
- [X] T011 [P] Executar o gate de **existência, `required` intacta e conjunto fechado** (quickstart §Gate 6) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, **nesta ordem**: (a) a propriedade `images` **existe** e declara `type: array` (FR-001, FR-002); (b) `images` **não** aparece na lista `required` de `Product` (FR-006); (c) as restrições dos itens são **exatamente** as de `ProductImageResponse.images`, comprovado por `diff` literal entre os dois blocos — o que cobre de uma vez FR-003 (conjunto fechado), FR-008 (`minItems`), FR-009 (`maxItems`) e FR-010 (`readOnly`)
  > **A ordem importa, e (a) não é formalidade.** Sem a asserção positiva, a extração `sed -n '/^        images:/,…'` devolve vazio quando a propriedade não existe, nenhuma restrição proibida é encontrada e o gate **imprime OK sobre uma edição jamais aplicada** — falso positivo confirmado experimentalmente contra o arquivo pré-edição. E (c) é **equivalência**, não lista de proibidas: uma blocklist só pega as palavras-chave que alguém lembrou de listar, enquanto o `diff` contra o molde reprova qualquer acréscimo, inclusive imprevisto.
- [X] T012 [P] Provar regressão zero no consumidor (quickstart §Gate 5): `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem `Tests run:` **idêntica à medida em T004**. **Registrar no relatório que este gate é fraco por construção** — `grep -rn 'com\.hubinity\.contracts' hb-catalog-service/src/` sem saída, ou seja, ele passa porque **nada consome** a classe alterada, não porque a mudança é compatível com o uso. Relatá-lo como prova de compatibilidade seria falso (FR-018, SC-007)
- [X] T013 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): expose product image URLs on the Product schema (T-002-6)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo (a) que a propriedade é **opcional** e por quê (produto nasce sem imagem) e (b) que a adição fecha a divergência identificada pela spec 009 — o contrato passa a declarar as imagens que o serviço devolverá após `T-003-4`
- [X] T014 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-6`, mantendo o restante intacto. **Não executado pelo pipeline de especificação** — é ação da fase de implementação
- [X] T015 **Apresentar ao usuário as pendências**, extraídas de `hb-catalog-service/specs/011-product-schema-images/spec.md` (§Out of Scope) e das specs anteriores: (a) entrada proposta **`T-002-9`** — o contrato não declara `GET /api/v1/products` nem nenhum path de `/api/v1/categories`, embora `T-006-1`..`T-006-6` especifiquem ETag sobre eles; (b) divergência **`Product` (contrato) × `ProductResponse` (serviço)**, encaminhada a **ADR** e não a task, porque a decisão precede a task; (c) **`T-005-3` segue contraditória** (recepção multipart × estratégia URL-only), com substituição proposta na spec 010 e ainda pendente; (d) **`T-002-8`** (enforcement HTTPS) proposta na spec 010 e ainda não inserida. **Não editar `TASKS.json`** para nenhuma delas
- [X] T016 Executar o roteiro completo de `hb-catalog-service/specs/011-product-schema-images/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/schema.md`) conforme o resultado
- [X] T017 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/011-product-schema-images/`, `TASKS.json` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for product images property (T-002-6)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T002, T003 e T004 **devem** rodar antes de qualquer edição — capturados depois, os baselines não provam nada. T003 é o que dá sentido a T010
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–4)**: US1 depende de T001–T004; US2 depende de T005 (edição aplicada), mas **não** de T007 — é inspeção textual, dispensa build
- **Polish (Phase 5)**: T009 e T010 dependem de T007 (artefato regenerado); T012 depende de T007 (artefato instalado); T013 depende de todos os gates verdes; T014 antes de T017, para que a transição de status entre no mesmo commit dos artefatos

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup. É o MVP
- **US2 (P2)**: inspeciona a `description` aplicada em T005; critério de teste próprio, sem build

### Within Each User Story

- US1: aplicar o bloco (T005) → aditividade (T006) → build (T007)
- US2: T008 — só verificação

### Parallel Opportunities

- **T003 ∥ T004** — inspeção do artefato gerado × build do consumidor, ambas somente leitura sobre alvos distintos
- **T008** pode rodar assim que T005 concluir, sem esperar o build
- **T009 ∥ T010 ∥ T011 ∥ T012** — inventário × campo gerado × YAML × build do consumidor. Paralelizáveis com segurança porque **nenhuma edita**: toda a escrita ficou em T005

---

## Parallel Example: gates finais

```bash
# Após T007 (artefato regenerado e instalado), lançar em paralelo:
Task: "T009 — mesmos 6 nomes; exatamente 1 checksum divergente, e é Product.java"
Task: "T010 — grep 'images' em Product.java → presente (era 0 em T003)"
Task: "T011 — required intacta; nenhuma restrição além das três nos itens"
Task: "T012 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T004, com a fraqueza registrada"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T004): confirmar herança e capturar os **três** baselines
2. Phase 3 (T005–T007): aplicar o bloco + aditividade + build
3. **STOP and VALIDATE**: quickstart §Gate 1, §Gate 2 e §Gate 6 — se verde, o contrato passa a declarar as imagens do produto e a divergência da spec 009 está fechada do lado do contrato

### Incremental Delivery

1. US1 → propriedade declarada (MVP)
2. US2 → convenção posicional utilizável por quem lê apenas produto
3. Polish → gates de geração dirigida, regressão, commit de contratos, tracker, encaminhamento das pendências, quickstart e commit do serviço

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T003 ∥ T004 e em T009 ∥ T010 ∥ T011 ∥ T012.

---

## Notes

- [P] tasks = arquivos/atividades distintas, sem dependências de escrita
- **Toda a escrita no contrato está em T005.** T006–T012 são verificação. Concentração deliberada, herdada da correção do defeito O1 da 008
- Nenhuma task escreve código Java à mão; **um arquivo gerado é alterado** (`Product.java`, campo `images`), nenhum criado ou removido
- Nenhuma task toca entidade, coluna, mapper, controller ou config — isso indicaria fuga para T-003 ou T-005
- Nenhuma task adiciona `pattern`, `readOnly`, `minItems`, `maxItems` ou `minLength` — o conjunto de restrições é **fechado** por FR-003, e T011 é quem o verifica
- Nenhuma task altera `ProductImageRequest`, `ProductImageResponse` ou `info.description` — propriedade de `T-002-3`, `T-002-4` e `T-002-5`, todas `done`
- Total: **17 tasks** (4 setup, 0 foundational — herdada, 3 US1, 1 US2, 9 polish)
- Commits apenas na Phase 5 (T013 contratos, T017 serviço), após todos os gates. `TASKS.json` é atualizado em T014, **antes** do commit do serviço, para entrar junto
- **Marco**: concluída esta feature, o contrato descreve o produto **com** suas imagens, e a decisão de posicionamento de `T-002-5` (estratégia em `info.description`) passa a ter o beneficiário que a motivou
- **Quatro pendências seguem abertas**, nenhuma criada por esta task, todas encaminhadas em T015: `T-002-9` (operações de coleção ausentes), divergência `Product` × `ProductResponse` (ADR), `T-005-3` (multipart contraditória) e `T-002-8` (enforcement HTTPS)
