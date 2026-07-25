# Feature Specification: Path do endpoint de imagens de produto

**Feature Branch**: `006-product-images-path`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Definir o path do endpoint de imagens de produto em platform-shared-contracts/contracts-catalog/openapi/catalog.yaml. Escopo estrito: APENAS a definição do path. Estratégia de armazenamento decidida pelo usuário: URL-only reference."

**Task de origem**: `T-002-1` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: []`. **Primeira task da cadeia T-002** (abre o contrato do endpoint de imagens de produto).

## Contexto técnico verificado (código real)

- **Contrato** (`contracts-catalog/openapi/catalog.yaml`): OpenAPI 3.1.0. Possui `info`, `servers`, `security` global (`bearerAuth`), `tags` (`products`, `stock`), **2 paths** e `components` (`securitySchemes` + 5 schemas).
- **Convenção de path existente — divergente**: `/api/v1/products/{id}` (recurso direto, parâmetro `{id}`) e `/api/v1/products/{productId}/stock` (sub-recurso, parâmetro `{productId}`). O documento **não é uniforme**: sub-recursos usam `{productId}`, o recurso direto usa `{id}`.
- **Path Item como unidade**: o path de estoque (cadeia T-001) demonstra o padrão adotado neste repositório — o Path Item carrega `summary` e `description` próprios, distintos dos da operação.
- **Segurança já é global**: `security: [ { bearerAuth: [] } ]` está no nível raiz (entregue em T-001-5). **Qualquer path novo herda o requisito de JWT bearer automaticamente**, sem declaração própria.
- **Schema `Product` (existente)**: possui `id`, `sku`, `name`, `description`, `price`, `categoryId`, `active`, `createdAt`, `updatedAt`. **Não possui `images`** — o atributo `images[]` descrito no PRD ainda não existe no contrato nem na entidade (é a cadeia T-003).

## Lacuna de origem (registrada explicitamente)

A descrição da task diz **"com base em NÃO ESPECIFICADO NO PRD"**. Verificação do `PRD-HUBINITY.md`:

- Linha 319 — `Product (id, sku, name, description, price, costPrice, active, barcode, images[])`: o atributo `images[]` **é** material de PRD em escopo.
- Linha 946 — item de roadmap 1.11, "Form de cadastro/edição de produto com upload de imagens": escopo **frontend**, não define o contrato do backend.
- Linha 1044 — "Imagens otimizadas servidas via CDN (Cloudflare R2 ou S3 + CloudFront)": está sob **`## 12. Melhorias no MVP`**, ou seja, explicitamente **pós-MVP**.

Conclusão: o PRD justifica a **existência** de imagens de produto, mas **não especifica** este endpoint (endereço, formato, armazenamento). As lacunas foram fechadas por decisão do usuário, registrada abaixo.

## Decisões do usuário (registradas na confirmação do pipeline, 2026-07-25)

1. **Estratégia de armazenamento = "URL-only reference"**. O serviço **não recebe bytes** e **não faz upload multipart**. O atributo `images[]` do Product armazena **apenas URLs** de imagens hospedadas externamente. O endpoint recebe referência(s) de URL, não arquivo binário.
   **Justificativa**: entrega via CDN (R2/S3) é pós-MVP (PRD §12); URL-only evita antecipar infraestrutura, credenciais e configuração de perfis que o MVP não pede.
2. **Prosseguir com a cadeia T-002** apesar da lacuna de PRD, tratando a decisão (1) como premissa de entrada da spec.

## Decisão de escopo desta task

Entregáveis: **(a)** declarar o Path Item `/api/v1/products/{productId}/images` em `paths`; **(b)** declarar seu parâmetro de path `productId` no **nível do Path Item** (compartilhado por todas as operações futuras do path); **(c)** dar ao Path Item `summary` e `description` que declarem o estado da cadeia e a semântica URL-only.

**Fora de escopo desta task**: a declaração da operação POST (T-002-2), o formato do corpo da requisição (T-002-3), o schema de resposta (T-002-4), a estratégia de armazenamento documentada no contrato (T-002-5), o atributo `images` no schema `Product` (cadeia T-003) e qualquer implementação no serviço (cadeia T-005).

**Elemento alvo**:

```yaml
  /api/v1/products/{productId}/images:
    summary: Image references for a product
    description: |
      Address for managing a product's image references. Images are stored
      externally; this API records only their URLs (URL-only reference
      strategy) and never receives image bytes. Operations on this path are
      declared by the remaining T-002 tasks. Access requires a valid Bearer
      JWT (inherited from the document-level bearerAuth requirement).
    parameters:
      - name: productId
        in: path
        required: true
        description: Product UUID
        schema:
          type: string
          format: uuid
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor localiza o endereço canônico das imagens de um produto (Priority: P1)

Uma equipe consumidora (ex.: `hb-catalog-web`, que construirá o formulário de produto do roadmap 1.11) abre o contrato e encontra o endereço canônico onde as imagens de um produto são gerenciadas, aninhado sob o produto ao qual pertencem — sem precisar adivinhar o endereço nem perguntar ao time de backend.

**Why this priority**: É o entregável central da task e o pré-requisito estrutural de toda a cadeia T-002 — nenhuma operação, corpo ou resposta pode ser declarada antes de existir um Path Item que os hospede.

**Independent Test**: O documento contém o Path Item `/api/v1/products/{productId}/images`; o build do módulo conclui verde. Verificável por inspeção isolada, sem qualquer outra task da cadeia.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** um consumidor procura o endereço das imagens de um produto, **Then** encontra `/api/v1/products/{productId}/images` sob `paths`.
2. **Given** o Path Item, **When** o consumidor lê sua `description`, **Then** entende que a API registra **apenas URLs** de imagens e nunca recebe bytes.
3. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros e o documento permanece um OpenAPI 3.1 válido.

---

### User Story 2 - O parâmetro de identificação do produto é declarado uma única vez para o path (Priority: P2)

Quem declarar a operação POST na task seguinte (T-002-2) encontra o parâmetro `productId` já declarado no nível do Path Item, herdado por qualquer operação que o path venha a ter, sem precisar repetir a declaração por operação.

**Why this priority**: Evita divergência entre operações do mesmo path e reduz o trabalho das tasks seguintes da cadeia; é estrutural, mas subordinado à existência do path.

**Independent Test**: O Path Item declara `parameters` com `productId` (`in: path`, `required: true`, `type: string`/`format: uuid`); nenhuma operação é necessária para verificar.

**Acceptance Scenarios**:

1. **Given** o Path Item, **When** inspecionados seus `parameters`, **Then** contém `productId` com `in: path`, `required: true` e schema `string`/`format: uuid`.
2. **Given** o parâmetro no nível do Path Item, **When** uma operação for adicionada em T-002-2, **Then** ela herda `productId` sem redeclará-lo.
3. **Given** o template do path (`{productId}`), **When** comparado ao nome do parâmetro declarado, **Then** os dois coincidem exatamente (exigência de validade do OpenAPI).

---

### Edge Cases

- **Divergência de nomenclatura do contrato (`{id}` vs `{productId}`)**: o documento já é inconsistente — `/api/v1/products/{id}` usa `{id}`, `/api/v1/products/{productId}/stock` usa `{productId}`. Esta task adota **`{productId}`**, alinhando-se ao precedente de **sub-recurso** (`/stock`), que é o caso análogo. A inconsistência preexistente em `/api/v1/products/{id}` **não é corrigida aqui** (seria mudança quebrante em operação alheia à cadeia T-002).
- **"Upload" no nome da task vs. semântica real**: a task fala em "endpoint de upload", mas a decisão URL-only significa que **nenhum byte trafega**. O path é nomeado `/images` (o recurso), não `/upload` (a ação) — mantendo o estilo orientado a recurso do contrato e permanecendo correto sob qualquer estratégia futura.
- **Path Item sem operações é transitoriamente válido**: entre esta task e T-002-2 o Path Item existirá sem nenhuma operação declarada. Isso é **válido** em OpenAPI 3.1 (todos os campos de operação de um Path Item são opcionais) e é o mesmo padrão já usado na cadeia T-001.
- **Segurança não é redeclarada**: o requisito `bearerAuth` global (nível raiz) já cobre o novo path. Declarar `security` próprio aqui seria redundante e arriscaria divergir do documento.
- **Autorização (role) vs. autenticação — deferida conscientemente**: o Princípio VI da constituição exige que endpoints de **mutação** sejam protegidos por `@PreAuthorize("hasRole('admin')")`. As operações futuras deste path **são mutações** e, portanto, exigirão a role `admin` no serviço. O contrato, porém, modela hoje **apenas autenticação** (`bearerAuth` global) — precedente estabelecido em T-001-5, onde o reforço de role foi explicitamente deixado para task própria. Esta task **não** introduz modelagem de role: um Path Item não é o lugar para expressá-la, e fazê-lo aqui divergiria do documento. A obrigação permanece viva para **T-002-2** (que declara a operação) e para a **cadeia T-005** (que a implementa).
- **Ausência de `images` no schema `Product`**: o path passa a existir antes do atributo que ele alimenta (cadeia T-003). Isso é aceitável porque as cadeias são independentes por construção, mas significa que o contrato descreve, transitoriamente, um endereço cujo efeito no recurso `Product` ainda não é observável.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O documento MUST declarar, sob `paths`, o Path Item `/api/v1/products/{productId}/images`.
- **FR-002**: O Path Item MUST declarar `parameters` no seu próprio nível (não no nível de operação), contendo `productId` com `in: path`, `required: true`, `description` e schema `type: string` / `format: uuid`.
- **FR-003**: O nome do parâmetro declarado MUST coincidir exatamente com o template `{productId}` usado no endereço do path.
- **FR-004**: O Path Item MUST declarar `summary` e `description`; a `description` MUST explicitar (a) a semântica **URL-only** — a API registra apenas URLs e não recebe bytes — e (b) que as operações são declaradas pelas tasks restantes da cadeia T-002.
- **FR-005**: O Path Item MUST NOT declarar `security` próprio — herda o requisito `bearerAuth` do nível raiz do documento.
- **FR-006**: O Path Item MUST NOT declarar nenhuma operação (`post`, `get`, etc.), corpo de requisição ou schema de resposta — esses são entregáveis de T-002-2, T-002-3 e T-002-4.
- **FR-007**: A mudança MUST ser estritamente aditiva: adiciona um Path Item novo e **nada mais**. Nenhuma linha preexistente do documento pode ser alterada, removida ou reordenada — nem semanticamente, nem por reformatação/reindentação. Em especial `/api/v1/products/{id}` permanece byte-a-byte inalterado, com sua nomenclatura divergente. Critério objetivo: no diff da mudança, todas as linhas preexistentes aparecem como contexto, nenhuma como `+`/`-`.
- **FR-008**: O documento MUST permanecer um OpenAPI 3.1 válido após a edição, comprovado pelo build do módulo `contracts-catalog`.
- **FR-009**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde em `hb-catalog-service` com o artefato de contratos reinstalado.
- **FR-010**: O Path Item MUST NOT declarar `tags`. `tags` é campo de **operação**, não de Path Item, no OpenAPI 3.1 — a marcação com a tag `products` é entregável de T-002-2, junto com a operação.

### Key Entities

- **Path Item `/api/v1/products/{productId}/images` (novo)**: o endereço canônico das referências de imagem de um produto; unidade entregue por esta task.
- **Parâmetro `productId` (novo, nível de Path Item)**: identifica o produto dono das imagens; UUID, compartilhado pelas operações futuras do path.
- **Requisito `security` global `bearerAuth` (existente — herdado, intocado)**: cobre o novo path sem declaração própria.
- **Path Item `/api/v1/products/{productId}/stock` (existente — precedente de convenção, intocado)**: fonte do padrão de sub-recurso e do nome `{productId}`.
- **Schema `Product` (existente — intocado)**: destinatário futuro do atributo `images[]` (cadeia T-003), fora do escopo desta task.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Um consumidor localiza o endereço das imagens de um produto por inspeção direta do contrato, sem consultar o time de backend nem o código do serviço.
- **SC-002**: O documento permanece válido e processável — build do módulo `contracts-catalog` verde após a edição.
- **SC-003**: A estratégia URL-only fica declarada no ponto de entrada do recurso, de modo que nenhum consumidor implemente envio de bytes por engano.
- **SC-004**: A cadeia T-002 fica **desbloqueada**: T-002-2 pode declarar a operação POST sobre um Path Item existente, com o parâmetro de produto já resolvido.
- **SC-005**: Regressão zero — `mvn -B verify` verde em `hb-catalog-service` após reinstalar o artefato de contratos.

## Assumptions

- **Armazenamento URL-only** é premissa de entrada, decidida pelo usuário em 2026-07-25, não uma inferência desta spec. Toda a cadeia T-002 herda essa premissa.
- **Nome do parâmetro `{productId}`** foi escolhido por alinhamento ao precedente de sub-recurso (`/stock`), e não ao `{id}` do recurso direto — decisão de consistência tomada nesta spec, dado que o contrato é ambíguo.
- **Path orientado a recurso (`/images`) e não a ação (`/upload`)** — coerente com o estilo do documento e resistente a uma futura mudança de estratégia de armazenamento.
- **Pluralidade do recurso**: `/images` (coleção) assume que um produto pode ter **múltiplas** imagens, fiel a `images[]` (array) no PRD linha 319.
- **Posição no arquivo**: o novo Path Item é inserido **após** `/api/v1/products/{productId}/stock`, como último item de `paths`. A ordem das chaves de um mapping YAML **não tem significado semântico** em OpenAPI; a convenção é adotada apenas para minimizar o diff e agrupar os sub-recursos de produto.
- **`tags` pertence à operação, não ao Path Item**: por isso a ausência de `tags` nesta entrega não é uma lacuna — é conformidade com a estrutura do OpenAPI 3.1 (ver FR-010).
- **Autoridade de validação e workflow herdados da cadeia T-001**: build do módulo como validador; trabalho na branch de feature corrente; commits na fase de polish.
- O `contracts-catalog` deve ser reinstalado localmente (`mvn -B -DskipTests install`) antes de `hb-catalog-service` compilar contra o contrato alterado.

## Out of Scope

- Declaração da operação POST no path (**T-002-2**).
- Formato do corpo da requisição (**T-002-3**) — que, sob a decisão URL-only, deixa de ser multipart e **precisará ser reescrita ou descartada**.
- Schema de resposta do endpoint (**T-002-4**).
- Documentação da estratégia de armazenamento no contrato (**T-002-5**).
- Adição do atributo `images` ao schema `Product` do contrato, à entidade JPA, aos DTOs e ao mapper (**cadeia T-003**).
- Implementação no serviço: `ProductService`, `ProductController`, persistência (**cadeia T-005**).
- Correção da nomenclatura divergente de `/api/v1/products/{id}` para `{productId}`.
- **Modelagem do requisito de role `admin`** para as operações de mutação deste path — deferida a T-002-2 (contrato) e à cadeia T-005 (serviço), conforme o precedente de T-001-5 e o Princípio VI da constituição. Ver Edge Cases.
- Validação do formato/host das URLs de imagem, políticas de CDN, otimização WebP/AVIF (PRD §12, pós-MVP).
