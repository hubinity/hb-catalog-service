# Feature Specification: Operação POST de registro de imagem de produto

**Feature Branch**: `007-product-images-post`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Declarar a operação POST no path de imagens de produto em platform-shared-contracts/contracts-catalog/openapi/catalog.yaml. Escopo estrito: identidade da operação + desfechos declarados, sem corpo de requisição e sem content de resposta."

**Task de origem**: `T-002-2` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-1]` (**concluída**, commit `fd9b905`). **Segunda task da cadeia T-002.**

## Contexto técnico verificado (código real)

- **Path Item já existe**: `/api/v1/products/{productId}/images` foi criado por T-002-1 (commit `fd9b905`) com `summary`, `description` e `parameters` de nível de Path Item contendo `productId` (`string`/`uuid`). A operação POST **herda** esse parâmetro.
- **`description` atual do Path Item — texto anterior, literal** (é o único elemento que esta task remove; fixado aqui para tornar o critério de diff do FR-014 verificável linha a linha):

  ```yaml
      description: |
        Address for managing a product's image references. Images are stored
        externally; this API records only their URLs (URL-only reference
        strategy) and never receives image bytes. Operations on this path are
        declared by the remaining T-002 tasks. Access requires a valid Bearer
        JWT (inherited from the document-level bearerAuth security scheme).
  ```

  A terceira frase (`Operations on this path are declared by the remaining T-002 tasks.`) é a que se torna **falsa** ao declarar-se o POST.
- **Esta é a PRIMEIRA operação de mutação do contrato inteiro.** Hoje o documento declara apenas dois GETs (`getProductById`, `getStockItemByProductId`) e nenhum `post`/`put`/`patch`/`delete`. O que for decidido aqui vira **precedente** para todas as mutações futuras do catálogo.
- **Tag `products` já existe** na seção `tags` do documento (ao lado de `stock`) — diferente de T-001-2, que precisou criar a tag `stock`. Nenhuma tag nova é necessária.
- **`security` global**: `security: [ { bearerAuth: [] } ]` está na raiz. A operação herda autenticação e **não** declara `security` próprio.
- **Padrão de autorização do serviço** (`ProductController.java`): **todas** as mutações — `@PostMapping`, `@PutMapping`, `@DeleteMapping` — usam `@PreAuthorize("hasRole('admin')")`; os GETs não têm anotação. A futura implementação (T-005-2) seguirá esse padrão.
- **Padrão de idempotência do serviço** (`IdempotencyFilter.java`): o filtro protege **exatamente 4 paths** — `/api/v1/products/*/stock/movements`, `/api/v1/stock/reservations`, `/api/v1/stock/reservations/*/release`, `/api/v1/stock/reservations/*/commit`. Nenhuma mutação de `ProductController` (POST/PUT/DELETE de produto) exige `Idempotency-Key`.
- **Precedente de escopo direto — T-001-2** (spec `002-stock-balance-get`): "declarar a operação" entregou verbo + `operationId` + `summary` + `tags` + **desfechos declarados sem `content`** (o corpo veio depois, em T-001-4). Declarou apenas `200` e `404`; **não** declarou `401` apesar de a autenticação ser exigida.
- **Destino da resposta** (`T-005-5`, tracker): "Retornar ProductResponse atualizado no handler de upload de imagens de produto".

## Decisões do usuário (registradas na confirmação do pipeline, 2026-07-25)

1. **Desfecho de sucesso = `201 Created`.** Convenção canônica de POST em coleção que cria um subrecurso.
   - **Compatível com T-005-5**: uma resposta `201` **pode** carregar representação, portanto devolver o `ProductResponse` atualizado não contradiz o código escolhido. Não há conflito a resolver.
   - **Sem header `Location`**: sob a estratégia URL-only não existe URI individual por imagem (as imagens vivem fora do sistema; o catálogo guarda apenas URLs). A ausência do `Location` é **decisão explícita**, não omissão.
2. **Requisito de role `admin` = declarar `403` + afirmar na `description` da operação.**
   - **Por que não é machine-readable**: `bearerAuth` é `type: http` / `scheme: bearer`, que **não possui mecanismo de scopes** em OpenAPI; e as roles do Keycloak chegam por `realm_access.roles`, não por scopes OAuth. Não existe construção do OpenAPI 3.1 que expresse "exige role admin" para este esquema. Logo, o contrato declara o **desfecho** (`403`) e afirma o requisito em **prosa**.
   - Esta decisão **encerra a pendência do Princípio VI** deferida por T-002-1.

## Decisão tomada por evidência (encerra a pendência do Princípio V)

3. **A operação NÃO exige `Idempotency-Key`.** Não é silêncio — é decisão registrada, sustentada por evidência: o `IdempotencyFilter` cobre somente os 4 paths de estoque, e **nenhuma** mutação de produto existente exige a chave. Exigi-la aqui seria (a) inconsistente com todas as demais mutações de produto e (b) dependente de alterar o array `PROTECTED` do filtro — mudança de serviço que nenhuma task agendou. O Princípio V da constituição impõe a chave aos POSTs mutantes **de estoque**; esta é uma mutação de **produto**, fora do alcance literal do princípio.

## Decisão de escopo desta task

Entregáveis: **(a)** declarar o bloco `post:` no Path Item existente; **(b)** identidade da operação (`operationId`, `summary`, `tags`); **(c)** desfechos `201`, `403` e `404` **description-only, sem `content`**; **(d)** atualizar a `description` do Path Item para refletir que a operação passou a existir.

**Sobre (d) — decisão explícita**: a `description` atual afirma *"Operations on this path are declared by the remaining T-002 tasks."* Depois desta task a frase fica **factualmente errada** (a operação passa a existir). Ela **será atualizada** para registrar que o POST está declarado e que corpo e schema seguem pendentes em T-002-3/T-002-4. Isso segue o padrão da cadeia T-001, onde T-001-5 atualizou a `description` do Path Item ao fechar a cadeia. **Consequência**: diferente de T-002-1, esta mudança **não é estritamente aditiva** — uma linha preexistente é reescrita, de forma deliberada e delimitada.

**Fora de escopo**: corpo da requisição (T-002-3), `content`/schema de resposta (T-002-4), estratégia de armazenamento documentada no contrato (T-002-5), implementação (cadeia T-005).

**Elementos alvo** — a mudança tem **duas** partes:

**(1) `description` do Path Item — texto substituto** (a frase sobre "remaining T-002 tasks" sai):

```yaml
  /api/v1/products/{productId}/images:
    summary: Image references for a product          # inalterado
    description: |
      Address for managing a product's image references. Images are stored
      externally; this API records only their URLs (URL-only reference
      strategy) and never receives image bytes. The POST operation is
      declared; its request body and response body are completed by
      T-002-3 and T-002-4. Access requires a valid Bearer JWT (inherited
      from the document-level bearerAuth security scheme); registering an
      image additionally requires the admin role.
    parameters:                                       # inalterado
```

**(2) bloco `post`** — inserido **após** `parameters`, como último elemento do Path Item:

```yaml
    post:
      tags: [products]
      operationId: addProductImage
      summary: Register an image reference for a product
      description: |
        Records the URL of an externally hosted image against the product.
        No image bytes are transmitted. Requires the admin realm role — a
        valid Bearer JWT alone is not sufficient; requests authenticated
        without that role are rejected with 403.
      responses:
        '201':
          description: |
            Image reference registered. No Location header is returned:
            under the URL-only strategy there is no per-image resource URI.
        '403':
          description: Authenticated principal lacks the required admin role
        '404':
          description: Product not found
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor descobre como registrar uma imagem e o que esperar de volta (Priority: P1)

Uma equipe consumidora (ex.: `hb-catalog-web`, construindo o formulário de produto do roadmap 1.11) abre o contrato e encontra a operação que registra uma referência de imagem: seu verbo, sua identidade e os desfechos possíveis — inclusive que o sucesso é `201` e que **não** virá um `Location`.

**Why this priority**: É o entregável central. Sem a operação declarada, o path criado por T-002-1 é um endereço sem verbo — inútil para qualquer consumidor.

**Independent Test**: O Path Item contém um bloco `post` com `operationId`, `summary`, `tags` e três desfechos declarados; build do módulo verde. Verificável sem T-002-3/T-002-4.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** um consumidor procura como registrar uma imagem, **Then** encontra a operação `post` em `/api/v1/products/{productId}/images` com `operationId: addProductImage`.
2. **Given** a operação, **When** o consumidor inspeciona os desfechos, **Then** encontra `201`, `403` e `404`, cada um com `description`.
3. **Given** o desfecho `201`, **When** lido, **Then** informa explicitamente que **não** há header `Location` e por quê.
4. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros.

---

### User Story 2 - Consumidor sabe, antes de integrar, que precisa de privilégio de administrador (Priority: P1)

Quem for integrar descobre — pela leitura do contrato, não por um `403` em produção — que esta operação exige role `admin`, e não apenas um JWT válido. Isso distingue a operação dos GETs do catálogo, que exigem só autenticação.

**Why this priority**: Também P1. É a informação que evita integração construída sobre premissa falsa de privilégio, e é a única forma de o contrato exprimir um requisito que ele não consegue tornar machine-readable.

**Independent Test**: A `description` da operação afirma o requisito de role `admin`, e existe o desfecho `403` correspondente — ambos inspecionáveis isoladamente.

**Acceptance Scenarios**:

1. **Given** a operação, **When** o consumidor lê sua `description`, **Then** encontra a afirmação de que a role `admin` é exigida e de que um JWT válido sozinho não basta.
2. **Given** os desfechos, **When** inspecionados, **Then** `403` está declarado e descreve a falta da role, não uma falha de autenticação.
3. **Given** a operação, **When** verificado seu bloco `security`, **Then** ele **não existe** — o requisito de autenticação continua herdado da raiz, sem divergir do documento.

---

### Edge Cases

- **Requisito de role não é machine-readable — e isso é limite do formato, não descuido**: `bearerAuth` (`http`/`bearer`) não tem scopes, e as roles do Keycloak vêm de `realm_access.roles`. Nenhuma construção do OpenAPI 3.1 expressa "exige admin" para este esquema. Prosa + `403` é o teto do que o contrato consegue afirmar; a aplicação é server-side (`@PreAuthorize`, T-005-2).
- **`201` sem `Location` é deliberado**: a convenção HTTP sugere `Location` num `201`, mas sob URL-only não há recurso individual endereçável por imagem. Declarar um `Location` inventado seria pior que omiti-lo — a `description` do `201` registra a ausência e a razão.
- **`201` convivendo com corpo de `ProductResponse`**: T-005-5 devolve o produto atualizado. Como `201` admite representação, não há contradição entre o código e o corpo; a amarração formal do corpo é de T-002-4.
- **`400` não é declarado aqui**: payload inválido só faz sentido depois que T-002-3 definir o formato da requisição. Declarar o desfecho antes do corpo inverteria a ordem da cadeia.
- **`401` não é declarado**: o precedente T-001-2 não declarou `401` para o GET mesmo com autenticação exigida. Manter a omissão preserva a consistência do documento; alterá-la seria mudança de convenção para todas as operações, fora do escopo desta task.
- **Idempotência ausente por decisão**: ver seção própria. A ausência de um parâmetro de header `Idempotency-Key` é intencional e alinhada às demais mutações de produto.
- **URL duplicada — desfecho não declarado, por decisão**: registrar duas vezes a mesma URL para o mesmo produto poderia justificar um `409 Conflict`. Nenhum desfecho de conflito é declarado nesta task porque a pergunta "o que conta como duplicata" só existe depois que **T-002-3** definir o formato do corpo (uma URL por requisição? uma lista?). Declarar `409` antes disso seria fixar semântica de corpo por antecipação. Deferido explicitamente, não esquecido.
- **Tamanho da coleção não é limitado aqui**: um teto de imagens por produto é restrição do **atributo** `images[]` (cadeia T-003, entidade e coluna), não da operação que o alimenta. Se um limite existir, o desfecho correspondente entra junto com ele.
- **Precedente para o contrato inteiro**: sendo a primeira mutação declarada, as escolhas aqui (prosa para role, `403` declarado, sem `401`, sem idempotência para produto) tendem a ser copiadas pelas mutações futuras. Registrado para que a repetição seja consciente e não acidental.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O Path Item `/api/v1/products/{productId}/images` MUST declarar um bloco `post`, posicionado **após** o bloco `parameters`, como último elemento do Path Item.
- **FR-002**: A operação MUST declarar identidade: `operationId: addProductImage`, uma `summary` descritiva e `tags: [products]` — reutilizando a tag existente, sem criar tag nova. O `operationId` MUST ser único no documento (exigência do OpenAPI); os existentes são `getProductById` e `getStockItemByProductId`.
- **FR-003**: A operação MUST NOT redeclarar o parâmetro `productId` — ele é herdado do nível do Path Item.
- **FR-004**: A operação MUST declarar o desfecho `'201'` com `description`, e **sem** `content` (o corpo é entregável de T-002-4).
- **FR-005**: A `description` do `201` MUST registrar explicitamente que **não** há header `Location` e a razão (inexistência de URI individual por imagem sob a estratégia URL-only). A operação MUST NOT declarar um bloco `headers` com `Location`.
- **FR-006**: A operação MUST declarar o desfecho `'403'` com `description` que identifique a causa como **falta da role `admin`**, distinguindo-a de falha de autenticação.
- **FR-007**: A `description` da operação MUST afirmar que a role `admin` é exigida e que um JWT válido, isoladamente, não é suficiente.
- **FR-008**: A operação MUST declarar o desfecho `'404'` com `description` para produto inexistente.
- **FR-009**: A operação MUST NOT declarar os desfechos `'400'` (pertence a T-002-3, após o formato da requisição existir) nem `'401'` (consistência com o precedente T-001-2).
- **FR-010**: A operação MUST NOT declarar `requestBody` — entregável de T-002-3.
- **FR-011**: A operação MUST NOT declarar `security` próprio — herda `bearerAuth` da raiz do documento.
- **FR-012**: A operação MUST NOT declarar parâmetro de header `Idempotency-Key`, alinhando-se às demais mutações de produto do serviço.
- **FR-013**: A `description` do Path Item MUST ser substituída pelo texto especificado em *Elementos alvo (1)*. O texto substituto MUST: (a) preservar a semântica URL-only já estabelecida por T-002-1; (b) afirmar que a operação POST está declarada e que corpo de requisição e corpo de resposta são completados por T-002-3 e T-002-4; (c) registrar que o registro de imagem exige adicionalmente a role `admin`. A frase "Operations on this path are declared by the remaining T-002 tasks" MUST desaparecer do documento, por ter se tornado factualmente falsa.
- **FR-014**: A mudança MUST ser delimitada a dois pontos: o bloco `post` novo e a `description` do Path Item. Todos os demais elementos do documento — os dois GETs existentes, `security` global, `components`, `tags`, `summary` e `parameters` do Path Item — permanecem **intactos**. Critério objetivo: no diff, todas as linhas removidas MUST estar contidas no bloco `description:` do Path Item de imagens, cujo conteúdo anterior e substituto estão ambos fixados nesta spec (*Contexto técnico* e *Elementos alvo (1)*); nenhuma linha removida pode pertencer a outro elemento.
- **FR-015**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-016**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde em `hb-catalog-service` com o artefato de contratos reinstalado.

### Key Entities

- **Operação `addProductImage` (nova)**: primeira mutação declarada no contrato; unidade central desta task.
- **Desfecho `201` (novo)**: sucesso, sem `content` e sem `Location`.
- **Desfecho `403` (novo)**: única expressão contratual do requisito de role `admin`.
- **Desfecho `404` (novo)**: produto inexistente.
- **`description` do Path Item (existente — reescrita)**: único elemento preexistente alterado.
- **Parâmetro `productId` (existente — herdado, intocado)**: fornecido pelo Path Item.
- **`security` global `bearerAuth` (existente — herdado, intocado)**: cobre a operação sem declaração própria.
- **`ProductController` / `IdempotencyFilter` (serviço — fontes de verdade, intocados)**: sustentam as decisões de role e de idempotência.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um consumidor identifica, só lendo o contrato, o verbo, a identidade e os três desfechos da operação de registro de imagem.
- **SC-002**: Um consumidor descobre o requisito de role `admin` **antes** de integrar, sem depender de receber um `403` em execução.
- **SC-003**: Nenhum consumidor implementa espera por header `Location`, porque a ausência está declarada no próprio desfecho `201`.
- **SC-004**: As duas pendências constitucionais herdadas de T-002-1 (Princípio VI — role; Princípio V — idempotência) ficam **encerradas** com decisão registrada e justificada, não pendentes.
- **SC-005**: O documento permanece válido — build do módulo verde.
- **SC-006**: Regressão zero — `mvn -B verify` verde no consumidor.
- **SC-007**: A cadeia T-002 avança: T-002-3 pode declarar o `requestBody` e T-002-4 o `content` do `201` sobre uma operação já existente.

## Assumptions

- As duas decisões estruturais (`201`; `403` + prosa) foram tomadas pelo usuário na confirmação do pipeline em 2026-07-25 e são premissas de entrada, não inferências desta spec.
- A decisão de **não** exigir `Idempotency-Key` foi tomada por evidência de código (`IdempotencyFilter` + `ProductController`) e registrada como consciente.
- **`operationId: addProductImage`** segue a convenção camelCase verbo-substantivo do documento (`getProductById`, `getStockItemByProductId`). Escolha desta spec.
- **Atualizar a `description` do Path Item** (em vez de mantê-la) é escolha desta spec, sustentada pelo precedente de T-001-5; assume-se que uma frase factualmente errada é pior que uma mudança não-aditiva delimitada.
- Assume-se que a operação **adiciona** uma referência à coleção `images[]`, e não substitui a coleção inteira — coerente com POST em coleção e com `images[]` como array (PRD linha 319). A semântica precisa do corpo é de T-002-3.
- Autoridade de validação e workflow herdados da cadeia: build do módulo; branch `feature/stock-balance-path`; commits na fase de polish.
- O `contracts-catalog` deve ser reinstalado localmente antes de `hb-catalog-service` compilar contra o contrato alterado.

## Out of Scope

- `requestBody` e formato JSON da referência de URL (**T-002-3**), incluindo o desfecho `400` que dele depende.
- `content`/schema do `201` (**T-002-4**).
- Documentação da estratégia de armazenamento no contrato (**T-002-5**).
- Atributo `images` no schema `Product` do contrato, na entidade, nos DTOs e no mapper (**cadeia T-003**).
- Implementação: `ProductService`, handler em `ProductController`, `@PreAuthorize`, persistência (**cadeia T-005**).
- Alteração do array `PROTECTED` do `IdempotencyFilter` — nenhuma task a agendou e a decisão 3 dispensa a mudança.
- Introdução de `401` nas operações do contrato (mudaria a convenção de todas, não só desta).
- Operações de leitura, atualização ou remoção de imagens (`GET`/`DELETE` em `/images`) — nenhuma task da cadeia T-002 as prevê.
- **Semântica de URL duplicada e eventual desfecho `409`** — depende do formato do corpo, definido em **T-002-3**.
- **Limite de quantidade de imagens por produto** e o desfecho que o acompanharia — restrição do atributo `images[]`, na **cadeia T-003**.
