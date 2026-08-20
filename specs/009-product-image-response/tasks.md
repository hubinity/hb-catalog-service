# Tasks: Schema de resposta do registro de imagem de produto

**Input**: Design documents from `/specs/009-product-image-response/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/product-image-response.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo, pelo argumento **já reformulado em T-002-3** — não o de T-002-1/T-002-2, que caducou. Há geração (`ProductImageResponse` → DTO, 5 → 6), mas geração ≠ comportamento: o artefato é portador de dados sem lógica, que o serviço ainda não referencia (cadeia T-005). Gates: build do módulo (T006), inventário de DTOs (T007) e regressão do consumidor (T008).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup herdado da cadeia (branch `feature/stock-balance-path`; T-002-3 concluída em `40dd8e0`). **Quarta task da cadeia T-002 — e a segunda que gera código.**

**Lição aplicada da execução anterior**: o `/speckit-analyze` da 008 encontrou um defeito (O1) em que um fragmento era aplicado numa fase de **verificação**, depois do build e da regressão — de modo que o estado entregue nunca passava por gate. Aqui, **toda a escrita está em T004**, uma única edição atômica antes de qualquer gate. Nenhuma task de T005 em diante edita o contrato.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmentos-fonte**: `hb-catalog-service/specs/009-product-image-response/contracts/product-image-response.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/009-product-image-response/quickstart.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar estado herdado e **recapturar os dois baselines** — obrigatoriamente antes de qualquer edição

- [X] T001 Confirmar pré-condições: branch `feature/stock-balance-path` ativa nos dois repos; working tree de `platform-shared-contracts` limpo com HEAD em `40dd8e0`; o `requestBody` existe (`grep -c "requestBody" platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` → `1`); o `201` ainda não tem corpo (nenhum `content:` entre `'201':` e `'400':`); há 5 schemas (`grep -cE "^    [A-Z][A-Za-z]+:$" …` → `5`)
- [X] T002 **Recapturar o inventário de DTOs** conforme quickstart passo 1a, gravando nomes + `md5sum` em `/tmp/dto-baseline-009.txt`. **Esperado: 5 linhas** (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`). **Recapturado nesta execução** — reaproveitar o arquivo da 008 invalidaria a comparação, porque `target/` é regenerado a cada build
- [X] T003 **Remedir** o baseline de testes do consumidor conforme quickstart passo 1b: `( cd hb-catalog-service && mvn -B verify )`, registrando a linha `Tests run:`. Medido agora, não herdado da 008

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001–T003 são os gates.

**Checkpoint**: Estado confirmado e ambos os baselines recapturados — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor confirma o resultado do registro sem uma segunda chamada (Priority: P1) 🎯 MVP

**Goal**: O `201` passa a ter corpo — `ProductImageResponse` com `productId` e a coleção resultante. A edição é aplicada **por inteiro** aqui, para que todos os gates incidam sobre o estado final.

**Independent Test**: quickstart passos 3–4 — `content` e schema presentes, diff sem remoções, build do módulo verde

### Implementation for User Story 1

- [X] T004 [US1] Aplicar os **dois fragmentos** de `hb-catalog-service/specs/009-product-image-response/contracts/product-image-response.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, numa **única edição atômica**: (a) `content` acrescentado ao desfecho `'201'` existente, **abaixo** da sua `description`, que não é tocada (FR-001, FR-002, FR-003); (b) schema `ProductImageResponse` anexado ao fim de `components/schemas`, após `ProductImageRequest` (FR-004, FR-005, FR-006, FR-007, FR-008) — **sem copiar as linhas de comentário**
  > **Os dois juntos, e nada depois.** Toda a escrita no contrato termina aqui. Aplicar qualquer coisa depois de T005/T006 repetiria o defeito O1 da 008: o build validaria estado intermediário e o artefato consumido por T008 não conteria a mudança inteira.
- [X] T005 [US1] Executar as verificações de aditividade do quickstart passo 3 sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) `git diff -U0 … | grep '^-' | grep -v '^---'` **sem nenhuma saída** — zero remoções (FR-013); (b) `grep -c "No Location header is returned" …` → `1`, confirmando a `description` do `201` intacta (FR-003); (c) `grep -c "are completed by" …` → `1`, confirmando a `description` do Path Item intacta (FR-012); (d) nenhum `content:` entre `'400':` e `'404':` — erros seguem description-only (FR-011)
- [X] T006 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-014 e SC-004, e regenera `target/` para T007

**Checkpoint**: User Story 1 completa — resposta declarada e documento válido; MVP entregável

---

## Phase 4: User Story 2 - Consumidor descobre qual é a imagem principal (Priority: P2)

**Goal**: Confirmar que ordenação, papel do primeiro elemento e restrições dos itens estão declarados; geração previsível; regressão zero. **Nenhuma task desta fase edita o contrato**

**Independent Test**: `description` do schema e de `images` inspecionáveis isoladamente; inventário com exatamente um nome novo; `mvn -B verify` verde

### Implementation for User Story 2

- [X] T007 [P] [US2] **Verificação pura.** Inspecionar `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) a `description` de `ProductImageResponse` afirma **coleção completa** e **primeiro elemento = imagem principal** (FR-004); (b) `required: [productId, images]` com exatamente duas propriedades (FR-005); (c) `productId` é `string`/`uuid` com `description` (FR-006); (d) `images` é `array` cuja `description` afirma a **ordem** (FR-007); (e) itens com `type: string`, `format: uri`, `maxLength: 2048` (FR-008); (f) **não** há `minItems` (FR-009) nem `additionalProperties: false` (FR-010); (g) o `content` usa `$ref`, sem inline, e `application/json` é a única mídia (FR-001, FR-002)
- [X] T008 [P] [US2] Executar o quickstart passo 5: gerar `/tmp/dto-after-009.txt` e `diff /tmp/dto-baseline-009.txt /tmp/dto-after-009.txt`. **Esperado**: exatamente **uma linha adicionada**, `ProductImageResponse.java`; checksums dos **5** preexistentes idênticos ao baseline de T002; total de **6** DTOs — satisfaz FR-015 e SC-005
- [X] T009 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem **idêntica à medida em T003** — satisfaz FR-016 e SC-006

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e encaminhamento das duas lacunas de backlog

- [X] T010 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): add product image response schema (T-002-4)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo (a) que a resposta devolve a **coleção resultante** e por quê (sob URL-only não há recurso individual endereçável, mesma razão da ausência de `Location`) e (b) que a operação de imagens fica **contratualmente completa**, restando à cadeia T-002 apenas a T-002-5
- [X] T011 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-4`, mantendo o restante intacto
- [X] T012 **Apresentar ao usuário as duas entradas propostas** para o tracker, extraídas de `hb-catalog-service/specs/009-product-image-response/spec.md` (*Lacuna de backlog* e *Out of Scope*): `T-002-6` (schema `Product` sem `images`) e `T-002-7` (`ProblemDetail` RFC 7807 nos desfechos de erro), com id, descrição, fase, `depends_on` e posição sugerida na fila. **Não editar `TASKS.json`** — a inserção é decisão do usuário
- [X] T013 Executar o roteiro completo de `hb-catalog-service/specs/009-product-image-response/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/response-schema.md`) conforme o resultado
- [X] T014 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/009-product-image-response/`, `TASKS.json` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for product image response schema (T-002-4)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T002 e T003 **devem** rodar antes de qualquer edição — capturado depois, o baseline não prova nada
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–4)**: US1 depende de T001–T003; US2 depende de T004 (edição aplicada) e, para T008/T009, de T006 (artefato reconstruído/instalado)
- **Polish (Phase 5)**: depende de US1 + US2. T011 antes de T014, para que a transição de status entre no mesmo commit dos artefatos

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup
- **US2 (P2)**: inspeciona a edição de T004 e o artefato de T006; critérios de teste próprios

### Within Each User Story

- US1: aplicar os dois fragmentos (T004) → aditividade (T005) → build (T006)
- US2: T007 ∥ T008 ∥ T009 — só verificação

### Parallel Opportunities

- T007 ∥ T008 ∥ T009 — inspeção de YAML × comparação de inventário × build do consumidor. Paralelizáveis com segurança porque **nenhuma edita**: toda a escrita ficou em T004

---

## Parallel Example: User Story 2

```bash
# Após T006 concluída (artefato instalado), lançar em paralelo:
Task: "T007 — inspecionar schema, descriptions, restrições e as duas ausências (minItems, additionalProperties)"
Task: "T008 — diff /tmp/dto-baseline-009.txt /tmp/dto-after-009.txt → exatamente +1 linha"
Task: "T009 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T003"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T003): confirmar herança e **recapturar** os dois baselines
2. Phase 3 (T004–T006): aplicar os dois fragmentos + validar
3. **STOP and VALIDATE**: quickstart 3–4 — se verde, a operação de imagens está contratualmente completa (endereço, verbo, corpo de requisição e de resposta)

### Incremental Delivery

1. US1 → corpo da resposta declarado (MVP)
2. US2 → ordenação, restrições, geração previsível e regressão zero
3. Polish → commit de contratos, tracker, apresentação das duas lacunas, quickstart e commit do serviço

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T007 ∥ T008 ∥ T009.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- **Toda a escrita no contrato está em T004.** T005–T009 são verificação. Concentração deliberada, corrigindo o defeito O1 detectado na 008
- Nenhuma task escreve código Java à mão; **uma classe é gerada** (`ProductImageResponse.java`), consequência da edição de contrato
- Nenhuma task toca entidade, coluna, mapper, controller ou config — isso indicaria fuga para T-003 ou T-005
- Nenhuma task modela `ProblemDetail` nem altera o schema `Product` — são as duas lacunas encaminhadas em T012
- Total: **14 tasks** (3 setup, 0 foundational — herdada, 3 US1, 3 US2, 5 polish)
- Commits apenas na Phase 5 (T010 contratos, T014 serviço), após todas as validações. `TASKS.json` é atualizado em T011, **antes** do commit do serviço, para entrar junto
- **Marco**: concluída esta feature, a operação de registro de imagem fica **contratualmente completa**. Resta à cadeia T-002 apenas a **T-002-5**, que documenta a estratégia de armazenamento e faz a limpeza final da `description` do Path Item
- **Duas divergências contrato × serviço permanecem abertas**, ambas preexistentes e agora documentadas — nenhuma criada por esta task:
  - **Schema `Product` sem `images`**: após T-003-4 o serviço devolverá imagens que o contrato não declara. Proposta `T-002-6`
  - **Erros sem `ProblemDetail`**: o Princípio I exige RFC 7807 do serviço; o contrato declara os desfechos de erro só com `description`. Proposta `T-002-7`, com `decomposition_allowed: true` por alcançar todas as operações
