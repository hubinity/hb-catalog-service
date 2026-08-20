# Feature Specification: Referenciar ProblemDetail no 404 de getProductById

**Feature Branch**: `013-getproductbyid-404-ref`

**Created**: 2026-07-29

**Status**: Draft

**Input**: User description: "T-002-7-2: Referenciar o schema ProblemDetail no content do desfecho 404 da operação getProductById em contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts."

**Task de origem**: `T-002-7-2` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-7-1]` (**concluída**, spec `012-problemdetail-schema`). **Primeira das cinco subtarefas de referência** em que a cadeia `T-002-7` avança após a declaração do schema; as demais (`T-002-7-3..6`) cobrem os outros quatro desfechos de erro do documento e permanecem fora de escopo.

## Contexto técnico verificado (código real)

- **O schema `ProblemDetail` existe e está órfão.** Declarado em `components/schemas` (T-002-7-1, `012-problemdetail-schema`), com os cinco membros RFC 7807 (`type`, `title`, `status`, `detail`, `instance`), todos opcionais. Varredura confirma **zero referências** a `ProblemDetail` em todo o documento — esta task é a primeira a usá-lo.
- **O desfecho `404` de `getProductById` hoje é `description`-only.** Trecho exato do documento (`paths./api/v1/products/{id}.get.responses.404`):
  ```yaml
          '404':
            description: Product not found
  ```
  Sem `content`, sem `schema` — nenhum tipo de corpo declarado para este erro.
- **Comportamento real do serviço** (`ApiExceptionHandler.handleProductNotFound`, verificado em código): produz `ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage())`, com `title = "Product not found"` e `type = urn:hubinity:catalog:product-not-found` setados explicitamente; `instance` nunca é setado em nenhum handler do arquivo (varredura por `setInstance` — zero ocorrências). `detail` é dinâmico (mensagem da exceção, tipicamente contendo o UUID do produto não encontrado).
- **`getProductById` é a única operação afetada por esta task.** As outras quatro referências pendentes (`getStockItemByProductId` 404, `addProductImage` 400/403/404) pertencem a `T-002-7-3` até `T-002-7-6`, já registradas em `TASKS.json` com `depends_on` encadeado a partir desta.
- **Nenhuma outra parte do desfecho `404` muda.** A `description` existente ("Product not found") permanece — a mudança é acrescentar `content`, não reescrever o que já está lá.

## Decisão de escopo desta task

**Entregável único**: adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` de `getProductById`, preservando a `description` existente.

**Fora de escopo**: qualquer outro desfecho de erro do documento (`getStockItemByProductId` 404 — `T-002-7-3`; `addProductImage` 400/403/404 — `T-002-7-4`, `T-002-7-5`, `T-002-7-6`); o desfecho `200` de `getProductById` (inalterado); o próprio schema `ProblemDetail` (já declarado, `T-002-7-1`); a propriedade de extensão `errors` (não aplicável a este erro — `ProductNotFoundException` não é um erro de validação de campo).

**Elemento alvo** — uma modificação pontual, sem remoção de conteúdo existente:

```yaml
        '404':
          description: Product not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe o formato do corpo de erro ao buscar um produto inexistente (Priority: P1)

Quem consome `getProductById` (hoje `hb-catalog-web`; futuramente `sc-order-service`, Fase 3) e recebe um `404` passa a ter, no próprio contrato, o formato exato do corpo de erro (`ProblemDetail`) — em vez de um desfecho `description`-only que não diz nada sobre o shape da resposta.

**Why this priority**: é o entregável único desta task e o primeiro passo concreto da cadeia `T-002-7` após a declaração do schema (`T-002-7-1`) — sem ele, o schema `ProblemDetail` permanece sem nenhum consumidor real no contrato.

**Independent Test**: o desfecho `404` de `getProductById` tem `content.application/json.schema` referenciando `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o desfecho `404` de `getProductById`, **Then** encontra um `content.application/json.schema` que referencia `ProblemDetail`.
2. **Given** a mudança, **When** comparada à versão anterior, **Then** a `description` "Product not found" permanece idêntica — apenas `content` foi acrescentado.
3. **Given** o desfecho `200` da mesma operação, **When** inspecionado após a mudança, **Then** permanece exatamente como antes (referenciando `Product`, sem alteração).

---

### Edge Cases

- **`detail` é dinâmico (contém o UUID do produto), mas o schema `ProblemDetail` não modela um formato específico para `detail`** — é `type: string` genérico, já declarado em `T-002-7-1`. Nenhuma mudança de schema é necessária aqui; a referência simplesmente aponta para a forma já existente.
- **`instance` nunca é preenchido por `handleProductNotFound`** — isso já é esperado pelo schema (todos os membros são opcionais, `T-002-7-1`, FR-009 de `012-problemdetail-schema`). Não é uma lacuna desta task: o schema já tolera a ausência.
- **Este é o primeiro consumo real do schema `ProblemDetail`** — antes desta task, ele existia mas não era referenciado por nenhuma operação (órfão intencional, conforme `012-problemdetail-schema`). Depois desta task, o schema deixa de ser órfão, mas os outros quatro desfechos de erro (`T-002-7-3..6`) continuam description-only até suas próprias tasks.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O desfecho `404` da operação `getProductById` MUST declarar `content` do tipo `application/json` com `schema` referenciando `#/components/schemas/ProblemDetail`.
- **FR-002**: A `description` existente do desfecho `404` ("Product not found") MUST permanecer inalterada.
- **FR-003**: Nenhum outro desfecho, operação, ou schema do documento MUST ser alterado por esta task — mudança estritamente pontual. Critério objetivo: no diff, a única linha nova é o bloco `content` do `404` de `getProductById`; nenhuma linha `-` (remoção).
- **FR-004**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-005**: A geração de DTOs MUST permanecer em 7 schemas ↔ 7 DTOs (contagem já estabelecida por `T-002-7-1`) — referenciar um schema existente em um novo local não cria nem remove nenhuma classe gerada.
- **FR-006**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde no `hb-catalog-service`, medido nesta execução.

### Key Entities

- **Desfecho `404` de `getProductById` (modificado)**: ganha `content` referenciando `ProblemDetail`; `description` inalterada.
- **Schema `ProblemDetail` (pré-existente, inalterado)**: primeira referência real no documento.
- **Demais desfechos de erro (`getStockItemByProductId` 404, `addProductImage` 400/403/404) — inalterados, fora de escopo, alvo de `T-002-7-3..6`**.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quem consome o contrato consegue determinar o formato do corpo de erro de `getProductById` 404 sem sair da especificação — 100% de cobertura de tipo para este desfecho, ante 0% antes da task.
- **SC-002**: Nenhuma regressão em consumidores existentes — `mvn -B verify` verde no `hb-catalog-service`.
- **SC-003**: A cadeia `T-002-7` avança: restam `T-002-7-3` até `T-002-7-6`, cada uma referenciando o mesmo schema já disponível em um desfecho distinto, sem retrabalho de schema.

## Assumptions

- **Nenhuma decisão de produto pendente**: a forma referenciada (`ProblemDetail`) já foi definida e ratificada em `T-002-7-1`; esta task é puramente uma referência a um schema já existente, não uma nova decisão de shape.
- **`contracts-catalog` deve ser reinstalado** (`mvn -B -DskipTests install`) antes de o `hb-catalog-service` recompilar, herdado do fluxo já estabelecido pela cadeia `T-002`.

## Out of Scope

- Referenciar `ProblemDetail` no `content` de `getStockItemByProductId` 404 (**T-002-7-3**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 400 (**T-002-7-4**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 403 (**T-002-7-5**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 404 (**T-002-7-6**).
- Qualquer alteração ao schema `ProblemDetail` em si, ou ao desfecho `200` de `getProductById`.
