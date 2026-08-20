---

description: "Task list template for feature implementation"
---

# Tasks: Referenciar ProblemDetail no 404 de getProductById

**Input**: Design documents from `specs/013-getproductbyid-404-ref/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md — todos completos e consistentes (checklists `requirements.md` e `api.md` resolvidas antes desta geração)

**Tests**: Não se aplica — nenhum código Java muda (o serviço já produz `ProblemDetail` em runtime). Por Constitution Principle III, "tests" aqui significa validação de build do contrato + regressão do consumidor, não `*Test.java`/`*IT.java` novos.

**Organization**: Uma única user story (P1) — mudança pequena o suficiente para não justificar Setup/Foundational separados.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode rodar em paralelo (arquivos/comandos diferentes, sem dependência pendente)
- **[Story]**: US1 (única story desta feature)

## Path Conventions

Mudança em um único arquivo de contrato compartilhado, consumido pelo `hb-catalog-service`. **`platform-shared-contracts` é um repositório irmão** de `hb-catalog-service` (ambos sob `hubinity/`), não um subdiretório dele — todos os caminhos abaixo assumem a raiz de `hb-catalog-service` como cwd e usam `../platform-shared-contracts` explicitamente (achado do `/speckit-analyze`, H1; convenção já estabelecida em `hb-catalog-service/CLAUDE.md`). Arquivo alterado: `../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`.

---

## Phase 1: Setup

**Não se aplica.** Nenhuma inicialização — mudança em um arquivo já existente.

## Phase 2: Foundational

**Não se aplica.** A única dependência (`T-002-7-1`, schema `ProblemDetail` declarado) já está `done`; nada bloqueia esta task além disso.

---

## Phase 3: User Story 1 - Consumidor sabe o formato do corpo de erro ao buscar um produto inexistente (Priority: P1) 🎯 MVP

**Goal**: O desfecho `404` de `getProductById` declara `content` referenciando `ProblemDetail`, preservando a `description` existente.

**Independent Test**: `content.application/json.schema` do desfecho `404` de `getProductById` referencia `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde (ver spec, User Story 1).

- [X] T001 [US1] Adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` da operação `getProductById` em `../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (repo irmão, um nível acima de `hb-catalog-service`), preservando a `description` "Product not found" existente
- [X] T002 [P] [US1] Confirmar via `git -C ../platform-shared-contracts diff contracts-catalog/openapi/catalog.yaml` que a mudança é estritamente aditiva (nenhuma linha `-`, exceto a reformatação mínima necessária para acomodar `content` sob o `404`) — se o diff mostrar remoção de conteúdo pré-existente (`description`, desfecho `200`, ou qualquer outro schema), reverter e refazer (depends on T001)
- [X] T003 [US1] A partir da raiz de `hb-catalog-service`, rodar `( cd ../platform-shared-contracts && mvn -B -DskipTests install )` e confirmar que `find ../platform-shared-contracts/contracts-catalog/target/generated-sources/openapi -type f -path '*/com/hubinity/contracts/catalog/dto/*.java' | wc -l` retorna 7 (nenhum schema novo, nenhum removido) (depends on T001)
- [X] T004 [US1] Na raiz de `hb-catalog-service`, rodar `mvn -B verify` e confirmar build verde, sem alteração em nenhum teste pré-existente (regressão zero no consumidor) (depends on T003)

**Checkpoint**: US1 completa — a única story desta feature. Feature pronta.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Não se aplica.** Mudança pequena demais para justificar uma fase de polimento separada — T002–T004 já cobrem verificação de diff, build, e regressão.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup/Foundational**: N/A
- **User Story 1**: única fase — T001 é o único passo com edição real; T002, T003, T004 são verificações sequenciais, todas dependentes de T001

### Parallel Opportunities

- T002 pode rodar em paralelo a T003 (comandos independentes — `git diff` não depende do resultado do build), ambos após T001
- T004 depende do resultado de T003 (precisa que `contracts-catalog` já esteja reinstalado)

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
2. **PARAR e VALIDAR**: diff aditivo, build verde, 7 DTOs, `mvn -B verify` verde — feature completa

---

## Notes

- [P] tasks = comandos/arquivos diferentes, sem dependência pendente entre si
- Nenhuma tarefa de teste Java (`*Test.java`/`*IT.java`) é gerada — não se aplica, nenhum código do serviço muda
- **Achado do checklist `api.md` (CHK010)**: nenhuma tarefa dedicada a "referência `$ref` quebrada" — o build em T003 já é o gate que detectaria isso (falha de geração se o nome do schema estivesse errado), então uma tarefa separada seria redundante dado o tamanho trivial da mudança
- Nenhuma tarefa desta lista atualiza `TASKS.json` — isso permanece a cargo do usuário
