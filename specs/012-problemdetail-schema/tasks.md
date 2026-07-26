# Tasks: Schema ProblemDetail (RFC 7807) no contrato do catálogo

**Input**: Design documents from `/specs/012-problemdetail-schema/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/problemdetail-schema.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo — mesmo argumento de `T-002-3`/`T-002-4`: há geração (`ProblemDetail` → DTO, 6 → 7), mas geração ≠ comportamento. O artefato é portador de dados sem lógica, não referenciado por nenhuma operação (isso é `T-002-7-2..6`). Gates: build do módulo (T006), inventário de DTOs (T008) e regressão do consumidor (T009).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup herdado da cadeia (branch `feature/stock-balance-path`; `T-002-4` concluída). **Primeira das seis subtarefas de `T-002-7`.**

**Lição aplicada de `008`/`009`**: o `/speckit-analyze` de `008` encontrou um defeito em que um fragmento era aplicado numa fase de verificação, depois do build e da regressão. Aqui, **toda a escrita está em T004**, edição atômica única antes de qualquer gate. Nenhuma task de T005 em diante edita o contrato.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmento-fonte**: `hb-catalog-service/specs/012-problemdetail-schema/contracts/problemdetail-schema.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/012-problemdetail-schema/quickstart.md`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar estado herdado e **recapturar os dois baselines** — obrigatoriamente antes de qualquer edição

- [X] T001 Confirmar pré-condições em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: branch `feature/stock-balance-path` ativa nos dois repos; working tree de `platform-shared-contracts` limpo; `ProductImageResponse` é o último schema em `components/schemas`; há 6 schemas (`grep -cE "^    [A-Z][A-Za-z]+:$" contracts-catalog/openapi/catalog.yaml` → `6`); nenhum desfecho de erro tem `content` hoje
- [X] T002 **Recapturar o inventário de DTOs** conforme quickstart passo 1a, gravando nomes + `md5sum` em `/tmp/dto-baseline-012.txt`. **Esperado: 6 linhas** (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`). **Recapturado nesta execução** — reaproveitar arquivo de `008`/`009`/`011` invalidaria a comparação, pois `target/` é regenerado a cada build
- [X] T003 **Remedir** o baseline de testes do consumidor conforme quickstart passo 1b: `( cd hb-catalog-service && mvn -B verify )`, registrando a linha `Tests run:`. Medido agora, não herdado de specs anteriores

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001–T003 são os gates.

**Checkpoint**: Estado confirmado e ambos os baselines recapturados — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor tem um único tipo de erro para todo o catálogo (Priority: P1) 🎯 MVP

**Goal**: O schema `ProblemDetail` passa a existir em `components/schemas`, com os cinco membros canônicos da RFC 7807, nenhum obrigatório. A edição é aplicada **por inteiro** aqui, para que todos os gates incidam sobre o estado final.

**Independent Test**: quickstart passos 3–4 — schema presente, diff sem remoções, nenhum desfecho de erro ganhou `content`, build do módulo verde

### Implementation for User Story 1

- [X] T004 [US1] Anexar o schema `ProblemDetail` de `hb-catalog-service/specs/012-problemdetail-schema/contracts/problemdetail-schema.yaml` ao final de `components/schemas` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, após `ProductImageResponse` — `type: object`, `description` (FR-002), as cinco propriedades `type`/`title`/`status`/`detail`/`instance` (FR-003–FR-008), **sem** `required` (FR-009) e **sem** `additionalProperties: false` (FR-010) — **sem copiar as linhas de comentário**
  > **Uma única edição, e nada depois.** Toda a escrita no contrato termina aqui. Aplicar qualquer coisa depois de T005/T006 repetiria o defeito de `008`: o build validaria estado intermediário.
- [X] T005 [US1] Executar as verificações de aditividade do quickstart passo 3 sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) `git diff -U0 … | grep '^-' | grep -v '^---'` **sem nenhuma saída** — zero remoções (FR-012); (b) nenhum `content:` associado aos desfechos `404` de `getProductById`/`getStockItemByProductId` nem aos `400`/`403`/`404` de `addProductImage` — os cinco permanecem description-only (FR-011)
- [X] T006 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-013, e regenera `target/` para T008

**Checkpoint**: User Story 1 completa — schema declarado e documento válido; MVP entregável

---

## Phase 4: User Story 2 - Forma do schema espelha exatamente o que o serviço já emite (Priority: P2)

**Goal**: Confirmar que os cinco membros e seus tipos correspondem ao que `ApiExceptionHandler` produz em runtime; geração previsível; regressão zero. **Nenhuma task desta fase edita o contrato**

**Independent Test**: `description` do schema inspecionável isoladamente contra `ApiExceptionHandler.java`; inventário com exatamente um nome novo; `mvn -B verify` verde

### Implementation for User Story 2

- [X] T007 [P] [US2] **Verificação pura.** Inspecionar `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` contra `hb-catalog-service/src/main/java/com/hubinity/catalog/api/error/ApiExceptionHandler.java`: (a) `type`/`instance` são `string`/`format: uri`, simétrico a `java.net.URI` no código (FR-004, FR-008); (b) `status` é `integer`/`format: int32`, não string (FR-006); (c) a `description` de `type` menciona o padrão `"about:blank"` (FR-004); (d) nenhuma das cinco propriedades é `required` (FR-009); (e) `additionalProperties: false` não aparece no schema (FR-010); (f) o schema tem **exatamente cinco** propriedades — nenhuma sexta foi adicionada (FR-003); (g) `title` é `string` (FR-005); (h) `detail` é `string` (FR-007)
- [X] T008 [P] [US2] Executar o quickstart passo 5: gerar `/tmp/dto-after-012.txt` e `diff /tmp/dto-baseline-012.txt /tmp/dto-after-012.txt`. **Esperado**: exatamente **uma linha adicionada**, `ProblemDetail.java`; checksums dos **6** preexistentes idênticos ao baseline de T002; total de **7** DTOs — satisfaz FR-014
- [X] T009 [P] [US2] Provar regressão zero no consumidor: `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem **idêntica à medida em T003** — satisfaz FR-015 e SC-005

**Checkpoint**: Ambas as user stories verificadas de forma independente

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Registro, rastreabilidade e fechamento da task

- [X] T010 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): add ProblemDetail schema (T-002-7-1)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo que o schema é transcrição de `org.springframework.http.ProblemDetail` já usado por `ApiExceptionHandler`, e que ainda não é referenciado por nenhuma operação (isso é `T-002-7-2..6`)
- [X] T011 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-7-1`, mantendo o restante intacto
- [X] T012 Executar o roteiro completo de `hb-catalog-service/specs/012-problemdetail-schema/quickstart.md` de ponta a ponta e confirmar que os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) seguem refletindo o resultado
- [X] T013 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/012-problemdetail-schema/`, `TASKS.json` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for ProblemDetail schema (T-002-7-1)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T002 e T003 **devem** rodar antes de qualquer edição — capturado depois, o baseline não prova nada
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–4)**: US1 depende de T001–T003; US2 depende de T004 (edição aplicada) e, para T008/T009, de T006 (artefato reconstruído/instalado)
- **Polish (Phase 5)**: depende de US1 + US2. T011 antes de T013, para que a transição de status entre no mesmo commit dos artefatos

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup
- **US2 (P2)**: inspeciona a edição de T004 e o artefato de T006; critérios de teste próprios

### Within Each User Story

- US1: anexar o schema (T004) → aditividade (T005) → build (T006)
- US2: T007 ∥ T008 ∥ T009 — só verificação

### Parallel Opportunities

- T007 ∥ T008 ∥ T009 — inspeção de YAML × comparação de inventário × build do consumidor. Paralelizáveis com segurança porque **nenhuma edita**: toda a escrita ficou em T004

---

## Parallel Example: User Story 2

```bash
# Após T006 concluída (artefato instalado), lançar em paralelo:
Task: "T007 — inspecionar os cinco membros contra ApiExceptionHandler.java, ausência de required e de additionalProperties"
Task: "T008 — diff /tmp/dto-baseline-012.txt /tmp/dto-after-012.txt → exatamente +1 linha"
Task: "T009 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T003"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T003): confirmar herança e **recapturar** os dois baselines
2. Phase 3 (T004–T006): anexar o schema + validar
3. **STOP and VALIDATE**: quickstart 3–4 — se verde, o schema `ProblemDetail` existe e está pronto para ser referenciado por `T-002-7-2..6`

### Incremental Delivery

1. US1 → schema declarado (MVP)
2. US2 → fidelidade de forma, geração previsível e regressão zero
3. Polish → commit de contratos, tracker, quickstart e commit do serviço

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T007 ∥ T008 ∥ T009.

---

## Notes

- [P] tasks = repos/atividades distintas, sem dependências de escrita
- **Toda a escrita no contrato está em T004.** T005–T009 são verificação.
- Nenhuma task escreve código Java à mão; **uma classe é gerada** (`ProblemDetail.java`), consequência da edição de contrato
- Nenhuma task toca entidade, coluna, mapper, controller, config ou qualquer desfecho de erro — isso indicaria fuga para `T-002-7-2..6`
- Nenhuma task modela a propriedade de extensão `errors` de validação — fora de escopo (research R3)
- Total: **13 tasks** (3 setup, 0 foundational — herdada, 3 US1, 3 US2, 4 polish)
- Commits apenas na Phase 5 (T010 contratos, T013 serviço), após todas as validações. `TASKS.json` é atualizado em T011, **antes** do commit do serviço, para entrar junto
- **Marco**: concluída esta task, `T-002-7` tem sua primeira subtarefa entregue. Restam `T-002-7-2` até `T-002-7-6`, cada uma referenciando este schema em um desfecho de erro específico
