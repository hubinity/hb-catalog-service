# Tasks: Corpo de requisição JSON do registro de imagem de produto

**Input**: Design documents from `/specs/008-product-image-request-body/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/product-image-request.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo — mas **o argumento mudou**. T-002-1 e T-002-2 dispensaram testes alegando que *nada era gerado*; aqui **há geração**: `ProductImageRequest` vira DTO (4 → 5 modelos). Geração, porém, não é comportamento: o artefato é um portador de dados sem lógica, que o serviço **sequer referencia** (isso é a cadeia T-005). Não há unidade de comportamento a testar em `hb-catalog-service`. O que muda é o **peso dos gates**: comparação de inventário de DTOs (T008) e regressão do consumidor (T009) deixam de ser formalidade.

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup herdado da cadeia (branch `feature/stock-balance-path`; T-002-2 concluída em `854c02f`). **Terceira task da cadeia T-002 — e a primeira que gera código.**

**⚠️ Duas diferenças em relação a 007** — não replicar aquele tasks.md por inércia:

1. **Volta ao critério ADITIVO.** A 007 substituía a `description` do Path Item, então o diff tinha remoções legítimas. Aqui **não há nenhuma**: o critério volta ao de T-002-1 — `grep '^-'` sem saída (T005).
2. **Dois baselines obrigatórios antes de editar** (T002, T003), não um. O inventário de DTOs com checksum é o que torna o FR-014 verificável; sem ele, "nenhum preexistente alterado" seria inverificável, porque `target/` é regenerado a cada build.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmentos-fonte**: `hb-catalog-service/specs/008-product-image-request-body/contracts/product-image-request.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/008-product-image-request-body/quickstart.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar estado herdado e **capturar os dois baselines** — obrigatoriamente antes de qualquer edição

- [X] T001 Confirmar pré-condições: branch `feature/stock-balance-path` ativa nos dois repos; working tree de `platform-shared-contracts` limpo com HEAD em `854c02f`; a operação existe (`grep -c "addProductImage" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` → `1`); ainda não há corpo de requisição (`grep -c "requestBody" …` → `0`); há 4 schemas (`grep -cE "^    [A-Z][A-Za-z]+:$" …` → `4`)
- [X] T002 **Capturar o inventário de DTOs gerados** conforme quickstart passo 1a: rodar `( cd platform-shared-contracts && mvn -B -DskipTests install )` para garantir `target/` no estado anterior, depois gravar nomes + `md5sum` em `/tmp/dto-baseline.txt`. **Esperado: 4 linhas** (`Category`, `Product`, `StockItem`, `StockMovement`). Sem esta captura o FR-014 é **inverificável** — `target/` é regenerado a cada build
- [X] T003 **Medir** o baseline de testes do consumidor conforme quickstart passo 1b: `( cd hb-catalog-service && mvn -B verify )`, registrando a linha `Tests run:`. Medido agora, não herdado de specs anteriores

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001–T003 são os gates.

**Checkpoint**: Estado confirmado e ambos os baselines capturados — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor sabe exatamente o que enviar para registrar uma imagem (Priority: P1) 🎯 MVP

**Goal**: A operação passa a ter corpo declarado — `requestBody` obrigatório apontando para `ProductImageRequest`, que existe com a única propriedade `url`. A edição é aplicada **por inteiro** aqui (os três fragmentos), de modo que todos os gates a seguir incidam sobre o estado final.

**Independent Test**: quickstart passos 3–4 — `requestBody`, `400` e schema presentes, diff sem remoções, build do módulo verde

### Implementation for User Story 1

- [X] T004 [US1] Aplicar os **três fragmentos** de `hb-catalog-service/specs/008-product-image-request-body/contracts/product-image-request.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, numa **única edição atômica**: (a) `requestBody` na operação `post`, entre a `description` da operação e `responses` (FR-001, FR-002, FR-003); (b) desfecho `'400'` em `responses`, antes de `'403'`, **sem `content`** (FR-008, FR-009); (c) schema `ProductImageRequest` anexado ao fim de `components/schemas`, após `StockMovement` (FR-004, FR-005, FR-006) — **sem copiar as linhas de comentário**
  > **Os três juntos, obrigatoriamente.** Aplicar qualquer fragmento depois de T005/T006 faria os gates rodarem sobre um estado intermediário: o build validaria um documento incompleto e o artefato instalado — consumido por T009 — não conteria a mudança inteira. Todas as verificações a partir daqui incidem sobre o estado **final**.
- [X] T005 [US1] Executar as verificações de aditividade do quickstart passo 3: (a) `git diff -U0 … | grep '^-' | grep -v '^---'` **sem nenhuma saída** — zero remoções (FR-012); (b) `grep -c "are completed by" …` → `1`, confirmando que a `description` do Path Item seguiu **intacta**; (c) `grep -c "multipart" …` → `0` e o `content` declara apenas `application/json` (FR-001)
- [X] T006 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-013 e SC-005, e regenera `target/` para T008

**Checkpoint**: User Story 1 completa — corpo declarado e documento válido; MVP entregável

---

## Phase 4: User Story 2 - Consumidor conhece os limites da URL e o desfecho de payload inválido (Priority: P2)

**Goal**: Confirmar que os limites da URL e o desfecho `400` (declarados em T004) estão corretos; geração de código previsível; regressão zero. **Nenhuma task desta fase edita o contrato** — são todas verificação

**Independent Test**: constraints de `url` e o `400` inspecionáveis isoladamente; inventário de DTOs com exatamente um nome novo; `mvn -B verify` verde

### Implementation for User Story 2

- [X] T007 [P] [US2] **Verificação pura — não edita nada.** Inspecionar `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) o desfecho `'400'` (aplicado em T004) nomeia as quatro causas (FR-008) e não tem `content` (FR-009); (b) `url` declara `format: uri`, `minLength: 1`, `maxLength: 2048` (FR-006); (c) **não** há `pattern` (FR-007), **não** há `additionalProperties: false` (FR-016), **não** há `'409'` (FR-010), e o corpo não é array (FR-011); (d) o `content` do `requestBody` usa `$ref` e não schema inline (FR-002), e o `requestBody` está entre a `description` da operação e `responses` (FR-003)
- [X] T008 [P] [US2] **Gate novo desta cadeia** — executar o quickstart passo 5: gerar `/tmp/dto-after.txt` e `diff /tmp/dto-baseline.txt /tmp/dto-after.txt`. **Esperado**: exatamente **uma linha adicionada**, `ProductImageRequest.java`; os checksums dos 4 modelos preexistentes **idênticos** ao baseline de T002; total de 5 DTOs; nenhum diretório `catalog/api/` — satisfaz FR-014
- [X] T009 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem **idêntica à medida em T003** — satisfaz FR-015 e SC-007. Aqui o gate **deixa de ser formalidade**: confirma que a classe gerada compila e não colide com nada existente

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e encerramento da obrigação herdada

- [X] T010 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): add product image request body (T-002-3)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo (a) que é o **primeiro `requestBody` do contrato** e o primeiro artefato da cadeia a gerar código, (b) o **encerramento** da obrigação do `400` herdada de T-002-2, (c) a **realocação** da questão do `409` para T-003 — com motivo e destinatário novos, não redeferimento
- [X] T011 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-3`, mantendo o restante intacto
- [X] T012 Executar o roteiro completo de `hb-catalog-service/specs/008-product-image-request-body/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/request-body.md`) conforme o resultado
- [X] T013 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/008-product-image-request-body/`, `TASKS.json` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for product image request body (T-002-3)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T002 e T003 **devem** rodar antes de qualquer edição — capturado depois, qualquer baseline é inútil
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–4)**: US1 depende de T001–T003; US2 depende de T004 (edição aplicada) e, para T008/T009, de T006 (artefato reconstruído/instalado)
- **Polish (Phase 5)**: depende de US1 + US2

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup
- **US2 (P2)**: inspeciona a edição de T004/T007 e o artefato de T006; critérios de teste próprios

### Within Each User Story

- US1: aplicar os três fragmentos (T004) → aditividade (T005) → build (T006)
- US2: T007 ∥ T008 ∥ T009 — só verificação

### Parallel Opportunities

- T007 ∥ T008 ∥ T009 — inspeção de YAML × comparação de inventário × build do consumidor. Paralelizáveis com segurança porque **nenhuma das três edita**: toda a escrita ficou concentrada em T004

---

## Parallel Example: User Story 2

```bash
# Após T006 concluída (artefato instalado), lançar em paralelo:
Task: "T007 — inspecionar 400, constraints de url e as quatro ausências (pattern, additionalProperties, 409, array)"
Task: "T008 — diff /tmp/dto-baseline.txt /tmp/dto-after.txt → exatamente +1 linha"
Task: "T009 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T003"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T003): confirmar herança e **capturar os dois baselines**
2. Phase 3 (T004–T006): aplicar fragmentos 1 e 3 + validar
3. **STOP and VALIDATE**: quickstart 3–4 — se verde, a operação passa a ser chamável (endereço + verbo + corpo)

### Incremental Delivery

1. US1 → corpo declarado (MVP)
2. US2 → limites, desfecho `400`, geração previsível e regressão zero
3. Polish → commit de contratos, atualização do tracker, quickstart de ponta a ponta e commit do serviço (levando junto o `TASKS.json`)

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T007 ∥ T008 ∥ T009.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- Nenhuma task escreve código Java à mão. **Uma classe é gerada** (`ProductImageRequest.java`) — é consequência da edição de contrato, não trabalho manual
- Nenhuma task toca entidade, migração, mapper, controller ou config — necessidade disso indica fuga para T-003 ou T-005
- Nenhuma task declara `content` de resposta (T-002-4) nem estratégia de armazenamento (T-002-5)
- Total: **13 tasks** (3 setup, 0 foundational — herdada, 3 US1, 3 US2, 4 polish)
- Commits apenas na Phase 5 (T010 contratos, T013 serviço), após todas as validações. O `TASKS.json` é atualizado em T011, **antes** do commit do serviço, para que a transição de status entre no mesmo commit dos artefatos
- **Toda a escrita no contrato está em T004.** T005–T009 são verificação; nenhuma edita. Essa concentração é deliberada: garante que build, comparação de inventário e regressão do consumidor incidam sobre o estado **final** do documento
- **Obrigação herdada ENCERRADA**: o desfecho `400`, deferido a esta task por T-002-2, é declarado em T007. Não é repassado adiante
- **Questão REALOCADA, não redeferida**: o `409`/URL duplicada saiu desta task para a cadeia T-003. O motivo mudou (deixou de depender do formato do corpo e passou a depender do modelo de armazenamento) e o destinatário mudou. Registrado para que não pareça a mesma pergunta empurrada duas vezes
- **Consequência fixada para T-003**: corpo com apenas `url` ⇒ `Product.images[]` é lista de strings ⇒ coluna **`text[]`** em T-003-2, não `jsonb`. É a primeira vez que uma decisão de contrato desta cadeia fixa uma escolha de esquema de banco
