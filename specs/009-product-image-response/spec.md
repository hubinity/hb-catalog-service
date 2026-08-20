# Feature Specification: Schema de resposta do registro de imagem de produto

**Feature Branch**: `009-product-image-response`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Definir schema de resposta do endpoint de registro de imagens de produto em contracts-catalog/openapi/catalog.yaml."

**Task de origem**: `T-002-4` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-3]` (**concluída**, commit `40dd8e0`). **Quarta task da cadeia T-002.**

## Contexto técnico verificado (código real)

- **Operação e corpo já existem**: `addProductImage` foi declarada por T-002-2 (`854c02f`) e ganhou `requestBody` + schema `ProductImageRequest` + desfecho `400` em T-002-3 (`40dd8e0`).
- **O desfecho `201` existe, mas sem `content`** — declarado description-only por T-002-2, registrando que não há header `Location`. É exatamente o `content` que esta task preenche.
- **Contagem de geração hoje**: 5 schemas ↔ 5 DTOs (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`). Verificado empiricamente após T-002-3. Esta task leva a **6 ↔ 6**.
- **Convenção de resposta do documento**: todo corpo de resposta usa `$ref: '#/components/schemas/…'`; não há schema inline. Sufixo `Response` já usado no serviço (`ProductResponse`, `StockItemResponse`, `CategoryResponse`).
- **Convenção de validação**: toda string limitada tem teto — inclusive `ProductImageRequest.url`, com `maxLength: 2048` fixado em T-002-3.
- **Convenção posicional herdada de T-002-3**: `Product.images[]` é lista de strings e a **imagem principal é o primeiro elemento**. Uma resposta que devolva a coleção **ordenada** é o que torna essa convenção observável pelo consumidor.

## Lacuna de backlog identificada (registrada, não resolvida aqui)

O schema **`Product` do contrato não possui `images`** — suas propriedades são `id`, `sku`, `name`, `description`, `price`, `categoryId`, `active`, `createdAt`, `updatedAt`. Varredura das 33 tasks do tracker: **nenhuma adiciona `images` a esse schema**. A cadeia T-003 cobre apenas o lado do serviço — entidade (T-003-1), migração Flyway (T-003-2), DTOs `ProductRequest`/`ProductResponse` (T-003-3/-4) e mapper (T-003-5).

**Impacto**: após T-003-4 o serviço devolverá imagens em `ProductResponse`, mas o contrato seguirá descrevendo um `Product` sem `images` — as operações de leitura de produto nunca as exporiam contratualmente. É divergência entre contrato e serviço, e apareceria tarde, na cadeia T-005.

**Encaminhamento**: entrada nova proposta ao usuário para o tracker (`T-002-6`, ver *Out of Scope*). **`TASKS.json` permanece intocado por esta spec.** Esta lacuna **não** é resolvida aqui — resolvê-la exigiria alterar um schema referenciado por outras operações, muito além de "schema de resposta do endpoint".

## Decisões do usuário (registradas na confirmação do pipeline, 2026-07-25)

1. **Schema de resposta próprio: `ProductImageResponse`**, autocontido, devolvendo `productId` e a **coleção resultante completa, em ordem** — não apenas a URL recém-registrada.
   **Justificativa**: mantém a task no escopo declarado ("schema de resposta do endpoint"), **não** exige tocar no schema `Product` (que nenhuma task possui) e confirma ao consumidor o efeito real da chamada. Devolver a coleção ordenada também torna observável a convenção "principal = primeiro elemento", fixada em T-002-3.
2. **A lacuna do schema `Product` é registrada com proposta de entrada nova no tracker**, sem que o pipeline edite `TASKS.json`.

## Decisões tomadas por evidência

3. **Não reescrever a `description` do Path Item.** Ela diz que corpo de requisição e de resposta "are completed by T-002-3 and T-002-4" — declaração de **proveniência**, verdadeira antes e depois desta entrega. Mantém-se o critério de T-002-3: a limpeza final da `description` cabe à **última** task da cadeia (T-002-5), espelhando o que T-001-5 fez. **Consequência: a mudança é estritamente aditiva** — zero remoções, como em T-002-1 e T-002-3.
4. **Não alterar a `description` do `201`.** Ela registra a ausência de `Location`, que continua verdadeira. O `content` é **acrescentado** ao bloco existente, sem tocar no que já está lá.

## Decisão de escopo desta task

Entregáveis: **(a)** acrescentar `content` (`application/json`, via `$ref`) ao desfecho `201` já existente; **(b)** declarar o schema `ProductImageResponse` em `components/schemas`.

**Fora de escopo**: estratégia de armazenamento documentada no contrato (**T-002-5**), propriedade `images` no schema `Product` (**lacuna — proposta `T-002-6`**), atributo/coluna/DTOs do serviço (**cadeia T-003**), implementação (**cadeia T-005**).

**Elementos alvo** — duas adições, **nenhuma remoção**:

**(1)** `content` acrescentado ao `201` existente (a `description` atual permanece intacta acima dele):

```yaml
        '201':
          description: |
            Image reference registered. No Location header is returned:
            under the URL-only strategy there is no per-image resource URI.
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ProductImageResponse'
```

**(2)** schema `ProductImageResponse`, anexado ao fim de `components/schemas`:

```yaml
    ProductImageResponse:
      type: object
      description: |
        The product's image references after the registration. Returns the
        full resulting collection, not just the newly added entry, so the
        caller can confirm the outcome and observe ordering — the first
        element is the product's primary image.
      required:
        - productId
        - images
      properties:
        productId:
          type: string
          format: uuid
          description: Product the images belong to
        images:
          type: array
          description: Full resulting collection of image URLs, in order
          items:
            type: string
            format: uri
            maxLength: 2048
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor confirma o resultado do registro sem uma segunda chamada (Priority: P1)

Quem registra uma imagem recebe de volta a coleção resultante do produto, e com isso confirma que a URL entrou — sem precisar emitir um GET adicional para verificar.

**Why this priority**: É o entregável central. Sem `content`, o `201` é opaco: o consumidor sabe que deu certo, mas não o quê exatamente ficou registrado.

**Independent Test**: O `201` tem `content` apontando para `ProductImageResponse`, e o schema existe com `productId` e `images` obrigatórios; build do módulo verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona o `201`, **Then** encontra `content` `application/json` referenciando `ProductImageResponse`.
2. **Given** o schema, **When** inspecionado, **Then** tem `productId` (UUID) e `images` (array de URIs), ambos obrigatórios.
3. **Given** a `description` do schema, **When** lida, **Then** deixa claro que a resposta traz a **coleção inteira**, não apenas a entrada nova.
4. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros e passa a gerar 6 DTOs.

---

### User Story 2 - Consumidor descobre qual é a imagem principal (Priority: P2)

Quem monta a vitrine (totem, formulário do roadmap 1.11) precisa saber qual imagem é a principal. A resposta devolve a coleção **em ordem**, e a `description` declara que o primeiro elemento é a principal — tornando observável a convenção posicional fixada em T-002-3.

**Why this priority**: Importante, mas subordinado: sem o corpo da resposta (US1) não há coleção onde observar ordem.

**Independent Test**: A `description` de `ProductImageResponse` e a de `images` afirmam a ordenação e o papel do primeiro elemento.

**Acceptance Scenarios**:

1. **Given** a `description` do schema, **When** lida, **Then** afirma que o primeiro elemento é a imagem principal.
2. **Given** a propriedade `images`, **When** inspecionada, **Then** sua `description` declara que a coleção vem **em ordem**.
3. **Given** os itens de `images`, **When** inspecionados, **Then** declaram `format: uri` e `maxLength: 2048`, simétricos ao `ProductImageRequest.url`.

---

### Edge Cases

- **Resposta devolve a coleção inteira, não o item criado**: incomum para um `201`, que costuma representar o recurso criado. Aqui é deliberado — sob URL-only não há recurso individual endereçável (razão pela qual T-002-2 já dispensou o `Location`), então a unidade observável é a coleção. A `description` declara isso para que não pareça engano.
- **Coleção com um único elemento**: o primeiro registro devolve `images` com um item, que é simultaneamente o único e o principal. Nenhum tratamento especial é necessário.
- **Nenhum `minItems` é declarado**: seria verdade que a coleção tem ao menos 1 elemento após um `201`, mas essa é afirmação sobre o **comportamento** do serviço, não sobre a forma do documento. Declará-la aqui anteciparia garantia que só a cadeia T-005 pode cumprir.
- **`maxLength` nos itens é documentacional**: numa resposta, restrição não valida nada — descreve o que o servidor produz. É verdadeira por construção, já que a entrada limita `url` a 2048 (T-002-3). Incluída por simetria e pela convenção do documento de não deixar string sem teto.
- **Divergência contrato × serviço permanece aberta fora desta task**: `Product` seguirá sem `images` no contrato até que a lacuna seja endereçada. Esta task **não** a agrava — pelo contrário, evita depender dela ao usar schema próprio.
- **`productId` na resposta é redundante com o path — e mesmo assim declarado**: o chamador já conhece o `productId`, pois o informou na URL. Devolvê-lo torna a resposta **autocontida**, legível fora do contexto da requisição que a originou (log, cache, fila). Há precedente direto no documento: o schema `StockItem` declara `productId` embora seja servido por `/api/v1/products/{productId}/stock`. Não é ruído — é a convenção vigente.
- **Tamanho da coleção não é limitado nem paginado**: a resposta devolve todas as imagens do produto. Aceitável porque a cardinalidade esperada é de poucas unidades por produto e um teto pertence ao **atributo**, não a esta resposta — mesma alocação feita em T-002-3, que remeteu o limite à cadeia T-003. Se um teto for criado lá, esta resposta o herda sem mudança contratual.
- **Segundo artefato gerado da cadeia**: como T-002-3, esta task produz um DTO (`ProductImageResponse.java`, 5 → 6). Sem comportamento novo; o serviço ainda não o referencia (cadeia T-005).
- **Corpos de erro não são modelados — e isso é uma segunda divergência contrato × serviço**: o Princípio I da constituição exige que **toda** resposta de erro seja RFC 7807 `ProblemDetail`, produzida pelo `ApiExceptionHandler`. O serviço fará isso; o contrato, porém, declara `400`/`403`/`404` apenas com `description`, sem schema — e **nenhuma task do tracker modela `ProblemDetail`**. Esta task **não** corrige isso (FR-011 preserva a convenção da cadeia), mas registra a lacuna com encaminhamento próprio (ver *Out of Scope*), pelo mesmo critério aplicado à lacuna do schema `Product`.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O desfecho `'201'` MUST receber um bloco `content` com o tipo de mídia `application/json` — **exclusivamente** esse tipo.
- **FR-002**: O `content` MUST referenciar o schema por `$ref: '#/components/schemas/ProductImageResponse'`, sem schema inline.
- **FR-003**: A `description` existente do `201` MUST permanecer **inalterada** — o `content` é acrescentado abaixo dela.
- **FR-004**: O documento MUST declarar, em `components/schemas`, o schema `ProductImageResponse` com `type: object` e `description` que afirme (a) que a resposta traz a **coleção resultante completa**, não apenas a entrada nova, e (b) que o **primeiro elemento é a imagem principal**.
- **FR-005**: `ProductImageResponse` MUST declarar `required: [productId, images]` e exatamente duas propriedades.
- **FR-006**: `productId` MUST declarar `type: string`, `format: uuid` e `description`.
- **FR-007**: `images` MUST declarar `type: array` e uma `description` que afirme que a coleção vem **em ordem**.
- **FR-008**: Os itens de `images` MUST declarar `type: string`, `format: uri` e `maxLength: 2048`, simétricos ao `ProductImageRequest.url`.
- **FR-009**: O schema MUST NOT declarar `minItems` em `images` — é afirmação de comportamento, própria da cadeia T-005.
- **FR-010**: O schema MUST NOT declarar `additionalProperties: false`, mantendo a postura de leitor tolerante adotada em T-002-3.
- **FR-011**: Os desfechos `400`, `403` e `404` MUST permanecer **sem `content`** — esta task preenche apenas o `201`. Corpos de erro seguem a convenção description-only da cadeia.
- **FR-012**: A `description` do Path Item MUST permanecer **intocada** — segue verdadeira como proveniência; a limpeza final é de T-002-5.
- **FR-013**: A mudança MUST ser **estritamente aditiva**: acrescenta o `content` do `201` e o schema novo, e nada mais. Critério objetivo: no diff, **nenhuma** linha aparece como `-`.
- **FR-014**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-015**: A entrega MUST provar que exatamente **um** DTO adicional é gerado (5 → 6), com `ProductImageResponse` como único nome novo e checksum idêntico nos 5 preexistentes. A captura do inventário MUST ser feita **antes** da edição **e nesta execução** — reaproveitar o arquivo produzido pela execução de T-002-3 é **proibido**: `target/` é regenerado a cada build, então um inventário de outra execução não descreve o estado de partida desta. O arquivo de baseline MUST ter nome próprio da feature (`/tmp/dto-baseline-009.txt`), de modo que a reutilização acidental seja estruturalmente impossível.
- **FR-016**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde, na mesma contagem de um baseline **medido antes** da edição **e nesta execução**. Herdar a contagem registrada em specs anteriores é **proibido** — a suíte pode ter mudado desde então, e uma comparação contra número herdado não prova ausência de regressão.

### Key Entities

- **Schema `ProductImageResponse` (novo)**: corpo do `201`; segundo artefato da cadeia a gerar código.
- **Propriedade `productId` (nova)**: identifica o produto dono da coleção.
- **Propriedade `images` (nova)**: coleção ordenada de URLs; primeiro elemento = imagem principal.
- **Desfecho `201` (existente — recebe `content`, `description` intocada)**.
- **Desfechos `400`/`403`/`404` (existentes — permanecem sem `content`)**.
- **Schema `Product` (existente — intocado; alvo da lacuna proposta como `T-002-6`)**.
- **`description` do Path Item (existente — intocada)**.

## Success Criteria *(mandatory)*

- **SC-001**: Um consumidor confirma o resultado do registro pela própria resposta, sem emitir uma segunda chamada.
- **SC-002**: A imagem principal é identificável pela resposta, tornando observável a convenção posicional de T-002-3.
- **SC-003**: A task é entregue **sem** depender do schema `Product`, cuja lacuna permanece registrada e endereçada por proposta própria.
- **SC-004**: O documento permanece válido — build do módulo verde.
- **SC-005**: A geração é previsível: exatamente um DTO novo, nenhum preexistente alterado.
- **SC-006**: Regressão zero no consumidor.
- **SC-007**: A cadeia T-002 avança: resta apenas **T-002-5** (estratégia de armazenamento documentada), que também encerra a `description` do Path Item.

## Assumptions

- As duas decisões estruturais (schema próprio devolvendo a coleção; registrar a lacuna com proposta de entrada) foram tomadas pelo usuário em 2026-07-25.
- **`maxLength: 2048` nos itens** é escolha desta spec, por simetria com `ProductImageRequest.url` e pela convenção do documento. Não constava do esboço aprovado; é adição conservadora e removível sem afetar os demais requisitos.
- **`ProductImageResponse`** como nome segue o sufixo `Response` do serviço. Escolha desta spec.
- **Posição em `components/schemas`**: anexado ao final, após `ProductImageRequest` — ordem de chaves não tem significado semântico e a lista é de inserção.
- Assume-se que a resposta reflete a coleção **após** a operação, incluindo a entrada recém-criada.
- Autoridade de validação e workflow herdados da cadeia; `contracts-catalog` reinstalado antes de o consumidor compilar.

## Out of Scope

- Estratégia de armazenamento documentada no contrato e limpeza final da `description` do Path Item (**T-002-5**).
- **Propriedade `images` no schema `Product`** — lacuna de backlog. Entrada proposta ao usuário, para inserção após `T-002-5`:

  ```json
  {
    "id": "T-002-6",
    "phase": "contracts",
    "description": "Adicionar a propriedade images ao schema Product em contracts-catalog/openapi/catalog.yaml, para que as operações de leitura de produto exponham as URLs de imagem no contrato. Lacuna identificada na spec 009-product-image-response: nenhuma task existente cobre o schema Product do contrato — a cadeia T-003 cobre apenas entidade, migração Flyway, DTOs do serviço e mapper",
    "source_reference": "shared-contracts",
    "status": "refined",
    "decomposition_allowed": false,
    "depends_on": ["T-002-4"]
  }
  ```

  **`TASKS.json` não é editado por esta spec.**
- **Corpos de resposta dos desfechos de erro (`400`/`403`/`404`)** — segunda lacuna de backlog. A cadeia manteve todos description-only, e nenhuma task modela RFC 7807 `ProblemDetail`, embora o Princípio I da constituição o exija de toda resposta de erro do serviço. Entrada proposta ao usuário, para inserção após `T-002-6`:

  ```json
  {
    "id": "T-002-7",
    "phase": "contracts",
    "description": "Declarar o schema ProblemDetail (RFC 7807) em contracts-catalog/openapi/catalog.yaml e referenciá-lo no content dos desfechos de erro das operações do catálogo. Lacuna identificada na spec 009-product-image-response: o Princípio I da constituição exige RFC 7807 em toda resposta de erro do serviço, mas o contrato declara os desfechos de erro apenas com description, sem schema",
    "source_reference": "shared-contracts",
    "status": "refined",
    "decomposition_allowed": true,
    "depends_on": ["T-002-4"]
  }
  ```

  Diferente da `T-002-6`, esta tem `decomposition_allowed: true`: alcança **todas** as operações do documento, não só a de imagens, e pode valer a pena quebrá-la por operação.
- Atributo, coluna, DTOs do serviço e mapper (**cadeia T-003**); implementação (**cadeia T-005**).
- Semântica de URL duplicada — realocada a T-003 em T-002-3.
