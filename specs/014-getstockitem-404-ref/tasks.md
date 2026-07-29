---

description: "Task list template for feature implementation"
---

# Tasks: Referenciar ProblemDetail no 404 de getStockItemByProductId

**Input**: Design documents from `specs/014-getstockitem-404-ref/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md — todos completos e consistentes (checklists `requirements.md` e `api.md` resolvidas antes desta geração)

**Tests**: Não se aplica — nenhum código Java muda; `getStockItemByProductId` nem sequer tem implementação ainda (cadeia `T-004`, `refined`). "Tests" aqui significa validação de build do contrato, não `*Test.java`/`*IT.java`.

**Organization**: Uma única user story (P1) — mesma escala da task irmã `T-002-7-2`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos/comandos diferentes, sem dependência pendente)
- **[Story]**: US1 (única story desta feature)

## Path Conventions

`../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` — repo irmão, um nível acima de `hb-catalog-service` (assumindo a raiz de `hb-catalog-service` como cwd).

---

## Phase 1: Setup

**Não se aplica.**

## Phase 2: Foundational

**Não se aplica.** A única dependência (`T-002-7-2`, done) já está satisfeita.

---

## Phase 3: User Story 1 - Consumidor sabe o formato do corpo de erro ao consultar saldo de estoque de um produto inexistente (Priority: P1) 🎯 MVP

**Goal**: O desfecho `404` de `getStockItemByProductId` declara `content` referenciando `ProblemDetail`, preservando a `description` existente.

**Independent Test**: `content.application/json.schema` do desfecho `404` de `getStockItemByProductId` referencia `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde (ver spec, User Story 1).

- [X] T001 [US1] Adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` da operação `getStockItemByProductId` em `../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, preservando a `description` "Stock balance not found for the given product (unknown product or no stock record)" existente
- [X] T002 [P] [US1] Confirmar via `git -C ../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml` que a mudança é estritamente aditiva (nenhuma linha `-`) — se houver remoção de conteúdo pré-existente, reverter e refazer (depends on T001)
- [X] T003 [US1] A partir da raiz de `hb-catalog-service`, rodar `( cd ../platform-shared-contracts && mvn -B -DskipTests install )` e confirmar que `find ../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l` retorna 7 (nenhum schema novo, nenhum removido) (depends on T001)
- [X] T004 [US1] Na raiz de `hb-catalog-service`, rodar `mvn -B verify` e confirmar build verde — trivialmente, já que `getStockItemByProductId` não tem implementação Java ainda (depends on T003)

**Checkpoint**: US1 completa — a única story desta feature. Feature pronta.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Fechar a task com os mesmos dois commits do precedente real desta cadeia (`29a9570`, T-002-7-2: um commit no repo de contratos, um commit em `hb-catalog-service` empacotando artefatos + tracker) e atualizar o tracker — passos que a execução literal desta lista deve cobrir, não deixar implícitos.

- [X] T005 Commit em `../platform-shared-contracts` contendo apenas `contracts-catalog/openapi/catalog.yaml`: `git -C ../platform-shared-contracts add contracts-catalog/openapi/catalog.yaml && git -C ../platform-shared-contracts commit -m "feat(contracts): reference ProblemDetail in stock balance 404 (T-002-7-3)"` (depends on T004)
- [X] T006 Atualizar `TASKS.json` (raiz de `hb-catalog-service`): alterar o campo `status` da task `T-002-7-3` de `"refined"` para `"done"` (depends on T005)
- [X] T007 Atualizar o marcador `<!-- SPECKIT START -->`/`<!-- SPECKIT END -->` em `CLAUDE.md` (raiz de `hb-catalog-service`) para apontar a `specs/014-getstockitem-404-ref/plan.md`, caso ainda não esteja apontando para lá (depends on T006)
- [X] T008 Commit em `hb-catalog-service` empacotando `CLAUDE.md`, `TASKS.json` e `specs/014-getstockitem-404-ref/**` (todo o diretório da feature, incluindo este `tasks.md` com os checkboxes marcados) com mensagem `docs(spec): complete getStockItemByProductId 404 contract task` (depends on T007)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup/Foundational**: N/A
- **User Story 1**: única fase — T001 é o único passo com edição real; T002, T003, T004 são verificações sequenciais dependentes de T001
- **Final Phase**: T005–T008 são estritamente sequenciais e dependem de US1 completa (T004) — cada commit/atualização depende do passo anterior ter sido feito com sucesso

### Parallel Opportunities

- T002 pode rodar em paralelo a T003 (comandos independentes), ambos após T001
- T004 depende do resultado de T003
- T005–T008 não têm oportunidade de paralelismo — são uma cadeia estrita de commit → atualizar tracker → atualizar CLAUDE.md → commit final

---

## Parallel Example: User Story 1

```bash
# Após T001 (edição do YAML):
Task: "Confirmar diff estritamente aditivo (T002)"
Task: "Build de contracts-catalog e confirmar 7 DTOs (T003)"

# Depois de T003:
Task: "mvn -B verify em hb-catalog-service (T004)"
```

---

## Implementation Strategy

### MVP First (única story)

1. T001 (edição) → T002 ∥ T003 (verificação) → T004 (regressão)
2. **PARAR e VALIDAR**: diff aditivo, build verde, 7 DTOs, `mvn -B verify` verde
3. T005 (commit do contrato) → T006 (tracker) → T007 (CLAUDE.md) → T008 (commit final) — feature completa e fechada

---

## Notes

- [P] tasks = comandos/arquivos diferentes, sem dependência pendente entre si
- Nenhuma tarefa de teste Java é gerada — não se aplica; a rota nem tem implementação ainda (cadeia `T-004`)
- **Distinção da task irmã (`T-002-7-2`)**: T004 aqui confirma regressão zero por ausência estrutural de código a exercitar, não por preservação de comportamento existente — mesma conclusão prática, motivo diferente
- **T005–T008 (commits e atualização de `TASKS.json`/`CLAUDE.md`) existem porque esta lista é executada por um agente que segue cada tarefa estritamente como descrita** — deixar esses passos implícitos significaria que ninguém os executa. Mensagens de commit e o padrão de dois commits (contrato / hb-catalog-service) seguem o precedente real já registrado no histórico desta cadeia (commit `29a9570`, T-002-7-2; commit `5b158a9` em `platform-shared-contracts`)
