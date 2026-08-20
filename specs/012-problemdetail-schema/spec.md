# Feature Specification: Schema ProblemDetail (RFC 7807) no contrato do catálogo

**Feature Branch**: `012-problemdetail-schema`

**Created**: 2026-07-26

**Status**: Draft

**Input**: User description: "T-002-7-1: Declarar o schema ProblemDetail em components/schemas de contracts-catalog/openapi/catalog.yaml com base no platform-shared-contracts."

**Task de origem**: `T-002-7-1` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-4]` (**concluída**). **Primeira das seis subtarefas** em que `T-002-7` (`decomposition_allowed: true`) foi decomposta. `T-002-7` por sua vez nasceu de uma lacuna registrada em `specs/009-product-image-response/spec.md` (*Out of Scope*, segunda lacuna): nenhuma task do tracker modelava RFC 7807 `ProblemDetail`, embora o Princípio I da constituição o exija de toda resposta de erro do serviço.

## Contexto técnico verificado (código real)

- **Schemas hoje em `components/schemas`**: `Product`, `Category`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse` — **6 schemas**, confirmados por leitura direta de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`. Nenhum modela erro.
- **Desfechos de erro sem `content` hoje** (5, em 3 operações): `getProductById` → `404`; `getStockItemByProductId` → `404`; `addProductImage` → `400`, `403`, `404`. Todos description-only. **Nenhum é tocado por esta task** — são o escopo de `T-002-7-2` até `T-002-7-6`, já registradas em `TASKS.json` com `depends_on` encadeado a partir desta.
- **Estrutura real emitida em runtime**: `com.hubinity.catalog.api.error.ApiExceptionHandler` (verificado em código) usa exclusivamente `org.springframework.http.ProblemDetail` — nunca um DTO próprio. Todo handler chama `ProblemDetail.forStatusAndDetail(status, detail)` e, na maioria dos casos, `problem.setTitle(...)` e `problem.setType(URI.create("urn:hubinity:catalog:..."))`. O handler de `MethodArgumentNotValidException` adicionalmente chama `problem.setProperty("errors", Map<String,String>)` — uma propriedade de extensão RFC 7807, não um dos cinco membros canônicos.
- **Serialização Spring do `ProblemDetail`**: os cinco membros canônicos são `type` (URI, "about:blank" se não setado), `title` (string), `status` (inteiro), `detail` (string) e `instance` (URI, opcional). Nenhum é obrigatório pela RFC 7807 — a especificação define todos como OPTIONAL.
- **Geração de DTOs é por schema declarado, não por schema referenciado**: `contracts-catalog/pom.xml` configura `openapi-generator-maven-plugin` com `generatorName: spring` e nenhuma restrição de `modelsToGenerate`; o comportamento padrão do gerador é emitir uma classe por entrada em `components/schemas`, independentemente de a entrada ser referenciada por alguma operação. Logo, declarar `ProblemDetail` aqui já produz `ProblemDetail.java` mesmo **antes** de qualquer desfecho referenciá-lo — verificável no mesmo build usado pelas specs anteriores da cadeia (6 → 7 DTOs).
- **Convenção de validação do documento**: nenhum outro schema usa `additionalProperties: false` (postura de leitor tolerante, fixada em `T-002-3`/FR-010 de `009-product-image-response`). Um `ProblemDetail` fechado quebraria essa convenção e impediria propriedades de extensão como `errors`, que o handler já emite hoje.

## Decisão de escopo desta task

**Entregável único**: declarar o schema `ProblemDetail` em `components/schemas`, com os cinco membros canônicos da RFC 7807, todos opcionais, e sem fechar o objeto a propriedades adicionais.

**Fora de escopo**: referenciar o schema em qualquer `content` de desfecho de erro — isso é `T-002-7-2` (`getProductById` 404), `T-002-7-3` (`getStockItemByProductId` 404), `T-002-7-4` (`addProductImage` 400), `T-002-7-5` (`addProductImage` 403) e `T-002-7-6` (`addProductImage` 404), todas já presentes em `TASKS.json` e dependentes desta em cadeia. Modelar a propriedade de extensão `errors` (específica de erros de validação) também fica fora de escopo — pertenceria a um schema mais específico, não ao `ProblemDetail` genérico.

**Elemento alvo** — uma única adição, nenhuma remoção, anexada ao fim de `components/schemas` (após `ProductImageResponse`, preservando a ordem de inserção sem significado semântico já praticada no documento):

```yaml
    ProblemDetail:
      type: object
      description: |
        RFC 7807 problem details object. Every error response from this API
        uses this shape, produced by the service's global exception handler.
        All members are optional per RFC 7807; a member absent from a given
        response was not set by the handler that produced it.
      properties:
        type:
          type: string
          format: uri
          description: |
            A URI identifying the problem type. "about:blank" when the
            handler did not set a more specific type.
        title:
          type: string
          description: A short, human-readable summary of the problem type.
        status:
          type: integer
          format: int32
          description: The HTTP status code generated by the origin server.
        detail:
          type: string
          description: A human-readable explanation specific to this occurrence.
        instance:
          type: string
          format: uri
          description: A URI identifying the specific occurrence of the problem.
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor tem um único tipo de erro para todo o catálogo (Priority: P1)

Quem consome os DTOs gerados de `contracts-catalog` (o próprio `hb-catalog-service`, e futuramente outros serviços) ganha um tipo `ProblemDetail` único para desserializar qualquer resposta de erro do catálogo, em vez de tratar cada desfecho como uma forma não-modelada.

**Why this priority**: É o entregável central e o único desta task — sem o schema declarado, não há o que as tasks seguintes (`T-002-7-2..6`) possam referenciar.

**Independent Test**: `components/schemas` do documento contém `ProblemDetail` com os cinco membros canônicos; build do módulo `contracts-catalog` verde e gera `ProblemDetail.java`.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor inspeciona `components/schemas`, **Then** encontra `ProblemDetail` com `type`, `title`, `status`, `detail` e `instance`.
2. **Given** o schema, **When** inspecionado, **Then** nenhum membro é `required` — todos opcionais, refletindo a RFC 7807.
3. **Given** o schema, **When** inspecionado, **Then** não declara `additionalProperties: false`.
4. **Given** a edição concluída, **When** o build do módulo `contracts-catalog` roda, **Then** conclui sem erros e o número de DTOs gerados sobe de 6 para 7, com `ProblemDetail` como o único nome novo.
5. **Given** os cinco desfechos de erro existentes (`getProductById` 404, `getStockItemByProductId` 404, `addProductImage` 400/403/404), **When** inspecionados após a edição, **Then** nenhum deles ganhou um bloco `content` — permanecem exatamente como antes, description-only.

---

### User Story 2 - Forma do schema espelha exatamente o que o serviço já emite (Priority: P2)

Quem for referenciar este schema nas próximas cinco subtarefas (`T-002-7-2..6`) confia que ele corresponde ao que `ApiExceptionHandler` realmente produz hoje — sem precisar retrabalhar o schema depois por divergência de forma.

**Why this priority**: Reduz risco de retrabalho nas tasks dependentes, mas é subordinada a US1: sem o schema existir, não há forma a comparar.

**Independent Test**: Cada membro do schema corresponde a um campo que `org.springframework.http.ProblemDetail` de fato serializa, verificado por leitura de `ApiExceptionHandler.java`.

**Acceptance Scenarios**:

1. **Given** a `description` do schema, **When** lida, **Then** declara que a forma é produzida pelo exception handler global do serviço.
2. **Given** a `description` de `type`, **When** lida, **Then** menciona o valor-padrão `"about:blank"` usado quando o handler não define um tipo mais específico — comportamento real de `ProblemDetail.forStatusAndDetail`.
3. **Given** os tipos declarados, **When** comparados a `ApiExceptionHandler.java`, **Then** `status` é inteiro (não string), e `type`/`instance` são URIs — simétrico ao uso de `java.net.URI` no código.

---

### Edge Cases

- **Schema declarado sem nenhuma referência (órfão) até `T-002-7-6`**: não é erro de OpenAPI 3.1 — um schema em `components/schemas` não precisa ser referenciado por nenhuma operação para ser válido. O build do gerador o processa (e emite `ProblemDetail.java`) independentemente de referência, conforme a configuração `generatorName: spring` sem `modelsToGenerate` restritivo.
- **Nenhum membro obrigatório**: diferente de todo outro schema do documento (todos têm `required`), este não tem. É intencional: a RFC 7807 define os cinco membros como OPTIONAL, e o próprio `ApiExceptionHandler` nem sempre define `instance`, por exemplo. Declarar `required` inventaria uma garantia que o serviço não cumpre uniformemente.
- **Propriedade de extensão `errors` (validação) não modelada**: `MethodArgumentNotValidException` produz um `ProblemDetail` com uma propriedade adicional `errors` (mapa campo→mensagem), fora dos cinco membros canônicos. Modelá-la aqui exigiria decidir se ela é opcional em todo `ProblemDetail` genérico (o que seria falso para a maioria dos erros) ou criar um schema especializado — decisão fora do escopo desta task, que se limita ao `ProblemDetail` genérico da RFC 7807. Fica implicitamente permitida pela ausência de `additionalProperties: false`.
- **`status` como `integer`, não como enum dos códigos do documento**: os desfechos HTTP usados pelo catálogo (`200, 201, 204, 400, 404, 409, 422`, mais `403`, conforme `addProductImage`) não são restringidos aqui — o schema é genérico e reutilizável por qualquer desfecho de erro futuro, e restringir a um enum acoplaria o schema à lista atual de status.
- **`type`/`instance` como `format: uri`, mas sem `maxLength`**: diferente de `ProductImageRequest.url`/`Product.images[]` (que têm teto de 2048 por serem entrada ou eco de entrada do usuário), estes URIs são gerados pelo próprio serviço (constantes `urn:hubinity:catalog:...` ou `"about:blank"`) — não há superfície de entrada externa a limitar.
- **Segundo builds de baseline devem ser medidos nesta execução**: seguindo o critério fixado em `009-product-image-response` (FR-015/016), a contagem de DTOs "6 antes / 7 depois" deve ser capturada empiricamente durante a implementação desta task, não herdada de specs anteriores — a suíte e o `target/` são regenerados a cada build.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O documento MUST declarar, em `components/schemas`, um schema chamado `ProblemDetail` com `type: object`.
- **FR-002**: O schema MUST declarar uma `description` afirmando que a forma é produzida pelo exception handler global do serviço e usada por toda resposta de erro da API.
- **FR-003**: O schema MUST declarar exatamente as cinco propriedades: `type`, `title`, `status`, `detail`, `instance`.
- **FR-004**: `type` MUST declarar `type: string`, `format: uri`, com `description` mencionando o valor-padrão `"about:blank"`.
- **FR-005**: `title` MUST declarar `type: string`.
- **FR-006**: `status` MUST declarar `type: integer`, `format: int32`.
- **FR-007**: `detail` MUST declarar `type: string`.
- **FR-008**: `instance` MUST declarar `type: string`, `format: uri`.
- **FR-009**: O schema MUST NOT declarar um array `required` — nenhuma das cinco propriedades é obrigatória, refletindo a RFC 7807.
- **FR-010**: O schema MUST NOT declarar `additionalProperties: false`, preservando a postura de leitor tolerante já adotada pelos demais schemas do documento e permitindo propriedades de extensão (ex.: `errors`) emitidas pelo serviço em alguns desfechos.
- **FR-011**: Nenhum dos cinco desfechos de erro existentes sem `content` (`getProductById` 404, `getStockItemByProductId` 404, `addProductImage` 400/403/404) MUST ser alterado por esta task — permanecem description-only, a cargo de `T-002-7-2` até `T-002-7-6`.
- **FR-012**: Nenhum schema, operação ou `description` de Path Item pré-existente MUST ser alterado por esta task — a mudança é estritamente aditiva. Critério objetivo: no diff, nenhuma linha aparece como `-`.
- **FR-013**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-014**: A entrega MUST provar que exatamente um DTO adicional é gerado (6 → 7), com `ProblemDetail` como único nome novo e checksum idêntico nos 6 preexistentes. A captura do inventário MUST ser feita antes da edição e nesta execução — reaproveitar arquivo de baseline de outra spec é proibido, pois `target/` é regenerado a cada build.
- **FR-015**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde no `hb-catalog-service`, medido nesta execução (não herdado de baseline de spec anterior).

### Key Entities

- **Schema `ProblemDetail` (novo)**: forma RFC 7807 genérica de erro; sétimo schema do documento; ainda não referenciado por nenhuma operação ao final desta task.
- **Desfechos `404`/`400`/`403` das três operações existentes (inalterados — alvo de `T-002-7-2` a `T-002-7-6`)**.
- **Schemas pré-existentes (`Product`, `Category`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`) — intocados**.

## Success Criteria *(mandatory)*

- **SC-001**: O contrato passa a ter um tipo de erro único e reutilizável, disponível para ser referenciado pelas cinco subtarefas seguintes sem retrabalho de forma.
- **SC-002**: O schema espelha fielmente o que `ApiExceptionHandler` produz em runtime, verificado por leitura direta do código.
- **SC-003**: O documento permanece válido — build do módulo verde.
- **SC-004**: A geração é previsível: exatamente um DTO novo, nenhum preexistente alterado.
- **SC-005**: Regressão zero no consumidor (`hb-catalog-service`).
- **SC-006**: A cadeia `T-002-7` avança: resta `T-002-7-2` até `T-002-7-6`, cada uma referenciando este schema em um desfecho específico.

## Assumptions

- Os cinco membros canônicos e sua tipagem seguem RFC 7807 e a serialização real de `org.springframework.http.ProblemDetail`, verificada em `ApiExceptionHandler.java` — não há decisão de produto pendente aqui, apenas transcrição de um contrato já implícito no código.
- **Posição em `components/schemas`**: anexado ao final, após `ProductImageResponse` — ordem de chaves não tem significado semântico, mesma convenção de `009-product-image-response`.
- A propriedade de extensão `errors` (erros de validação de campo) é deliberadamente não modelada nesta task; sua ausência de `additionalProperties: false` a permite sem exigir declaração explícita.
- Autoridade de validação e workflow herdados da cadeia `T-002`; `contracts-catalog` deve ser reinstalado (`mvn -B -DskipTests install`) antes de qualquer consumidor recompilar.

## Out of Scope

- Referenciar `ProblemDetail` no `content` de `getProductById` 404 (**T-002-7-2**).
- Referenciar `ProblemDetail` no `content` de `getStockItemByProductId` 404 (**T-002-7-3**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 400 (**T-002-7-4**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 403 (**T-002-7-5**).
- Referenciar `ProblemDetail` no `content` de `addProductImage` 404 (**T-002-7-6**).
- Modelagem da propriedade de extensão `errors` de erros de validação de campo — não pertence ao `ProblemDetail` genérico.
