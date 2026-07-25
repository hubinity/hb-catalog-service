# Feature Specification: Corpo de requisição JSON do registro de imagem de produto

**Feature Branch**: `008-product-image-request-body`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Definir formato de requisição JSON (referência de URL, não multipart) do endpoint de registro de imagens de produto, com base na decisão de armazenamento URL-only (spec 006, decisão do usuário de 2026-07-25)."

**Task de origem**: `T-002-3` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-2]` (**concluída**, commit `854c02f`). **Terceira task da cadeia T-002.**

## Contexto técnico verificado (código real)

- **Operação já existe**: `addProductImage` (`post`) foi declarada por T-002-2 no Path Item `/api/v1/products/{productId}/images`, com `tags: [products]`, `summary`, `description` e os desfechos `201`, `403`, `404` — todos **sem `content`**. O parâmetro `productId` é herdado do Path Item.
- **Este será o PRIMEIRO `requestBody` do contrato inteiro** (`grep -c requestBody` → `0`). Assim como T-002-2 foi a primeira mutação, esta é a primeira entrada de dados — e vira precedente.
- **Este será o PRIMEIRO schema novo desde o início da cadeia T-002** e, portanto, **a primeira task da cadeia que gera código Java**. O pom pai fixa `generateModels=true`: todo item de `components/schemas` vira um DTO. Hoje existem 4 schemas ↔ 4 DTOs (`Product`, `Category`, `StockItem`, `StockMovement`); após esta task serão 5 ↔ 5. **O argumento "nada é gerado", usado no Constitution Check de T-002-1 e T-002-2, deixa de valer aqui.**
- **Convenção de validação do documento**: toda string limitada tem teto — `sku` (1–64), `name` de produto (1–200), `name` de categoria (1–120); numéricos usam `minimum: 0`. Não há string sem limite.
- **Convenção de corpo**: todo corpo de resposta do documento usa `$ref: '#/components/schemas/…'`. Não há schema inline em lugar algum.
- **Convenção de nome de DTO no serviço** (`api/dto/`): `CategoryRequest`, `ProductRequest`, `StockMovementRequest`, `StockReservationRequest` — sufixo `Request` para entrada.
- **Texto já commitado que restringe o formato**: a `description` da operação diz *"Records **the URL** of an externally hosted image"* e a `summary`, *"Register **an image reference**"* — ambos no singular.

## Decisões do usuário (registradas na confirmação do pipeline, 2026-07-25)

1. **Corpo carrega apenas a URL** — `{ "url": "https://…" }`, sem metadados.
   **Consequência declarada na cadeia**: `Product.images[]` será uma **lista de strings**, e a coluna de T-003-2 será `text[]` (não `jsonb`). Imagem principal = **primeiro elemento**, por convenção posicional; a ordenação é a da própria lista.
   **Justificativa**: o PRD (linha 319) diz apenas `images[]`, sem estrutura. `altText` e flag de imagem principal seriam requisitos que ninguém pediu, e encareceriam entidade, migração e mapper na cadeia T-003.
2. **Validação da URL = `format: uri` + `maxLength`**, com a expectativa de `https` afirmada **em prosa**, sem `pattern`.
   **Justificativa**: segue a convenção do documento (toda string tem teto) sem introduzir regex, que é rígida em contrato e difícil de afrouxar depois — em particular, não bloqueia cenários futuros de CDN (PRD §12).

## Decisões tomadas por evidência

3. **Uma imagem por requisição** (não lote). O texto já commitado em `854c02f` diz "the URL of an externally hosted image" e "an image reference", ambos no singular; aceitar lote exigiria reescrever contrato já entregue. Singular também é o que dá sentido ao `201 Created`.
4. **Schema nomeado `ProductImageRequest` em `components/schemas`**, não inline. Todo corpo do documento usa `$ref`, e o serviço já nomeia entradas com sufixo `Request`. Consequência assumida: gera o DTO `ProductImageRequest.java`.
5. **Nenhum desfecho `409` é declarado; a semântica de URL duplicada é deferida à cadeia T-003.** A spec 007 deferiu o `409` a esta task *porque* ele dependia da forma do corpo. Com a forma resolvida (uma URL), fica claro que "isto é duplicata?" é pergunta sobre o **estado da coleção**, não sobre o **formato do payload** — pertence à task que define o atributo e a coluna (T-003), onde uma eventual restrição de unicidade teria de ser implementada. Registrado como decisão, não como silêncio.
6. **A `description` do Path Item NÃO é reescrita nesta task.** Ela diz que corpo de requisição e de resposta "are completed by T-002-3 and T-002-4" — afirmação de **proveniência**, que continua verdadeira depois desta entrega (T-002-4 segue pendente). Diferente do caso de T-002-2, nenhuma frase se torna falsa. A limpeza final da `description` cabe à última task da cadeia, espelhando o que T-001-5 fez. **Consequência: esta mudança volta a ser estritamente aditiva**, ao contrário de T-002-2.

## Decisão de escopo desta task

Entregáveis: **(a)** declarar o `requestBody` da operação `addProductImage` (obrigatório, `application/json`, referenciando o schema); **(b)** declarar o schema `ProductImageRequest` em `components/schemas`; **(c)** declarar o desfecho **`400`** — obrigação **herdada de T-002-2**, que o deferiu explicitamente a esta task por depender da existência do formato de requisição.

**Fora de escopo**: `content` do `201` e schema de resposta (**T-002-4**), documentação da estratégia de armazenamento (**T-002-5**), atributo `images` na entidade/DTO/mapper e coluna (**cadeia T-003**), implementação (**cadeia T-005**).

**Elementos alvo** — três adições, **nenhuma remoção**:

**(1)** `requestBody` na operação `addProductImage`, entre `description` e `responses`:

```yaml
      requestBody:
        required: true
        description: The image reference to register
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductImageRequest'
```

**(2)** desfecho `400`, em `responses`, antes de `'403'`:

```yaml
        '400':
          description: Malformed request body, or url absent, not a valid URI, or too long
```

**(3)** schema `ProductImageRequest` em `components/schemas`:

```yaml
    ProductImageRequest:
      type: object
      description: |
        A reference to an image hosted outside this system. The catalog
        stores only the URL; image bytes are never transmitted. HTTPS URLs
        are expected — an http:// URL will be blocked as mixed content by
        browsers rendering the catalog over HTTPS.
      required:
        - url
      properties:
        url:
          type: string
          format: uri
          minLength: 1
          maxLength: 2048
          description: Absolute URL of the externally hosted image
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe exatamente o que enviar para registrar uma imagem (Priority: P1)

Uma equipe consumidora (ex.: `hb-catalog-web`, construindo o formulário do roadmap 1.11) descobre, lendo o contrato, o formato exato do corpo: um objeto JSON com um único campo `url` obrigatório. Não precisa deduzir se envia arquivo, lista ou objeto com metadados.

**Why this priority**: É o entregável central. Sem corpo declarado, a operação entregue por T-002-2 não é chamável — o consumidor sabe o endereço e o verbo, mas não o que enviar.

**Independent Test**: A operação tem `requestBody` obrigatório apontando para `ProductImageRequest`, e o schema existe com `url` obrigatório; build do módulo verde. Verificável sem T-002-4.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor procura o formato do corpo, **Then** encontra `requestBody` obrigatório com `application/json` referenciando `ProductImageRequest`.
2. **Given** o schema, **When** inspecionado, **Then** tem exatamente uma propriedade, `url`, marcada como obrigatória.
3. **Given** a `description` do schema, **When** lida, **Then** confirma que apenas a URL é armazenada e que bytes nunca trafegam.
4. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros e passa a gerar 5 DTOs.

---

### User Story 2 - Consumidor conhece os limites da URL e o desfecho de payload inválido (Priority: P2)

Quem integra descobre os limites aceitos (URI válida, até 2048 caracteres) e que uma requisição malformada é rejeitada com `400`, em vez de descobrir por tentativa e erro.

**Why this priority**: Importante, mas subordinado à existência do formato: sem o corpo declarado (US1), não há o que validar.

**Independent Test**: `url` tem `format: uri`, `minLength: 1`, `maxLength: 2048`; o desfecho `400` existe com `description` que nomeia as causas.

**Acceptance Scenarios**:

1. **Given** a propriedade `url`, **When** inspecionada, **Then** declara `format: uri`, `minLength: 1` e `maxLength: 2048`.
2. **Given** os desfechos da operação, **When** inspecionados, **Then** `400` existe e sua `description` nomeia as causas (corpo malformado, `url` ausente, URI inválida, excesso de tamanho).
3. **Given** a `description` do schema, **When** lida, **Then** explica a expectativa de HTTPS e a razão concreta (mixed content), sem impor `pattern`.

---

### Edge Cases

- **HTTPS esperado mas não imposto**: a expectativa está em prosa, não em `pattern`. Consequência aceita: o contrato **não rejeita** `http://` — quem rejeita, se for o caso, é o serviço (cadeia T-005). Declarar regex tornaria o contrato rígido e bloquearia cenários futuros de CDN (PRD §12). A prosa nomeia a razão concreta (mixed content) para que a escolha não pareça arbitrária.
- **`format: uri` é documentacional, não validador**: em OpenAPI, `format` é anotação; a validação efetiva ocorre no serviço. O `maxLength` é o único limite realmente estrutural declarado — e é o que dá base concreta ao dimensionamento da coluna em T-003-2.
- **URL duplicada continua sem desfecho** — ver decisão 5. A pergunta migrou de "depende do formato do corpo" (posição de T-002-2) para "depende do modelo de armazenamento" (T-003). Não é esquecimento: é a realocação da pergunta para onde ela pode ser respondida.
- **`2048` é convenção prática, não norma**: nenhuma RFC limita URIs a 2048; o número vem do menor teto historicamente praticado por navegadores e CDNs. Escolhido por ser o limite abaixo do qual nenhum cliente relevante trunca, e por dar teto explícito à coluna de T-003-2.
- **Primeira geração de código da cadeia**: diferente de T-002-1 e T-002-2, esta task **produz um artefato Java** (`ProductImageRequest.java`). Não há comportamento novo — é um portador de dados que o serviço ainda nem referencia (T-005) — mas a prova de regressão do consumidor deixa de ser formalidade e passa a ser o gate que confirma que o DTO novo compila sem quebrar nada.
- **Propriedades desconhecidas são ignoradas, não rejeitadas** (FR-016): o schema não declara `additionalProperties: false`. Enviar `altText` hoje não produz `400` — o campo é descartado em silêncio. É o comportamento de "leitor tolerante", escolhido para que a eventual introdução de metadados em T-003 seja mudança aditiva e não quebre clientes. A contrapartida aceita é que um erro de digitação em nome de campo passa despercebido.
- **Precedente de entrada de dados**: sendo o primeiro `requestBody` do contrato, as escolhas (schema nomeado com sufixo `Request`, `required: true` no corpo, mídia única, limites explícitos, leitor tolerante) tendem a ser copiadas. Registrado para que a repetição seja consciente.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A operação `addProductImage` MUST declarar `requestBody` com `required: true`, `description` e `content` de mídia `application/json`. O `content` MUST declarar **exatamente esse** tipo de mídia — nenhum outro pode ser adicionado ao lado, em particular `multipart/form-data`, cuja exclusão é a premissa da estratégia URL-only.
- **FR-002**: O `content` do `requestBody` MUST referenciar o schema por `$ref: '#/components/schemas/ProductImageRequest'` — **sem** schema inline, conforme a convenção do documento.
- **FR-003**: O `requestBody` MUST ser posicionado entre a `description` da operação e o bloco `responses`.
- **FR-004**: O documento MUST declarar, em `components/schemas`, o schema `ProductImageRequest` com `type: object` e uma `description` que afirme que apenas a URL é armazenada e que bytes de imagem nunca trafegam.
- **FR-005**: `ProductImageRequest` MUST declarar `required: [url]` e MUST ter **exatamente uma** propriedade, `url` — nenhum metadado adicional.
- **FR-006**: A propriedade `url` MUST declarar `type: string`, `format: uri`, `minLength: 1`, `maxLength: 2048` e uma `description`.
- **FR-007**: A `description` do schema MUST declarar a expectativa de HTTPS e a razão concreta (bloqueio por mixed content). O schema MUST NOT declarar `pattern` — a expectativa é afirmada, não imposta.
- **FR-008**: A operação MUST declarar o desfecho `'400'`, cuja `description` MUST nomear as causas cobertas: corpo malformado, `url` ausente, URI inválida e excesso de tamanho. *(Obrigação herdada de T-002-2, que deferiu este desfecho a esta task.)*
- **FR-009**: O desfecho `'400'` MUST ser declarado **sem `content`**, mantendo a convenção description-only estabelecida pela cadeia; os corpos de resposta são de T-002-4.
- **FR-010**: A operação MUST NOT declarar o desfecho `'409'` nem qualquer semântica de URL duplicada — deferido à cadeia T-003 (ver decisão 5).
- **FR-011**: A operação MUST NOT aceitar lote: o corpo descreve **uma** referência de imagem, coerente com o texto singular já commitado em `854c02f`.
- **FR-012**: A mudança MUST ser **estritamente aditiva**: adiciona `requestBody`, o desfecho `400` e o schema `ProductImageRequest`, e **nada mais**. Nenhuma linha preexistente pode ser alterada, removida ou reordenada — em particular, a `description` do Path Item e os desfechos `201`/`403`/`404` permanecem intactos. Critério objetivo: no diff, **nenhuma** linha aparece como `-`.
- **FR-013**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-014**: A entrega MUST provar que o novo schema gera exatamente **um** DTO adicional. Para que a parte "nenhum DTO preexistente é alterado" seja verificável, o inventário dos modelos gerados MUST ser **capturado antes da edição** (nomes + checksum do conteúdo) e comparado depois. Critério objetivo: após a edição existem 5 modelos, `ProductImageRequest` é o único nome novo, e o checksum dos 4 preexistentes é idêntico ao capturado. *(Sem a captura prévia a afirmação seria inverificável, já que `target/` é reconstruído a cada build.)*
- **FR-016**: O schema `ProductImageRequest` MUST NOT declarar `additionalProperties: false`. Propriedades desconhecidas são **ignoradas**, não rejeitadas — escolha que mantém o contrato compatível para frente caso a cadeia T-003 venha a introduzir metadados. **Consequência declarada**: um cliente que envie hoje `{ "url": …, "altText": … }` recebe `201`, com `altText` silenciosamente descartado; isso **não** é causa de `400`.
- **FR-015**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde em `hb-catalog-service` com o artefato reinstalado, na mesma contagem de testes do baseline medido antes da edição.

### Key Entities

- **`requestBody` de `addProductImage` (novo)**: primeiro corpo de requisição do contrato; unidade central da task.
- **Schema `ProductImageRequest` (novo)**: primeiro schema adicionado desde o início da cadeia T-002 e **primeiro artefato desta cadeia que gera código**.
- **Propriedade `url` (nova)**: única propriedade; URI absoluta, 1–2048 caracteres.
- **Desfecho `400` (novo)**: obrigação herdada de T-002-2.
- **Operação `addProductImage` (existente — recebe adições, sem reescrita)**.
- **`description` do Path Item (existente — intocada)**: permanece verdadeira como declaração de proveniência.
- **Desfechos `201`/`403`/`404` (existentes — intocados)**.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um consumidor monta uma requisição válida lendo apenas o contrato, sem consultar o time de backend.
- **SC-002**: Nenhum consumidor implementa envio de arquivo ou multipart, porque o corpo declarado é JSON e a `description` do schema afirma que bytes nunca trafegam.
- **SC-003**: Os limites da URL ficam explícitos, dando base objetiva ao dimensionamento da coluna em T-003-2.
- **SC-004**: O desfecho de payload inválido deixa de ser lacuna — a obrigação herdada de T-002-2 fica **encerrada**.
- **SC-005**: O documento permanece válido — build do módulo verde.
- **SC-006**: A geração de código é previsível: exatamente um DTO novo, nenhum preexistente alterado.
- **SC-007**: Regressão zero — `mvn -B verify` verde no consumidor.
- **SC-008**: A cadeia T-002 avança: resta apenas T-002-4 (corpo de resposta) e T-002-5 (estratégia documentada).

## Assumptions

- As duas decisões estruturais (corpo só com URL; `format: uri` + `maxLength` sem `pattern`) foram tomadas pelo usuário em 2026-07-25 e são premissas de entrada.
- **`maxLength: 2048`** é escolha desta spec, por convenção prática de navegadores/CDNs e por dar teto explícito à coluna de T-003-2. Nenhuma RFC impõe esse número.
- **`minLength: 1`** é escolha desta spec, para impedir string vazia — coerente com `sku` e `name`, que também usam `minLength: 1`.
- **`ProductImageRequest`** como nome segue o sufixo `Request` já usado em `api/dto/`. Escolha desta spec.
- **Posição em `components/schemas`**: o schema é **anexado ao final**, após `StockMovement`. A ordem das chaves de um mapping YAML não tem significado semântico em OpenAPI, e a lista existente (`Product`, `Category`, `StockItem`, `StockMovement`) já não é alfabética — é ordem de inserção. Anexar minimiza o diff e preserva a convenção.
- Assume-se que `Product.images[]` será **lista de strings** e a coluna, `text[]` — consequência direta da decisão 1, a ser confirmada pela cadeia T-003.
- Autoridade de validação e workflow herdados da cadeia: build do módulo; branch `feature/stock-balance-path`; commits na fase de polish.
- O `contracts-catalog` deve ser reinstalado localmente antes de `hb-catalog-service` compilar contra o contrato alterado.

## Out of Scope

- `content` do `201` e schema do corpo de resposta (**T-002-4**).
- Documentação da estratégia de armazenamento no contrato (**T-002-5**).
- Atributo `images` no schema `Product`, na entidade JPA, nos DTOs do serviço e no mapper; coluna `text[]` e migração Flyway (**cadeia T-003**).
- Implementação: `ProductService`, handler, validação efetiva da URL, `@PreAuthorize` (**cadeia T-005**).
- **Semântica de URL duplicada e eventual desfecho `409`** — realocado à cadeia T-003 (decisão 5).
- Imposição de HTTPS por `pattern`, allow-list de hosts, verificação de alcançabilidade da URL ou de o recurso ser realmente uma imagem.
- Aceitação de lote (múltiplas URLs por requisição).
- Reescrita da `description` do Path Item — cabe à última task da cadeia (decisão 6).
