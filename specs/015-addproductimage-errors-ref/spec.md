# Feature Specification: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

**Feature Branch**: `015-addproductimage-errors-ref`

**Created**: 2026-07-29

**Status**: Draft

**Input**: User description: "Referenciar o schema ProblemDetail (definido em platform-shared-contracts) no content dos desfechos de erro 400, 403 e 404 da operação addProductImage em contracts-catalog/openapi/catalog.yaml. Escopo: T-002-7-4 (400), T-002-7-5 (403), T-002-7-6 (404)."

**Tasks de origem**: `T-002-7-4`, `T-002-7-5`, `T-002-7-6` (TASKS.json, fase `contracts`, todas `decomposition_allowed: false`) — cadeia `depends_on` linear: `T-002-7-4 → T-002-7-3` (**concluída**, spec `014-getstockitem-404-ref`), `T-002-7-5 → T-002-7-4`, `T-002-7-6 → T-002-7-5` (ambas dentro deste mesmo escopo). **Últimas três das cinco subtarefas de referência** da cadeia `T-002-7` — encerram a cadeia após esta feature.

## Contexto técnico verificado (código real)

- **Os três desfechos de erro de `addProductImage` hoje são `description`-only.** Trecho exato do documento (`platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, operação `addProductImage`):
  ```yaml
        '400':
          description: Malformed request body, or url absent, not a valid URI, or too long
        '403':
          description: Authenticated principal lacks the required admin role
        '404':
          description: Product not found
  ```
- **A operação `addProductImage` não tem implementação no serviço ainda.** Varredura de `ProductController.java` confirma que não existe nenhum `@PostMapping`/`@GetMapping`/`@PatchMapping` relacionado a imagem de produto — apenas os endpoints padrão de CRUD/consulta de produto (`POST /`, `GET /{id}`, `GET /`, `GET /{id}/price-history`). Nenhum arquivo do serviço referencia rotas de imagem (`grep` por "image"/"Image" em `src/main/java` não retorna nada). A funcionalidade de imagens de produto é exatamente o que as cadeias `T-002` (contrato — em progresso, é esta feature) e `T-003` (domínio: `T-003-1..5`, todas `refined`, nenhuma `done`) ainda vão construir.
- **O contrato já descreve a operação por completo**: `requestBody` (`ProductImageRequest`), resposta `201` (`ProductImageResponse`, já referenciando `content`), e os três desfechos de erro `400`/`403`/`404` (hoje description-only, alvo desta feature). O contrato roda à frente da implementação — mesmo padrão já visto em `013-getproductbyid-404-ref` e `014-getstockitem-404-ref`.
- **O schema `ProblemDetail` já está em uso real**, com duas referências existentes: `404` de `getProductById` (`T-002-7-2`) e `404` de `getStockItemByProductId` (`T-002-7-3`). Esta feature adiciona a terceira, quarta e quinta referência — uma para cada desfecho de erro de `addProductImage`.
- **Consequência para verificação de regressão**: como nenhum código de `hb-catalog-service` implementa `addProductImage`, não há comportamento de runtime a regredir. `mvn -B verify` continuar verde é confirmação estrutural de ausência de impacto, mesmo padrão de `014-getstockitem-404-ref`.

## Decisão de escopo desta feature

**Entregável único**: adicionar `content: application/json: schema: $ref: '#/components/schemas/ProblemDetail'` aos três desfechos `400`, `403` e `404` da operação `addProductImage`, preservando as `description` existentes de cada um.

**Fora de escopo**: qualquer outro desfecho da mesma operação (`201`, inalterado); qualquer desfecho de erro de outras operações do documento (já cobertos pelas features `013`/`014`, ou fora da cadeia `T-002-7`); a implementação da própria operação `addProductImage` (`ProductController`/`ProductService`, cadeias `T-002`/`T-003`, em progresso/`refined`, não iniciada); o próprio schema `ProblemDetail` (já declarado, `T-002-7-1`).

**Elemento alvo** — três modificações pontuais, sem remoção de conteúdo existente:

```yaml
        '400':
          description: Malformed request body, or url absent, not a valid URI, or too long
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
        '403':
          description: Authenticated principal lacks the required admin role
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
        '404':
          description: Product not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProblemDetail'
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe o formato do corpo de erro ao registrar uma imagem inválida ou malformada (Priority: P1)

Quem for consumir `addProductImage` — quando as cadeias `T-002`/`T-003` a implementarem — já encontra, no contrato, o formato exato do corpo de erro (`ProblemDetail`) para o caso de requisição malformada (`400`: URL ausente, inválida ou muito longa), em vez de um desfecho `description`-only sem shape definido.

**Why this priority**: é o desfecho de erro mais provável de ser exercitado em uso real (validação de entrada do cliente) entre os três alvos desta feature.

**Independent Test**: o desfecho `400` de `addProductImage` tem `content.application/json.schema` referenciando `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o desfecho `400` de `addProductImage`, **Then** encontra um `content.application/json.schema` que referencia `ProblemDetail`.
2. **Given** a mudança, **When** comparada à versão anterior, **Then** a `description` "Malformed request body, or url absent, not a valid URI, or too long" permanece idêntica — apenas `content` foi acrescentado.

---

### User Story 2 - Consumidor sabe o formato do corpo de erro ao tentar registrar imagem sem a role de admin (Priority: P2)

Quem for consumir `addProductImage` sem a role `admin` já encontra, no contrato, o formato exato do corpo de erro (`ProblemDetail`) para o caso de autorização insuficiente (`403`).

**Why this priority**: cenário de autorização, testável de forma independente do `400`/`404`, e menos frequente em uso real do que validação de entrada.

**Independent Test**: o desfecho `403` de `addProductImage` tem `content.application/json.schema` referenciando `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o desfecho `403` de `addProductImage`, **Then** encontra um `content.application/json.schema` que referencia `ProblemDetail`.
2. **Given** a mudança, **When** comparada à versão anterior, **Then** a `description` "Authenticated principal lacks the required admin role" permanece idêntica — apenas `content` foi acrescentado.

---

### User Story 3 - Consumidor sabe o formato do corpo de erro ao registrar imagem para um produto inexistente (Priority: P3)

Quem for consumir `addProductImage` para um `productId` inexistente já encontra, no contrato, o formato exato do corpo de erro (`ProblemDetail`) para o caso de produto não encontrado (`404`).

**Why this priority**: terceiro e último desfecho de erro da operação, mesma prioridade estrutural das duas anteriores — completa a cobertura de erro da operação e encerra a cadeia `T-002-7`.

**Independent Test**: o desfecho `404` de `addProductImage` tem `content.application/json.schema` referenciando `#/components/schemas/ProblemDetail`; build do módulo `contracts-catalog` verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o desfecho `404` de `addProductImage`, **Then** encontra um `content.application/json.schema` que referencia `ProblemDetail`.
2. **Given** a mudança, **When** comparada à versão anterior, **Then** a `description` "Product not found" permanece idêntica — apenas `content` foi acrescentado.
3. **Given** o desfecho `201` da mesma operação, **When** inspecionado após a mudança, **Then** permanece exatamente como antes (referenciando `ProductImageResponse`, sem alteração).

---

### Edge Cases

- **A operação não tem implementação ainda** — as três `description` ("malformed...", "lacks the required admin role", "Product not found") descrevem comportamento que as cadeias `T-002`/`T-003` ainda vão construir, não handlers já existentes. Referenciar `ProblemDetail` aqui é uma declaração antecipada de shape, consistente com o padrão já usado pelas features `013`/`014`. Não é um erro desta feature — é o mesmo padrão de sequenciamento já estabelecido no projeto.
- **Quando a implementação chegar**, os handlers que produzirem esses três desfechos (validação de `ProductImageRequest` para o 400, `@PreAuthorize("hasRole('admin')")` para o 403, `ProductNotFoundException` reaproveitada para o 404 — já usada no 404 de `getProductById`) devem produzir um `ProblemDetail` compatível com o schema aqui referenciado; isso é responsabilidade das cadeias `T-002`/`T-003`, não desta feature.
- **`instance` continua sempre ausente por convenção** (nenhum handler do projeto o preenche, `T-002-7-1`) — mesma tolerância já embutida no schema genérico.
- **Ordem de aplicação das três mudanças**: como as três tasks (`T-002-7-4/5/6`) têm `depends_on` encadeado entre si, o contrato deve chegar ao estado final com as três referências presentes simultaneamente; não há estado intermediário observável fora desta feature.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O desfecho `400` da operação `addProductImage` MUST declarar `content` do tipo `application/json` com `schema` referenciando `#/components/schemas/ProblemDetail`.
- **FR-002**: O desfecho `403` da operação `addProductImage` MUST declarar `content` do tipo `application/json` com `schema` referenciando `#/components/schemas/ProblemDetail`.
- **FR-003**: O desfecho `404` da operação `addProductImage` MUST declarar `content` do tipo `application/json` com `schema` referenciando `#/components/schemas/ProblemDetail`.
- **FR-004**: As `description` existentes dos três desfechos ("Malformed request body, or url absent, not a valid URI, or too long"; "Authenticated principal lacks the required admin role"; "Product not found") MUST permanecer inalteradas.
- **FR-005**: Nenhum outro desfecho, operação, ou schema do documento MUST ser alterado por esta feature — mudança estritamente pontual aos três desfechos de erro de `addProductImage`. Critério objetivo: no diff, as únicas linhas novas são os três blocos `content`; nenhuma linha `-` (remoção).
- **FR-006**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-007**: A geração de DTOs MUST permanecer estável — referenciar um schema existente em três novos locais não cria nem remove nenhuma classe gerada.
- **FR-008**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde no `hb-catalog-service`, medido nesta execução. Como a operação `addProductImage` ainda não tem implementação (cadeias `T-002`/`T-003`, pendentes), nenhum código Java é exercitado por esta verificação — a ausência de regressão é estrutural, não apenas observada.

### Key Entities

- **Desfecho `400` de `addProductImage` (modificado)**: ganha `content` referenciando `ProblemDetail`; `description` inalterada.
- **Desfecho `403` de `addProductImage` (modificado)**: ganha `content` referenciando `ProblemDetail`; `description` inalterada.
- **Desfecho `404` de `addProductImage` (modificado)**: ganha `content` referenciando `ProblemDetail`; `description` inalterada.
- **Schema `ProblemDetail` (pré-existente, inalterado)**: terceira, quarta e quinta referência real no documento (anteriores: `getProductById` 404, `getStockItemByProductId` 404).
- **Desfecho `201` de `addProductImage` — inalterado, fora de escopo**.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Quem consultar o contrato consegue determinar o formato do corpo de erro dos três desfechos de `addProductImage` (400/403/404) sem sair da especificação — 100% de cobertura de tipo para esses desfechos, ante 0% antes desta feature.
- **SC-002**: Nenhuma regressão em consumidores existentes — `mvn -B verify` verde no `hb-catalog-service`.
- **SC-003**: A cadeia `T-002-7` é concluída: as cinco subtarefas de referência (`T-002-7-1` a `T-002-7-6`, contando o schema em si) ficam completas, cada desfecho de erro do documento hoje declarado (`getProductById` 404, `getStockItemByProductId` 404, `addProductImage` 400/403/404) referenciando `ProblemDetail`.

## Assumptions

- **Nenhuma decisão de produto pendente**: a forma referenciada (`ProblemDetail`) já foi definida em `T-002-7-1` e já tem dois usos reais de referência estabelecidos pelas features `013`/`014`; esta feature estende o mesmo padrão a três novos locais na mesma operação.
- **A ausência de implementação da operação (`T-002`/`T-003`, em progresso/pendente) não bloqueia esta feature** — o contrato já descreve a operação por completo há mais tempo do que sua implementação existe, e referenciar um schema de erro não pressupõe que o erro já seja produzido em runtime.
- **`contracts-catalog` deve ser reinstalado** (`mvn -B -DskipTests install`) antes de o `hb-catalog-service` recompilar, herdado do fluxo já estabelecido pelas features anteriores da cadeia `T-002-7`.
- **As três mudanças (400/403/404) são entregues juntas nesta feature** porque compartilham a mesma operação, o mesmo schema-alvo e o mesmo padrão de mudança pontual — decompô-las em três features separadas não agregaria valor de revisão adicional, ao contrário do padrão adotado para `013`/`014` (que cobriam operações distintas).

## Out of Scope

- Referenciar `ProblemDetail` em qualquer desfecho de outra operação do documento (já cobertos por `013-getproductbyid-404-ref` e `014-getstockitem-404-ref`).
- Implementar a operação `addProductImage` em si (`ProductController`/`ProductService`, cadeias `T-002`/`T-003`).
- Qualquer alteração ao schema `ProblemDetail` em si, ou ao desfecho `201` de `addProductImage`.
