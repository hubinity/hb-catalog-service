# Feature Specification: Operação GET no path canônico de leitura de saldo de estoque

**Feature Branch**: `002-stock-balance-get`

**Created**: 2026-07-22

**Status**: Draft

**Input**: User description: "detalhe a task T-001-2 descrita no arquivo TASKS.json deste repositório: 'Declarar operação GET no path de leitura de saldo de estoque em contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts'. Use o código real de platform-shared-contracts como contexto técnico absoluto."

**Task de origem**: `T-001-2` (TASKS.json, fase `contracts`) — depende de `T-001-1` (**concluída**: path `/api/v1/products/{productId}/stock` já declarado no `catalog.yaml`, branch `feature/stock-balance-path`).

## Contexto técnico verificado (código real)

Estado atual de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (pós T-001-1):

- Seção `paths` tem 2 entradas: `/api/v1/products/{id}` (operação completa `getProductById`) e `/api/v1/products/{productId}/stock` (**Path Item sem operação** — apenas `summary`/`description` afirmando que a operação GET será declarada por T-001-2..5).
- Convenção de operação existente (`getProductById`): `tags: [products]`, `operationId` em camelCase `get<Recurso>By<Chave>`, `summary` imperativo, respostas `'200'` (com content) e `'404'` (description-only).
- Seção `tags` declara somente `products` — não existe tag para estoque.
- Schema `StockItem` já existe em `components/schemas` (será o corpo da resposta em T-001-4).
- Pipeline de validação: `openapi-generator-maven-plugin` (DTO-only, ADR 0002) parseia o documento durante `mvn -B install` — autoridade de validação estabelecida na feature 001 (research R3).

## Decisão de escopo desta task

Esta especificação cobre **somente T-001-2**: declarar a **operação GET** (identidade e desfechos) no path canônico. Ficam fora: parâmetro `productId` (T-001-3), schema de resposta `StockItemResponse` no content (T-001-4) e requisito de autorização (T-001-5).

**Operação definida** (derivada da convenção real `getProductById`):

- Verbo: `get` no path `/api/v1/products/{productId}/stock`
- Identidade: `operationId: getStockItemByProductId`, `summary: Fetch the current stock balance for a product`, `tags: [stock]` (nova tag `stock` declarada na seção `tags`)
- Desfechos declarados: `'200'` — saldo encontrado (sem `content` nesta task; o corpo vem em T-001-4) e `'404'` — **desfecho único** cobrindo produto inexistente ou produto sem registro de saldo (description-only, padrão do contrato; decisão do usuário em revisão de checklist)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor descobre como ler o saldo (verbo e desfechos) (Priority: P1)

Uma equipe consumidora que já conhece o endereço canônico (entregue por T-001-1) abre o contrato e agora vê **como** interagir com ele: uma operação de leitura (GET) com identidade única e os dois desfechos possíveis — saldo encontrado ou produto sem registro de saldo — sem precisar perguntar ao time de backend qual verbo usar ou o que esperar quando não há estoque cadastrado.

**Why this priority**: É o entregável central da task; sem a operação, o path é um endereço mudo que não diz como consumi-lo.

**Independent Test**: Abrir `contracts-catalog/openapi/catalog.yaml` e verificar que o Path Item do saldo contém uma operação `get` com `operationId` único e respostas `'200'` e `'404'` declaradas; o build do módulo de contratos permanece verde.

**Acceptance Scenarios**:

1. **Given** o contrato com o path canônico de saldo, **When** um consumidor inspeciona o Path Item, **Then** encontra exatamente uma operação (`get`) com `operationId: getStockItemByProductId` e `tags: [stock]`.
2. **Given** a operação declarada, **When** o consumidor lê os desfechos, **Then** vê `'200'` (saldo encontrado) e `'404'` (produto sem registro de saldo), cada um com descrição textual.
3. **Given** a edição concluída, **When** o documento é validado pela autoridade de validação (build do módulo), **Then** o build passa sem erros.

---

### User Story 2 - Contrato permanece coerente com as convenções existentes (Priority: P2)

Quem mantém o contrato (e as ferramentas que o processam) encontra a nova operação seguindo exatamente o padrão da operação preexistente `getProductById` — nomenclatura de `operationId`, uso de tag declarada na seção `tags`, estrutura de respostas — de modo que o documento continue parecendo escrito por uma só mão.

**Why this priority**: Coerência interna evita que cada task da cadeia T-001-x invente seu próprio estilo, o que degradaria o contrato como fonte de verdade.

**Independent Test**: Comparar lado a lado a operação nova e `getProductById`: mesmo formato de `operationId` (`get<Recurso>By<Chave>`), tag declarada (não órfã), respostas com aspas simples nos códigos e descrições textuais.

**Acceptance Scenarios**:

1. **Given** a seção `tags` do contrato, **When** a operação usa `tags: [stock]`, **Then** a tag `stock` está declarada na seção `tags` com `name` e `description` (nenhuma tag órfã).
2. **Given** os paths preexistentes e os schemas do contrato, **When** a operação é adicionada, **Then** nada além do Path Item do saldo (e da seção `tags`) é alterado.

---

### Edge Cases

- **Parâmetro de template não declarado**: o path contém `{productId}`, mas a declaração do parâmetro é escopo de T-001-3. Validadores OpenAPI costumam exigir que operações declarem seus parâmetros de path; se a autoridade de validação rejeitar a operação sem o parâmetro, aplica-se a contingência de entrega conjunta com T-001-3 (ver FR-006).
- **404 vs. saldo zero**: saldo `0` é `'200'` com quantidade zero (corpo modelado em T-001-4); `'404'` é reservado à ausência do recurso — **desfecho único** cobrindo produto inexistente ou produto sem registro de saldo (decisão do usuário: sem distinção no nível do contrato; distinção fina, se necessária, no corpo de erro em T-001-4). Esta é a decisão semântica registrada por esta task; a implementação (T-004-x) deve segui-la.
- **Descrição do Path Item desatualizada**: o texto herdado de T-001-1 afirma que "the GET operation … are declared by follow-up contract tasks"; após esta entrega isso fica parcialmente falso e deve ser ajustado (FR-007).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O Path Item `/api/v1/products/{productId}/stock` MUST declarar exatamente uma operação: `get`.
- **FR-002**: A operação MUST ter `operationId: getStockItemByProductId` — único no documento e no formato `get<Recurso>By<Chave>` da convenção existente (`getProductById`) — e `summary: Fetch the current stock balance for a product` (estilo imperativo da convenção).
- **FR-003**: A operação MUST usar `tags: [stock]`, e a tag `stock` MUST ser declarada na seção `tags` do documento como `name: stock` / `description: Product stock balance and movements` (sem tag órfã).
- **FR-004**: A operação MUST declarar as respostas: `'200'` com `description: Current stock balance for the product` (**sem** a chave `content` nesta task — o corpo é escopo de T-001-4) e `'404'` com `description: Stock balance not found for the given product (unknown product or no stock record)` — **um único desfecho 404 cobrindo os dois casos** (produto inexistente ou sem registro de saldo); a distinção fina, se necessária, será modelada no corpo de erro em T-001-4.
- **FR-005**: A mudança MUST ser aditiva e restrita ao Path Item do saldo e à seção `tags`; todos os demais paths, schemas e metadados permanecem intactos, e o documento MUST permanecer um OpenAPI 3.1 válido segundo a autoridade de validação (build do módulo de contratos).
- **FR-006**: Se a autoridade de validação rejeitar a operação por parâmetro de path não declarado (`{productId}` — escopo de T-001-3), a entrega MUST ser coordenada com T-001-3 no mesmo commit, mantendo T-001-2 como a decisão de identidade/desfechos da operação.
- **FR-007**: A `description` do Path Item (herdada de T-001-1) MUST ser ajustada para o novo estado: declarar que a operação GET existe e que **o parâmetro `productId`, o corpo da resposta e a autorização** são declarados por T-001-3..T-001-5 (substituindo o texto atual, que afirma que a própria operação GET ainda está pendente).

### Key Entities

- **Operação GET (nova)**: interação de leitura no path canônico; identidade = `operationId` + tag; desfechos = `'200'`/`'404'`. Não carrega parâmetros nem corpo de resposta nesta task.
- **Tag `stock` (nova)**: agrupador de operações de estoque na seção `tags`; passa a coexistir com `products`.
- **Path Item do saldo (existente, editado)**: recebe a operação e o ajuste de descrição (FR-007); seu `summary` permanece.
- **Schema `StockItem` (existente, intocado)**: futuro corpo da resposta `'200'` — referenciá-lo é escopo de T-001-4.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O contrato contém exatamente 1 operação no Path Item do saldo (`get`), com `operationId` único no documento — verificável por inspeção direta.
- **SC-002**: O documento permanece válido e processável de ponta a ponta: a autoridade de validação (build do módulo de contratos) conclui sem erros após a edição.
- **SC-003**: Coerência de convenção evidenciada: a operação nova espelha `getProductById` em formato de `operationId`, tag declarada e estrutura de respostas — verificável por comparação lado a lado.
- **SC-004**: A task T-001-3 (parâmetro `productId`) fica desbloqueada imediatamente após a conclusão desta.

## Assumptions

- A semântica de `'404'` = recurso ausente (produto inexistente **ou** sem registro de saldo, sem distinção — decisão do usuário na revisão do checklist) segue o padrão da operação existente e resolve o caso deferido pela feature 001 ("404 vs. saldo zero"); saldo zero é `'200'` com quantidade 0 (corpo em T-001-4).
- `getStockItemByProductId` é o nome derivado da convenção real (`getProductById` + schema `StockItem` + chave `productId`); nenhum outro `operationId` existe no documento, então a unicidade é trivialmente satisfeita.
- A resposta `'200'` sem `content` é intencional e temporária (T-001-4 adiciona o corpo); em OpenAPI 3.1 uma resposta com apenas `description` é válida.
- A autoridade de validação e o workflow (branch `feature/stock-balance-path` em ambos os repos, commits na fase final) permanecem os estabelecidos pela feature 001 (research R3, tasks T003/T009/T012).
- Autorização (quem pode chamar o GET) é decisão de T-001-5; a ausência de `security` na operação nesta task não implica endpoint público.

## Out of Scope

- Declaração do parâmetro `productId` (T-001-3) — salvo acionamento da contingência FR-006.
- Schema de resposta `StockItemResponse`/`StockItem` no `content` de `'200'` (T-001-4).
- Requisito de autorização/`security` da operação (T-001-5).
- Implementação do endpoint no serviço (T-004-1 a T-004-4).
