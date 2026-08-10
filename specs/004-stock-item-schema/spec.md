# Feature Specification: Schema de resposta da operação de leitura de saldo de estoque

**Feature Branch**: `004-stock-item-schema`

**Created**: 2026-07-23

**Status**: Draft

**Input**: User description: "detalhe a task T-001-4 do TASKS.json: 'Definir schema de resposta StockItemResponse da operação de leitura de saldo de estoque em contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts'. Use o código real de platform-shared-contracts como contexto técnico absoluto."

**Task de origem**: `T-001-4` (TASKS.json, fase `contracts`) — depende de `T-001-3` (**concluída**, commit `4ddfd2c`).

## Contexto técnico verificado (código real)

- **Contrato** (`catalog.yaml`): a operação `getStockItemByProductId` está completa até as respostas — `'200'` ainda **sem `content`** (deferido pela feature 002 para esta task) e `'404'` único. O schema `components/schemas/StockItem` existe com campos **divergentes da realidade do serviço**: `productId`, `quantityOnHand (int64)`, `reorderLevel (int64)`, `lastMovementAt (date-time)` — divergência registrada desde a feature 001.
- **Serviço** (`hb-catalog-service`): o record `api/dto/StockItemResponse.java` **já existe** e é a projeção pública real do saldo: `productId (UUID)`, `available (Integer)`, `reserved (Integer)`, `reorderPoint (Integer)`, `updatedAt (Instant)`. É retornado hoje dentro de `StockMovementResult` e `StockReservationResult` e será o corpo do GET (task T-004-3).
- **Consumo do artefato**: `contracts-catalog:0.1.0-SNAPSHOT` é build-only (ADR 0006) — **nenhum consumidor publicado** depende dos campos atuais do schema `StockItem`; mudança breaking no schema é segura neste estágio.

## Decisões do usuário (registradas na confirmação do pipeline)

1. **Reusar o schema `StockItem`** como corpo da `'200'` (referência direta) — o rótulo "StockItemResponse" do tracker é o nome da entrega, não do schema; nenhum schema duplicado é criado.
2. **Alinhar os campos ao serviço** (fonte de verdade): o schema `StockItem` é **reescrito** para `productId`, `available`, `reserved`, `reorderPoint`, `updatedAt`, substituindo os campos especulativos originais.

## Decisão de escopo desta task

Entregáveis: (a) reescrita do schema `components/schemas/StockItem` em paridade com o record real do serviço; (b) `content: application/json` na resposta `'200'` referenciando `#/components/schemas/StockItem`; (c) atualização da `description` do Path Item (resta apenas autorização — T-001-5). Fora de escopo: `security` (T-001-5), corpo de erro do `'404'` (padrão RFC 7807 do serviço, não modelado no contrato nesta fase), implementação (T-004-x).

**Schema alvo (paridade com `StockItemResponse.java`)**:

```yaml
StockItem:
  type: object
  description: Current stock balance snapshot for a product.
  required:
    - productId
    - available
    - reserved
    - reorderPoint
  properties:
    productId:
      type: string
      format: uuid
      description: Product unique identifier
    available:
      type: integer
      format: int32
      minimum: 0
      description: Units available for sale or reservation
    reserved:
      type: integer
      format: int32
      minimum: 0
      description: Units held by active reservations
    reorderPoint:
      type: integer
      format: int32
      minimum: 0
      description: Threshold that signals restocking
    updatedAt:
      type: string
      format: date-time
      description: Timestamp of the last counter change
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe exatamente o que a leitura de saldo retorna (Priority: P1)

Uma equipe consumidora abre o contrato e vê, na resposta `'200'` da operação de saldo, o corpo completo: os contadores reais (`available`, `reserved`, `reorderPoint`) e o carimbo da última mudança — os mesmos campos que o serviço de fato retorna, sem tradução nem campos fantasma.

**Why this priority**: É o entregável central — sem o corpo, o consumidor sabe o endereço e o verbo mas não o formato do dado; com campos divergentes, integraria contra uma mentira.

**Independent Test**: A `'200'` referencia `#/components/schemas/StockItem`; o schema tem os 5 campos do record `StockItemResponse.java` com tipos equivalentes; build do módulo verde e DTO `StockItem` gerado com os novos campos.

**Acceptance Scenarios**:

1. **Given** a operação `getStockItemByProductId`, **When** um consumidor inspeciona a `'200'`, **Then** encontra `content: application/json` com `$ref` para `StockItem`.
2. **Given** o schema `StockItem`, **When** comparado ao record `StockItemResponse.java` do serviço, **Then** os 5 campos coincidem em nome e tipo equivalente (UUID↔uuid, Integer↔int32, Instant↔date-time).
3. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros e o DTO gerado (`com.hubinity.contracts.catalog.dto.StockItem`) expõe os novos campos.

---

### User Story 2 - Divergência histórica do schema encerrada sem resíduos (Priority: P2)

Quem mantém o contrato vê a divergência registrada desde a feature 001 (campos especulativos `quantityOnHand`/`reorderLevel`/`lastMovementAt`) eliminada: nenhum campo antigo sobrevive, nenhum consumidor quebra (artefato build-only), e a description do Path Item passa a apontar só a autorização como pendência.

**Why this priority**: Encerra a última dívida estrutural do schema antes de T-001-5 e da implementação T-004-x.

**Independent Test**: grep dos nomes antigos retorna 0 ocorrências; regressão zero no consumidor (`mvn -B verify` verde); description do Path Item menciona apenas T-001-5.

**Acceptance Scenarios**:

1. **Given** o contrato editado, **When** buscados `quantityOnHand`, `reorderLevel`, `lastMovementAt`, **Then** nenhuma ocorrência resta no documento.
2. **Given** o consumidor `hb-catalog-service`, **When** `mvn -B verify` roda com o artefato reinstalado, **Then** permanece verde (nenhum código do serviço referencia o DTO gerado hoje).
3. **Given** a description do Path Item, **When** lida após a entrega, **Then** declara operação, parâmetro e corpo de resposta definidos, restando autorização (T-001-5).

---

### Edge Cases

- **Breaking change no schema**: os campos antigos somem — seguro porque o artefato é build-only (ADR 0006) e a verificação de regressão (US2) prova que o único consumidor não referencia o DTO gerado. Se `mvn -B verify` do consumidor falhar por referência ao DTO antigo, a correção do ponto de uso entra nesta entrega (improvável — verificado que não há referências).
- **`updatedAt` opcional**: no serviço, `updatedAt` pode ser nulo antes da primeira mudança de contadores; por isso fica fora de `required` (os 4 demais são inicializados no INSERT e sempre presentes).
- **Tipos int32 vs int64**: o contrato antigo usava int64; o serviço usa `Integer` (int32). Alinhamento ao serviço = int32 com `minimum: 0` (contadores nunca negativos).
- **Nome do DTO gerado**: continua `StockItem` (classe `com.hubinity.contracts.catalog.dto.StockItem`); o record local `StockItemResponse` do serviço permanece como projeção da API do serviço — a convergência de uso é decisão da implementação (T-004-x), não deste contrato.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O schema `components/schemas/StockItem` MUST ser reescrito para o bloco-alvo definido em §Decisão de escopo: 5 propriedades (`productId` uuid, `available`/`reserved`/`reorderPoint` int32 com `minimum: 0`, `updatedAt` date-time), `required: [productId, available, reserved, reorderPoint]`, description atualizada.
- **FR-002**: A resposta `'200'` da operação `getStockItemByProductId` MUST ganhar `content: application/json` cujo schema é `$ref: '#/components/schemas/StockItem'` — estrutura idêntica à `'200'` de `getProductById` (convenção).
- **FR-003**: Nenhuma ocorrência dos campos antigos (`quantityOnHand`, `reorderLevel`, `lastMovementAt`) MUST restar no documento após a edição.
- **FR-004**: A `description` do Path Item MUST ser atualizada para o texto: "Canonical read address for a product's on-hand stock snapshot. The GET operation, its productId parameter, and the response body are declared; authorization is declared by the follow-up contract task (T-001-5)."
- **FR-005**: A mudança MUST ficar restrita ao schema `StockItem`, ao bloco `'200'` da operação de saldo e à description do Path Item; demais paths, operações, tags e schemas (`Product`, `Category`, `StockMovement`) intactos; documento válido pela autoridade herdada (build do módulo).
- **FR-006**: A entrega MUST provar regressão zero no consumidor (`mvn -B verify` verde em `hb-catalog-service` com o artefato reinstalado), evidenciando que a mudança breaking do schema não atinge código existente.

### Key Entities

- **Schema `StockItem` (existente — reescrito)**: passa de projeção especulativa para paridade com o record real `StockItemResponse.java` do serviço; referenciado pela `'200'`.
- **Resposta `'200'` (existente — completada)**: ganha `content` com `$ref` — último elemento estrutural da operação antes da autorização.
- **Path Item do saldo (existente — description atualizada)**: pendência restante = T-001-5.
- **Record `StockItemResponse.java` (serviço — intocado)**: fonte de verdade dos campos; nenhuma mudança no serviço nesta feature.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A `'200'` da operação de saldo referencia um schema cujos 5 campos coincidem em nome e tipo com o record real do serviço — verificável por comparação lado a lado.
- **SC-002**: O documento permanece válido e processável: build do módulo verde, DTO `StockItem` regenerado com os novos campos.
- **SC-003**: Divergência histórica encerrada: 0 ocorrências dos campos especulativos no contrato; regressão zero no consumidor comprovada.
- **SC-004**: A task T-001-5 (autorização) fica desbloqueada como única pendência estrutural da operação.

## Assumptions

- As duas decisões estruturais foram tomadas pelo usuário na confirmação do pipeline (2026-07-23): reusar `StockItem` (sem schema novo) e alinhar campos ao serviço — resolvem o achado L2 (feature 002) e a divergência registrada na feature 001.
- `required` exclui `updatedAt` porque o serviço pode ter contadores recém-criados sem atualização; os 4 demais campos são sempre presentes (inicializados no INSERT).
- Mudança breaking no schema é aceitável: artefato SNAPSHOT build-only (ADR 0006), sem consumidores publicados; FR-006 prova o caso do único consumidor real.
- Autoridade de validação e workflow herdados da cadeia (build do módulo; branch `feature/stock-balance-path`; commits no polish).
- O corpo de erro do `'404'` (ProblemDetail RFC 7807) permanece não modelado no contrato — padrão transversal do serviço, fora do escopo da cadeia atual.

## Out of Scope

- Requisito de autorização/`security` (T-001-5).
- Modelagem de ProblemDetail/corpo de erro no contrato.
- Implementação do endpoint e convergência DTO gerado × record local (T-004-x).
- Qualquer mudança em código Java do serviço.
