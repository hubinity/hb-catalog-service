# Feature Specification: Referenciar ProblemDetail no 404 de getStockItemByProductId

**Feature Branch**: `014-getstockitem-404-ref`

**Created**: 2026-07-29

**Status**: Draft

**Input**: User description: "T-002-7-3: Referenciar o schema ProblemDetail no content do desfecho 404 da operação getStockItemByProductId em contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts."

**Task de origem**: `T-002-7-3` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-7-2]` (**concluída**, spec `013-getproductbyid-404-ref`, verificado: o contrato já tem `content` referenciando `ProblemDetail` no 404 de `getProductById`). **Segunda das cinco subtarefas de referência** da cadeia `T-002-7`; restam `T-002-7-4..6` (todas em `addProductImage`) após esta.

## Contexto técnico verificado (código real)

- **O desfecho `404` de `getStockItemByProductId` hoje é `description`-only.** Trecho exato do documento:
  ```yaml
          '404':
            description: Stock balance not found for the given product (unknown product or no stock record)
  ```
- **Diferença crítica em relação à task irmã (`T-002-7-2`): esta operação não tem implementação no serviço ainda.** Varredura de `StockController.java` confirma **zero** `@GetMapping` para `/api/v1/products/{productId}/stock` (só existe `@GetMapping("/api/v1/products/{productId}/stock/movements")`, uma rota distinta). A leitura de saldo de estoque (`getStockItemByProductId`) é exatamente o que a cadeia `T-004` (`T-004-1` a `T-004-4`, todas `refined`, nenhuma `done`) ainda vai construir — método no `StockService`, handler no `StockController`, mapeamento de resposta, definição de path. **Nenhum exception handler existe hoje para o caso "estoque não encontrado"** desta rota especificamente, porque a rota em si não existe.
- **O contrato já descreve a operação por completo** (`getStockItemByProductId`: parâmetro `productId`, resposta `200` com `StockItem`, resposta `404` description-only) — isso é intencional e já documentado no próprio `description` do Path Item: "the GET operation, its productId parameter, the response body, and the authorization requirement are fully declared (T-001 chain complete)". O contrato roda à frente da implementação, mesmo padrão já visto na cadeia `T-002`/`T-003`/`T-005` para imagens de produto.
- **O schema `ProblemDetail` já está em uso real** (não mais órfão desde `T-002-7-2`): referenciado no `404` de `getProductById`. Esta task adiciona a segunda referência.
- **Consequência para verificação de regressão**: como nenhum código de `hb-catalog-service` implementa esta rota, não há comportamento de runtime a regredir. `mvn -B verify` continuar verde aqui é uma confirmação ainda mais direta de ausência de impacto — não porque um handler existente foi preservado (como em `T-002-7-2`), mas porque nenhum handler existe para este path ainda.

## Decisão de escopo desta task

**Entregável único**: adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` de `getStockItemByProductId`, preservando a `description` existente.

**Fora de escopo**: qualquer outro desfecho de erro do documento (`addProductImage` 400/403/404 — `T-002-7-4..6`); o desfecho `200` de `getStockItemByProductId` (inalterado); a implementação da rota em si (`StockService`/`StockController`, cadeia `T-004`, `refined`, não iniciada); o próprio schema `ProblemDetail` (já declarado, `T-002-7-1`).

**Elemento alvo** — uma modificação pontual, sem remoção de conteúdo existente:

```yaml
        '404':
          description: Stock balance not found for the given product (unknown product or no stock record)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe o formato do corpo de erro ao consultar saldo de estoque de um produto inexistente (Priority: P1)

Quem for consumir `getStockItemByProductId` — quando a cadeia `T-004` a implementar — já encontra, no contrato, o formato exato do corpo de erro (`ProblemDetail`) para o caso de estoque/produto não encontrado, em vez de um desfecho `description`-only sem shape definido.

**Why this priority**: é o entregável único desta task e avança a cadeia `T-002-7` para sua segunda referência real — sem ele, o contrato descreveria a leitura de estoque com um erro sem forma definida, exatamente quando a implementação (`T-004`) for começar a consumi-lo.

**Independent Test**: o desfecho `404` de `getStockItemByProductId` tem `content.application/json.schema` referenciando `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o desfecho `404` de `getStockItemByProductId`, **Then** encontra um `content.application/json.schema` que referencia `ProblemDetail`.
2. **Given** a mudança, **When** comparada à versão anterior, **Then** a `description` "Stock balance not found for the given product (unknown product or no stock record)" permanece idêntica — apenas `content` foi acrescentado.
3. **Given** o desfecho `200` da mesma operação, **When** inspecionado após a mudança, **Then** permanece exatamente como antes (referenciando `StockItem`, sem alteração).

---

### Edge Cases

- **A rota não tem implementação ainda** — a `description` do 404 ("unknown product or no stock record") descreve um comportamento que a cadeia `T-004` ainda vai construir, não um handler já existente. Referenciar `ProblemDetail` aqui é uma declaração antecipada de shape, consistente com o padrão já usado pela cadeia `T-002` para imagens de produto (contrato à frente da implementação). Não é um erro desta task — é o mesmo padrão de sequenciamento já estabelecido no projeto.
- **Quando `T-004` implementar a rota**, o handler que produzir esse `404` (provavelmente reaproveitando `ProductNotFoundException` — a `description` já cobre "unknown product" — ou uma exceção nova para "no stock record") deve produzir um `ProblemDetail` compatível com o schema aqui referenciado; isso é responsabilidade de `T-004`, não desta task.
- **`instance` continua sempre ausente por convenção** (nenhum handler do projeto o preenche, `T-002-7-1`) — mesma tolerância já embutida no schema genérico.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O desfecho `404` da operação `getStockItemByProductId` MUST declarar `content` do tipo `application/json` com `schema` referenciando `#/components/schemas/ProblemDetail`.
- **FR-002**: A `description` existente do desfecho `404` ("Stock balance not found for the given product (unknown product or no stock record)") MUST permanecer inalterada.
- **FR-003**: Nenhum outro desfecho, operação, ou schema do documento MUST ser alterado por esta task — mudança estritamente pontual. Critério objetivo: no diff, a única linha nova é o bloco `content` do `404` de `getStockItemByProductId`; nenhuma linha `-` (remoção).
- **FR-004**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-005**: A geração de DTOs MUST permanecer em 7 schemas ↔ 7 DTOs — referenciar um schema existente em um novo local não cria nem remove nenhuma classe gerada.
- **FR-006**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde no `hb-catalog-service`, medido nesta execução. Como a rota `getStockItemByProductId` ainda não tem implementação (cadeia `T-004`, pendente), nenhum código Java é exercitado por esta verificação — a ausência de regressão é estrutural, não apenas observada.

### Key Entities

- **Desfecho `404` de `getStockItemByProductId` (modificado)**: ganha `content` referenciando `ProblemDetail`; `description` inalterada.
- **Schema `ProblemDetail` (pré-existente, inalterado)**: segunda referência real no documento (primeira: `getProductById`, `T-002-7-2`).
- **Demais desfechos de erro (`addProductImage` 400/403/404) — inalterados, fora de escopo, alvo de `T-002-7-4..6`**.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quem consultar o contrato consegue determinar o formato do corpo de erro de `getStockItemByProductId` 404 sem sair da especificação — 100% de cobertura de tipo para este desfecho, ante 0% antes da task.
- **SC-002**: Nenhuma regressão em consumidores existentes — `mvn -B verify` verde no `hb-catalog-service`.
- **SC-003**: A cadeia `T-002-7` avança: restam `T-002-7-4` até `T-002-7-6`, todas em `addProductImage`, cada uma referenciando o mesmo schema já disponível.

## Assumptions

- **Nenhuma decisão de produto pendente**: a forma referenciada (`ProblemDetail`) já foi definida em `T-002-7-1` e já tem um segundo uso real de referência estabelecido por esta task, seguindo o mesmo padrão de `T-002-7-2`.
- **A ausência de implementação da rota (`T-004`, pendente) não bloqueia esta task** — o contrato já descreve a operação por completo há mais tempo do que sua implementação existe (mesmo padrão da cadeia de imagens de produto), e referenciar um schema de erro não pressupõe que o erro já seja produzido em runtime.
- **`contracts-catalog` deve ser reinstalado** (`mvn -B -DskipTests install`) antes de o `hb-catalog-service` recompilar, herdado do fluxo já estabelecido pela cadeia `T-002`.

## Out of Scope

- Referenciar `ProblemDetail` no `content` de `addProductImage` 400 (**T-002-7-4**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 403 (**T-002-7-5**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 404 (**T-002-7-6**).
- Implementar a rota `getStockItemByProductId` em si (`StockService`/`StockController`, cadeia `T-004`).
- Qualquer alteração ao schema `ProblemDetail` em si, ou ao desfecho `200` de `getStockItemByProductId`.
