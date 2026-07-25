# Tasks: Estratégia de armazenamento de imagens de produto

**Input**: Design documents from `/specs/010-image-storage-strategy/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/image-storage-strategy.yaml, quickstart.md

**Tests**: Nenhum teste automatizado novo — e aqui o argumento é **mais forte** que nas tasks anteriores da cadeia, não mais fraco. T-002-3 e T-002-4 precisaram alegar "geração ≠ comportamento" apesar de emitirem um DTO; esta task **não gera nada**: a mudança é exclusivamente textual em dois campos `description` que o gerador não lê para produzir modelo (ver `data-model.md`). Não há unidade de comportamento a testar em `hb-catalog-service`. Gates: escopo do diff (T005), build do módulo (T006), inércia de geração (T009) e regressão do consumidor (T010).

**Organization**: Tasks agrupadas por user story. Caminhos relativos ao diretório-pai comum (`.../hubinity/`). Setup herdado da cadeia (branch `feature/stock-balance-path`; T-002-4 concluída em `4fa9056`). **Quinta e última task da cadeia T-002 — a primeira que não gera código e a primeira que remove linhas.**

**Lição aplicada da cadeia**: o `/speckit-analyze` da 008 encontrou o defeito **O1** — um fragmento aplicado numa fase de *verificação*, depois do build e da regressão, de modo que o estado entregue nunca passava por gate. A 009 corrigiu concentrando toda a escrita em T004. Aqui vale o mesmo, com **razão adicional de conteúdo**: o Bloco 2 (Path Item) *remete* à declaração criada pelo Bloco 1 (`info.description`), por exigência de FR-012. Aplicar o Bloco 2 sozinho produziria uma `description` que aponta para uma declaração inexistente — estado intermediário inválido. **Os dois blocos são, portanto, uma única edição atômica por necessidade, não por conveniência.**

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)

## Path Conventions

- **Entregável físico**: `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão)
- **Fragmentos-fonte**: `hb-catalog-service/specs/010-image-storage-strategy/contracts/image-storage-strategy.yaml`
- **Roteiro de validação**: `hb-catalog-service/specs/010-image-storage-strategy/quickstart.md`
- **`hb-catalog-service` não tem arquivo de produção alterado** — participa apenas como medidor de regressão

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Confirmar estado herdado e **recapturar os dois baselines** — obrigatoriamente antes de qualquer edição

- [X] T001 Confirmar pré-condições em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: branch `feature/stock-balance-path` ativa nos dois repos; working tree limpo com HEAD em `4fa9056`; o andaime a remover ainda existe (`grep -c "are completed by" …` → `1`); há 6 schemas (`grep -cE "^    [A-Z][A-Za-z]+:$" …` → `6`); `info.description` ainda tem apenas as 2 linhas originais (nenhuma ocorrência de `URL-only reference strategy` fora do Path Item e de `ProductImageRequest`)
- [X] T002 **Recapturar o inventário de DTOs** conforme quickstart §Gate 0, gravando caminho + `sha256sum` em `/tmp/dto-baseline-010.txt`. **Esperado: 6 linhas** (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`). **Recapturado nesta execução** — reaproveitar `/tmp/dto-baseline-009.txt` invalidaria a comparação, porque `target/` é regenerado a cada build (FR-019)
- [X] T003 **Remedir** o baseline de testes do consumidor conforme quickstart §Gate 0: `( cd hb-catalog-service && mvn -B verify )`, registrando a linha `Tests run:` em `/tmp/verify-before-010.log`. Medido agora, não herdado da 009 (FR-020)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Nada a construir — herdado da cadeia. T001–T003 são os gates.

**Checkpoint**: Estado confirmado e ambos os baselines recapturados — user stories podem começar

---

## Phase 3: User Story 1 - Consumidor descobre a fronteira de responsabilidade antes de integrar (Priority: P1) 🎯 MVP

**Goal**: `info.description` passa a declarar a estratégia URL-only como decisão normativa, incluindo a fronteira de responsabilidade hoje ausente do contrato: o catálogo não verifica que a URL resolve, e uma referência que deixa de resolver não é violação de contrato.

**Independent Test**: quickstart §Gate 1 e §Gate 5 — parágrafo presente com os seis conteúdos exigidos, documento válido pelo build

### Implementation for User Story 1

- [X] T004 [US1] Aplicar os **dois blocos** de `hb-catalog-service/specs/010-image-storage-strategy/contracts/image-storage-strategy.yaml` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, numa **única edição atômica**: (a) parágrafo da estratégia acrescentado a `info.description`, **abaixo** das 2 linhas existentes, que não são tocadas (FR-001 a FR-008); (b) `description` do Path Item `/api/v1/products/{productId}/images` reescrita para o estado final, com `summary` intocado (FR-009 a FR-013) — **sem copiar as linhas de comentário do arquivo-fonte**
  > **Os dois juntos, e nada depois.** Toda a escrita no contrato termina aqui; T005 em diante é verificação. Além do defeito O1 da 008, há razão de conteúdo: FR-012 exige que o Bloco 2 remeta à declaração criada pelo Bloco 1 — aplicá-lo sozinho apontaria para uma declaração inexistente.
- [X] T005 [US1] Executar o gate de **escopo do diff** (quickstart §Gate 4) sobre `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: `git diff -U0 … | grep '^-' | grep -v '^---'` deve retornar **apenas** linhas do bloco `description` do Path Item de imagens — em particular a frase de andaime *"The POST operation is declared; its request body and response body are completed by T-002-3 and T-002-4."* (FR-017). Reprova se houver remoção em `info.description` (FR-007), no `summary` (FR-013), em schemas (FR-014) ou em operações (FR-016). Confirmar também `git status --porcelain` sem nenhum arquivo além do `catalog.yaml`
  > **Este é o gate próprio desta task.** É a primeira da cadeia que remove linhas, então o critério "nenhuma linha `-`" usado em T-002-1/-3/-4 não está disponível; o predicado o substitui sem perder objetividade. Usar `grep '^-' | grep -v '^---'`, **não** `grep '^-[^-]'`: uma linha em branco removida aparece como `-` sozinho e escaparia do segundo padrão.
- [X] T006 [US1] Executar a autoridade de validação: `( cd platform-shared-contracts && mvn -B -pl contracts-catalog -am -DskipTests install )` → `BUILD SUCCESS` — satisfaz FR-018 e SC-005, e regenera `target/` para T009

**Checkpoint**: User Story 1 completa — estratégia declarada e documento válido; MVP entregável

---

## Phase 4: User Story 2 - Leitor da operação de produto entende URLs que não são nossas (Priority: P2)

**Goal**: Confirmar que a declaração é **canônica e única**, no nível do documento — alcançando quem lê `/api/v1/products/{id}`, hoje e depois que `T-002-6` expuser `images` no schema `Product`. **Nenhuma task desta fase edita o contrato**

**Independent Test**: posição da declaração verificável no documento, sem necessidade de build

### Implementation for User Story 2

- [X] T007 [P] [US2] **Verificação pura.** Inspecionar `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) a declaração canônica está em `info.description`, **acima** de `paths`, e **não** dentro de um Path Item nem de um schema (FR-008); (b) o parágrafo **nomeia** a estratégia como `URL-only reference strategy` (FR-001) e afirma hospedagem externa e persistência apenas de URLs absolutas (FR-002), ausência de recepção/armazenamento/serviço de bytes e de operações de upload/download/transformação e de URI por imagem (FR-003), a fronteira de responsabilidade — não-verificação de resolução e não-violação de contrato (FR-004) —, a semântica de remoção (FR-005) e a expectativa de HTTPS (FR-006); (c) as 2 linhas originais de `info.description` seguem idênticas (FR-007); (d) o texto de HTTPS não contradiz o já presente em `ProductImageRequest` (FR-006)

**Checkpoint**: Posicionamento e conteúdo normativo verificados de forma independente

---

## Phase 5: User Story 3 - Leitor do contrato para de ver tasks pendentes que já terminaram (Priority: P3)

**Goal**: Confirmar o encerramento da cadeia na `description` do Path Item, na forma espelhada de `T-001-5` (`68873d5`). **Nenhuma task desta fase edita o contrato**

**Independent Test**: `description` do Path Item inspecionável isoladamente; nenhuma menção a task pendente

### Implementation for User Story 3

- [X] T008 [P] [US3] **Verificação pura.** Inspecionar a `description` do Path Item `/api/v1/products/{productId}/images` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`: (a) declara operação, corpo de requisição, corpo de resposta e autorização como plenamente declarados, **cadeia T-002 completa** (FR-009); (b) `grep -n 'T-002-3\|T-002-4' …` sem saída — nenhuma menção a tasks nem a trabalho futuro (FR-010); (c) preserva a autorização vigente: Bearer JWT herdado do `bearerAuth` em nível de documento **e** role `admin` para registrar imagem (FR-011); (d) a remissão à estratégia cabe em **uma frase**, nomeia o nível do documento como origem e **não** reproduz o conteúdo de FR-004 (FR-012); (e) `summary` idêntico ao anterior (FR-013); (f) comparar com a `description` do Path Item de saldo e confirmar a mesma forma de encerramento de cadeia (US3 #3)

**Checkpoint**: Todas as três user stories verificadas de forma independente

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Gates transversais, registro, rastreabilidade e encaminhamento das pendências

- [X] T009 [P] Executar o gate de **inércia de geração** (quickstart §Gate 2): gerar `/tmp/dto-after-010.txt` e `diff /tmp/dto-baseline-010.txt /tmp/dto-after-010.txt`. **Esperado**: `diff` **sem saída** — 6 ↔ 6 com checksums idênticos, nenhum DTO criado, alterado ou removido. Satisfaz FR-019 e SC-006. Qualquer divergência indica que um schema foi tocado, provavelmente `ProductImageRequest` (FR-014/FR-015)
- [X] T010 [P] Provar regressão zero no consumidor (quickstart §Gate 3): `( cd hb-catalog-service && mvn -B verify )` verde, com a contagem `Tests run:` **idêntica à medida em T003** e `Failures: 0, Errors: 0` — satisfaz FR-020 e SC-007
- [X] T011 [P] Confirmar que **nenhum `pattern` foi introduzido** no documento (`grep -n 'pattern:' platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` sem saída) e que `ProductImageRequest.url` mantém `format: uri`, `minLength: 1`, `maxLength: 2048` inalterados — satisfaz FR-014 e FR-015
- [X] T012 Commitar em `platform-shared-contracts/` (branch `feature/stock-balance-path`) com prefixo convencional — sugestão: `feat(contracts): ratify URL-only image storage strategy (T-002-5)` — restrito a `contracts-catalog/openapi/catalog.yaml`, registrando no corpo (a) que a estratégia URL-only passa de propriedade emergente a **decisão declarada**, com a fronteira de responsabilidade explicitada, e (b) que a **cadeia T-002 fica encerrada**, com a `description` do Path Item em estado final
- [X] T013 Atualizar `hb-catalog-service/TASKS.json`: definir `"status": "done"` na task `T-002-5`, mantendo o restante intacto. **Não executado pelo pipeline de especificação** — é ação da fase de implementação
- [X] T014 **Apresentar ao usuário as pendências**, extraídas de `hb-catalog-service/specs/010-image-storage-strategy/spec.md` (§Out of Scope): (a) a **substituição proposta para `T-005-3`**, cuja redação atual ("recepção multipart") contradiz a estratégia ratificada e fica inválida; (b) a entrada proposta **`T-002-8`** (enforcement de HTTPS via `pattern`, `depends_on: [T-002-5]`); (c) a observação de que `T-005-1`/`T-005-2` usam o termo *upload*, enganoso sob URL-only ainda que o comportamento descrito não conflite. **Não editar `TASKS.json`** para nenhuma delas — a inserção é decisão do usuário
- [X] T015 Executar o roteiro completo de `hb-catalog-service/specs/010-image-storage-strategy/quickstart.md` de ponta a ponta e atualizar os checklists da feature (`checklists/requirements.md`, `checklists/contract.md`) conforme o resultado
- [X] T016 Commitar em `hb-catalog-service/` (branch `feature/stock-balance-path`) os artefatos da feature — `specs/010-image-storage-strategy/`, `TASKS.json` e o bloco SPECKIT de `CLAUDE.md` — sugestão: `docs: add spec artifacts for image storage strategy (T-002-5)`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: T002 e T003 **devem** rodar antes de qualquer edição — capturado depois, o baseline não prova nada
- **Foundational (Phase 2)**: vazia (herdada)
- **User Stories (Phase 3–5)**: US1 depende de T001–T003; US2 e US3 dependem de T004 (edição aplicada), mas **não** de T006 — são inspeção textual, dispensam build
- **Polish (Phase 6)**: T009 depende de T006 (artefato regenerado); T010 depende de T006 (artefato instalado); T012 depende de todos os gates verdes; T013 antes de T016, para que a transição de status entre no mesmo commit dos artefatos

### User Story Dependencies

- **US1 (P1)**: independente — exige apenas o setup. É o MVP
- **US2 (P2)**: inspeciona o Bloco 1 aplicado em T004; critério de teste próprio, sem build
- **US3 (P3)**: inspeciona o Bloco 2 aplicado em T004; critério de teste próprio, sem build

> **Nota sobre independência**: as três stories são verificáveis de forma independente, mas **não são entregáveis de forma independente** — T004 é atômica por exigência de FR-012 (o Bloco 2 remete ao Bloco 1). Registrar isso é mais honesto que fatiar a edição e criar um estado intermediário inválido só para satisfazer a forma do template.

### Within Each User Story

- US1: aplicar os dois blocos (T004) → escopo do diff (T005) → build (T006)
- US2: T007 — só verificação
- US3: T008 — só verificação

### Parallel Opportunities

- **T007 ∥ T008** — inspeção de dois blocos distintos do mesmo arquivo, ambas somente leitura
- **T009 ∥ T010 ∥ T011** — inventário de DTOs × build do consumidor × ausência de `pattern`. Paralelizáveis com segurança porque **nenhuma edita**: toda a escrita ficou em T004

---

## Parallel Example: Verificações

```bash
# Após T004 aplicada, sem esperar o build:
Task: "T007 — declaração canônica em info.description, conteúdo FR-002..FR-006, 2 linhas originais intactas"
Task: "T008 — description do Path Item em estado final, sem menção a tasks, autorização preservada"

# Após T006 (artefato instalado), lançar em paralelo:
Task: "T009 — diff /tmp/dto-baseline-010.txt /tmp/dto-after-010.txt → SEM saída"
Task: "T010 — ( cd hb-catalog-service && mvn -B verify ) vs baseline de T003"
Task: "T011 — grep 'pattern:' no catalog.yaml → sem saída"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1 (T001–T003): confirmar herança e **recapturar** os dois baselines
2. Phase 3 (T004–T006): aplicar os dois blocos + escopo do diff + build
3. **STOP and VALIDATE**: quickstart §Gate 1, §Gate 4 e §Gate 5 — se verde, a estratégia de armazenamento está declarada como decisão do contrato e a fronteira de responsabilidade deixou de ser suposição do integrador

### Incremental Delivery

1. US1 → estratégia declarada em nível de documento (MVP)
2. US2 → posicionamento canônico verificado, alcançando o leitor do schema `Product`
3. US3 → cadeia T-002 encerrada na `description` do Path Item
4. Polish → gates transversais, commit de contratos, tracker, encaminhamento das pendências, quickstart e commit do serviço

### Parallel Team Strategy

Feature de uma pessoa — paralelismo em T007 ∥ T008 e em T009 ∥ T010 ∥ T011.

---

## Notes

- [P] tasks = arquivos/atividades distintas, sem dependências de escrita
- **Toda a escrita no contrato está em T004.** T005–T011 são verificação. Concentração deliberada, herdada da correção do defeito O1 da 008 e reforçada pela dependência de conteúdo entre os dois blocos
- Nenhuma task escreve código Java à mão e **nenhuma classe é gerada** — primeira task da cadeia com essa propriedade. O gate correspondente (T009) exige **igualdade**, não incremento
- Nenhuma task toca entidade, coluna, mapper, controller ou config — isso indicaria fuga para T-003 ou T-005
- Nenhuma task adiciona `pattern`, modela `ProblemDetail` ou altera o schema `Product` — são as pendências encaminhadas em T014
- Total: **16 tasks** (3 setup, 0 foundational — herdada, 3 US1, 1 US2, 1 US3, 8 polish)
- Commits apenas na Phase 6 (T012 contratos, T016 serviço), após todos os gates. `TASKS.json` é atualizado em T013, **antes** do commit do serviço, para entrar junto
- **Marco**: concluída esta feature, a **cadeia T-002 está encerrada**. Restam no tracker `T-002-6` (propriedade `images` no schema `Product`) e `T-002-7` (`ProblemDetail` RFC 7807), ambas já registradas como `refined`, mais a proposta `T-002-8`
- **Uma contradição downstream fica registrada e não resolvida**: `T-005-3` exige recepção **multipart**, incompatível com a estratégia ratificada. A contradição nasceu em `T-002-3` (que fixou corpo JSON) e permanecia latente; esta task a torna detectável ao promover a estratégia a decisão declarada. Encaminhada em T014 — `TASKS.json` não é editado pelo pipeline de especificação
