# Feature Specification: Path canônico do endpoint de leitura de saldo de estoque no contrato compartilhado

**Feature Branch**: `001-stock-balance-path`

**Created**: 2026-07-22

**Status**: Draft

**Input**: User description: "detalhe a task T-001-1 descrita no arquivo TASKS.json deste repositório. Use o código real de platform-shared-contracts como contexto técnico absoluto."

**Task de origem**: `T-001-1` (TASKS.json, fase `contracts`) — "Definir path do endpoint de leitura de saldo de estoque em `contracts-catalog/openapi/catalog.yaml` com base em NÃO ESPECIFICADO NO PRD".

## Contexto técnico verificado (código real)

Estado atual de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`:

- Declara **um único path**: `/api/v1/products/{id}` (operação `getProductById`, parâmetro de path chamado `id`).
- Já possui os schemas `Product`, `Category`, `StockItem` (campos `productId`, `quantityOnHand`, `reorderLevel`, `lastMovementAt`) e `StockMovement` — ou seja, o conceito de "saldo de estoque por produto" já existe no contrato, mas **nenhum path o expõe**.
- ⚠️ As duas primeiras linhas do arquivo estão corrompidas (`UTCOME TESTS` / `12/1openapi: 3.1.0`), tornando o YAML inválido. Qualquer edição desta feature depende da correção desse cabeçalho para que o arquivo volte a ser um documento OpenAPI 3.1 parseável. (Correção realizada.)

Convenção real já implantada em `hb-catalog-service` (`StockController`):

| Recurso | Path real |
|---|---|
| Movimentos de estoque (escopo produto) | `/api/v1/products/{productId}/stock/movements` (POST e GET) |
| Reservas (escopo global) | `/api/v1/stock/reservations`, `/api/v1/stock/reservations/{id}/release`, `/api/v1/stock/reservations/{id}/commit` |

O saldo de estoque é um atributo de um produto específico; a convenção vigente para dados de estoque escopados a produto é o sub-recurso `.../products/{productId}/stock/...`.

## Decisão de escopo desta task

Esta especificação cobre **somente T-001-1**: estabelecer e registrar o **path canônico** do endpoint de leitura de saldo no contrato compartilhado. A declaração da operação GET (T-001-2), do parâmetro (T-001-3), do schema de resposta (T-001-4) e da autorização (T-001-5) são features subsequentes e estão fora de escopo aqui.

**Path canônico definido**: `/api/v1/products/{productId}/stock`

Justificativa (derivada do código, já que o PRD não especifica):

1. O saldo (`StockItem`) é identificado unicamente por `productId` — é um sub-recurso singular do produto.
2. `.../products/{productId}/stock/movements` já existe no serviço; o saldo no path pai `.../stock` é a leitura natural do agregado cujo histórico são os movements.
3. O nome do parâmetro `productId` (e não `id`) segue o padrão dos endpoints de estoque existentes e o campo `productId` do schema `StockItem` já presente no contrato.
4. Alternativa global (`/api/v1/stock/items/{productId}`) foi descartada: no serviço, o escopo global `/api/v1/stock/*` é usado apenas para reservas, cujo identificador é o id da reserva, não do produto.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor do contrato descobre onde ler o saldo de estoque (Priority: P1)

Uma equipe consumidora do catálogo (hoje `hb-catalog-web`; na Fase 3, `sc-order-service`) abre o contrato compartilhado do domínio Catalog e encontra, na lista de paths, um endereço único e inequívoco para consultar o saldo de estoque de um produto — sem precisar ler o código-fonte do serviço nem perguntar ao time de backend.

**Why this priority**: O contrato compartilhado é a fonte de verdade entre 12 repositórios independentes; sem o path registrado, cada consumidor inventaria o seu ou acoplaria ao código interno do serviço. É o único entregável da task.

**Independent Test**: Abrir `contracts-catalog/openapi/catalog.yaml`, verificar que a seção `paths` contém a entrada `/api/v1/products/{productId}/stock` e que o documento é um OpenAPI 3.1 válido.

**Acceptance Scenarios**:

1. **Given** o contrato compartilhado do domínio Catalog, **When** um consumidor procura pelo endpoint de saldo de estoque, **Then** encontra exatamente um path dedicado a esse fim: `/api/v1/products/{productId}/stock`.
2. **Given** o contrato com o novo path adicionado, **When** o documento é validado como OpenAPI 3.1, **Then** a validação passa sem erros (incluindo o cabeçalho do arquivo, hoje corrompido, já reparado).
3. **Given** os paths preexistentes do contrato, **When** o novo path é adicionado, **Then** nenhum path existente é alterado ou removido.

---

### User Story 2 - Contrato e serviço convergem para o mesmo endereço (Priority: P2)

A pessoa desenvolvedora que implementará a leitura de saldo no serviço de catálogo (tasks T-004-1 a T-004-4) usa o path registrado no contrato como referência única, garantindo que o endereço publicado aos consumidores e o endereço servido pelo backend sejam idênticos.

**Why this priority**: Evita divergência contrato × implementação — a task T-004-4 ("definir path do handler") depende diretamente da decisão registrada aqui.

**Independent Test**: Comparar o path declarado no contrato com a convenção dos endpoints de estoque já existentes no serviço e confirmar que o novo path se encaixa no mesmo padrão de sub-recurso (`.../products/{productId}/stock/...`).

**Acceptance Scenarios**:

1. **Given** o path canônico registrado no contrato, **When** a equipe backend for implementar o endpoint (T-004-x), **Then** não há nenhuma decisão de endereço a tomar — o path é copiado do contrato.
2. **Given** o path `/api/v1/products/{productId}/stock` e o path existente `/api/v1/products/{productId}/stock/movements`, **When** ambos coexistem, **Then** não há ambiguidade de roteamento (o path do saldo não captura requisições de movements, e vice-versa).

---

### Edge Cases

- **Arquivo de contrato atualmente inválido**: as duas primeiras linhas de `catalog.yaml` estão corrompidas; adicionar o path sem reparar o cabeçalho manteria o contrato inutilizável para geração de código. O reparo do cabeçalho é pré-condição. (Reparo concluído.)
- **Colisão de nome de parâmetro**: o path existente `/api/v1/products/{id}` usa `id`; o novo path usa `productId`. OpenAPI permite nomes de parâmetro diferentes em paths distintos — não há conflito, mas o nome deve ser `productId` em toda a hierarquia `.../stock/...` para consistência com o serviço.
- **Produto sem registro de estoque**: o comportamento (404 vs. saldo zero) é decisão da operação GET (T-001-2/T-001-4), não do path — fora de escopo aqui.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O contrato compartilhado do domínio Catalog MUST declarar o path `/api/v1/products/{productId}/stock` na seção `paths` de `contracts-catalog/openapi/catalog.yaml` como endereço canônico de leitura do saldo de estoque de um produto.
- **FR-002**: O path MUST usar `productId` como nome do parâmetro de template (não `id`), em consistência com os endpoints de estoque existentes e com o campo `productId` do schema `StockItem` já presente no contrato.
- **FR-003**: A adição do path MUST preservar intactos todos os paths, schemas e metadados existentes do contrato.
- **FR-004**: Após a edição, o documento MUST permanecer um OpenAPI 3.1 válido, com `openapi: 3.1.0` como primeira declaração do documento. (Pré-condição já satisfeita: o cabeçalho corrompido do working tree foi restaurado ao estado commitado — ver research R4; resta apenas confirmá-la antes da edição.)
- **FR-005**: O path MUST ser único no contrato — nenhum outro path pode expor leitura de saldo de estoque, evitando endereços concorrentes.
- **FR-006**: A entrada de path criada nesta task MAY permanecer sem operação declarada (a operação GET é escopo de T-001-2); se a ferramenta de validação exigir ao menos um item no path, a entrega desta task MUST ser coordenada com T-001-2 no mesmo commit, mantendo T-001-1 como a decisão de endereço.

### Key Entities

- **Path de contrato**: entrada da seção `paths` do documento OpenAPI; representa o endereço público e versionado de um recurso do domínio Catalog. Novo valor: `/api/v1/products/{productId}/stock`.
- **StockItem (schema já existente no contrato)**: fotografia do saldo de estoque de um produto; é o recurso que o novo path passa a endereçar. A reconciliação de seus campos (`quantityOnHand` no contrato × `available`/`reserved` no serviço) é escopo de T-001-4.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O contrato compartilhado contém exatamente 1 path dedicado à leitura de saldo de estoque (`/api/v1/products/{productId}/stock`), localizável por inspeção direta da seção `paths` — nenhum acesso ao código do serviço é necessário para descobri-lo.
- **SC-002**: O contrato do domínio Catalog volta a ser um documento válido e processável de ponta a ponta (validação OpenAPI 3.1 sem erros), desbloqueando a geração de DTOs do módulo.
- **SC-003**: O path registrado tem convergência evidenciada com as convenções reais do serviço (comparação documentada contra os endpoints de estoque existentes), de modo que as tasks de implementação (T-004-x) herdem o endereço sem nenhuma decisão de rota a tomar.
- **SC-004**: As 4 tasks dependentes da cadeia (T-001-2 → T-001-5) ficam desbloqueadas imediatamente após a conclusão desta.

## Assumptions

- O PRD não especifica o path ("NÃO ESPECIFICADO NO PRD"); a decisão foi derivada exclusivamente das convenções reais do código (`StockController` de `hb-catalog-service` e schemas de `catalog.yaml`), conforme instrução do usuário de usar `platform-shared-contracts` como contexto técnico absoluto.
- O saldo de estoque é um sub-recurso **singular** por produto (relação 1:1 produto × registro de saldo), portanto o path não é paginado nem pluralizado.
- A corrupção do cabeçalho de `catalog.yaml` era dano local não commitado — validado contra o histórico git: a versão commitada sempre foi íntegra (research R4). O working tree já foi restaurado; a pré-condição de FR-004 está satisfeita e resta apenas confirmação antes da edição.
- O versionamento do contrato permanece `0.1.0-SNAPSHOT` (ADR 0006 — publicação remota adiada; build local via `mvn -B install`); adicionar um path é mudança aditiva e não exige bump de versão neste estágio.
- Consumo do endpoint por `sc-order-service` (Fase 3) usará este mesmo path via proxy, sem endereço alternativo.

## Out of Scope

- Declaração da operação GET, parâmetros, schema de resposta e autorização no contrato (T-001-2, T-001-3, T-001-4, T-001-5).
- Implementação do endpoint no serviço (T-004-1 a T-004-4).
- Reconciliação dos campos do schema `StockItem` do contrato com a entidade do serviço (T-001-4).
