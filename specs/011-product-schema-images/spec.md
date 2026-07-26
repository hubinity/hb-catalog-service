# Feature Specification: Propriedade images no schema Product

**Feature Branch**: `011-product-schema-images`

**Created**: 2026-07-26

**Status**: Draft

**Input**: User description: "Adicionar a propriedade images ao schema Product em contracts-catalog/openapi/catalog.yaml, para que as operações de leitura de produto exponham as URLs de imagem no contrato."

**Task de origem**: `T-002-6` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-4]` (**concluída**, commit `4fa9056`). **Task nascida de uma lacuna**: foi a spec `009-product-image-response` que a identificou e propôs; o usuário a inseriu no tracker.

## Contexto técnico verificado (código real)

- **A cadeia T-002 original está fechada.** `T-002-5` entregou em `c6eaccb` a estratégia URL-only declarada em `info.description` e a `description` do Path Item em estado final. `T-002-1` a `T-002-5` estão `done`.
- **O schema `Product` hoje não tem `images`.** Suas propriedades são `id`, `sku`, `name`, `description`, `price`, `categoryId`, `active`, `createdAt`, `updatedAt`; `required` é `[id, sku, name, price, categoryId, active]` — ou seja, `description`, `createdAt` e `updatedAt` **já são opcionais**, o que dá precedente direto para a decisão desta task.
- **`Product` é referenciado exatamente uma vez** no documento (linha 52): o desfecho `200` de `getProductById`. **Nunca é corpo de requisição** — o único `requestBody` do documento é o de `addProductImage`, que usa `ProductImageRequest`. Verificado por varredura, não presumido.
- **`ProductImageResponse.images` já existe como molde exato** da forma pretendida: `type: array`, itens `type: string` / `format: uri` / `maxLength: 2048`, com a convenção posicional "primeiro elemento = imagem principal" fixada em `T-002-3`.
- **Contagem de geração hoje**: **6 schemas ↔ 6 DTOs**, verificado em `contracts-catalog/target/generated-sources/`.
- **`T-002-5` preparou o terreno para esta task.** A justificativa registrada para colocar a estratégia em `info.description` — e não no Path Item de imagens — foi precisamente alcançar quem lê a operação de produto **depois que `T-002-6` expusesse `images` no schema `Product`**. Esta task materializa esse alcance; sem ela, a decisão de posicionamento de `T-002-5` fica sem o beneficiário que a motivou.

## Descoberta desta spec: os DTOs gerados ainda não são consumidos

Varredura de `hb-catalog-service/src/`: **nenhum arquivo importa `com.hubinity.contracts`**. O serviço declara a dependência `contracts-catalog` em `pom.xml` (linha 175) mas usa DTOs próprios, escritos à mão, em `api/dto/` — incluindo um `ProductResponse.java` que é paralelo ao `Product` do contrato, não derivado dele.

**Duas consequências, ambas relevantes para esta task:**

1. **O gate de geração muda de forma em relação a `T-002-5`.** Aquela task exigia **inércia total** (nenhum arquivo gerado podia mudar). Esta **altera deliberadamente** um artefato gerado: `Product.java` ganha um campo. O critério correto é, portanto, **6 ↔ 6 arquivos, nenhum nome novo, checksum alterado em exatamente um — `Product.java`** — com os outros cinco idênticos. É mais forte que "o build passou": prova que a mudança atingiu o alvo pretendido e **apenas** ele.

2. **A regressão no consumidor é estruturalmente nula, e por um motivo que convém declarar em vez de celebrar.** Nada quebra porque nada usa. Isso torna o gate de regressão barato, mas também expõe que contrato e serviço mantêm DTOs paralelos — a divergência que `T-003-4` (campo `images` em `ProductResponse`) vai aprofundar do lado do serviço. Esta task **não** resolve isso; registra (ver *Out of Scope*).

## Decisões tomadas por evidência

1. **`images` é opcional — fica fora de `required`.** Produto sem imagem é caso legítimo e presumivelmente comum (todo produto nasce sem imagem, já que a URL é registrada por chamada posterior a `POST …/images`). Tornar `images` obrigatório quebraria a leitura de qualquer produto ainda não ilustrado. O schema já tem três propriedades opcionais, então a decisão não introduz padrão novo.

2. **Forma simétrica a `ProductImageResponse.images`**: `type: array`, itens `type: string` / `format: uri` / `maxLength: 2048`. Duas representações divergentes da mesma coleção, no mesmo documento, seriam defeito — o consumidor que lê um produto e o que registra uma imagem falam do mesmo dado.

3. **A convenção posicional é redeclarada aqui — e isso não contradiz `T-002-5`.** A spec 010 excluiu deliberadamente a ordenação da declaração de *armazenamento*, por não pertencer a ela. Mas o leitor de `Product` precisa saber qual é a imagem principal, e `ProductImageResponse` — onde a convenção está escrita — descreve a resposta de **outra** operação, que esse leitor pode nunca invocar. Declarar a ordem na `description` de `Product.images` é o que torna a convenção utilizável por quem só lê produto.

4. **Sem `maxItems`.** Teto de cardinalidade pertence ao **atributo**, não a esta exposição — alocação feita por `T-002-3` e reafirmada por `T-002-4`, ambas remetendo o limite à cadeia T-003. Se um teto for criado lá, esta propriedade o herda sem mudança contratual.

5. **Nenhum risco de ambiguidade de escrita.** Em documentos onde o mesmo schema serve de corpo de requisição e de resposta, acrescentar uma propriedade gerenciada por endpoint dedicado criaria dúvida ("posso definir `images` no PUT?"). Aqui **não existe essa ambiguidade**: `Product` só aparece no `200` de `getProductById`. Verificado, e por isso **nenhum `readOnly: true` é declarado** — seria responder a uma pergunta que o documento não faz, e alteraria o DTO gerado sem necessidade.

6. **Mudança estritamente aditiva** — zero remoções, como em `T-002-1`, `T-002-3` e `T-002-4`. `T-002-5` foi a única da linhagem a remover linhas, e por mandato próprio de encerramento de cadeia.

## Decisão de escopo desta task

Entregável único: **acrescentar a propriedade `images` ao schema `Product`**, em `components/schemas`.

**Fora de escopo**: `required` de `Product` (permanece idêntico), schema `ProblemDetail` nos desfechos de erro (**`T-002-7`**, no tracker), operação de coleção de produtos e de categorias (**lacuna nova — ver *Out of Scope***), atributo/coluna/DTOs do serviço e mapper (**cadeia T-003**), implementação (**cadeia T-005**), remoção de imagem (nenhuma task cobre).

**Elemento alvo** — uma adição, nenhuma remoção:

```yaml
        images:
          type: array
          description: |
            Absolute URLs of the product's images, in order — the first
            element is the primary image. Absent or empty when the product
            has no image registered. Images are hosted externally; see the
            URL-only reference strategy declared at the document level.
          items:
            type: string
            format: uri
            maxLength: 2048
```

Posição: anexada ao fim de `properties`, após `updatedAt` — ordem de chaves não tem significado semântico e a lista é de inserção, como fixado em `T-002-4`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Quem lê um produto recebe as imagens dele (Priority: P1)

Quem consome `getProductById` — totem, backoffice, `sc-order-service` — passa a receber as URLs de imagem no mesmo payload do produto, em vez de precisar de uma segunda chamada ou de descobrir que o contrato simplesmente não as descreve. É o entregável central e o que fecha a divergência apontada pela spec 009.

**Why this priority**: sem isso, o serviço devolveria imagens (após `T-003-4`) que o contrato nunca declarou — o contrato descreveria um produto que não existe.

**Independent Test**: o schema `Product` tem `images` como array de URIs; build do módulo verde.

**Acceptance Scenarios**:

1. **Given** o schema `Product`, **When** inspecionado, **Then** possui a propriedade `images` do tipo `array`.
2. **Given** a propriedade `images`, **When** inspecionada, **Then** seus itens declaram `type: string`, `format: uri` e `maxLength: 2048`, simétricos a `ProductImageResponse.images`.
3. **Given** a lista `required` de `Product`, **When** inspecionada, **Then** permanece `[id, sku, name, price, categoryId, active]` — `images` **não** foi acrescentada.
4. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros.

---

### User Story 2 - Quem lê um produto descobre qual imagem é a principal (Priority: P2)

Quem monta a vitrine precisa da imagem principal. A `description` de `images` declara que a coleção vem em ordem e que o primeiro elemento é a principal — tornando a convenção utilizável por quem lê **apenas** produto e talvez nunca chame o endpoint de registro de imagem, onde a convenção estava escrita até agora.

**Why this priority**: importante, mas subordinado — sem a propriedade (US1) não há coleção onde observar ordem.

**Independent Test**: a `description` de `images` afirma a ordenação e o papel do primeiro elemento.

**Acceptance Scenarios**:

1. **Given** a `description` de `images`, **When** lida, **Then** afirma que a coleção vem **em ordem** e que o **primeiro elemento é a imagem principal**.
2. **Given** essa `description`, **When** lida, **Then** remete à estratégia URL-only declarada em nível de documento, sem redeclará-la por extenso.
3. **Given** essa `description`, **When** lida, **Then** deixa claro que a propriedade pode estar **ausente ou vazia** quando o produto não tem imagem.

---

### Edge Cases

- **`images` ausente × array vazio**: sendo opcional, um produto sem imagem pode vir sem a chave **ou** com `[]`. O contrato não força uma das duas formas — forçar seria afirmação sobre o **comportamento** do serviço, própria da cadeia T-005/T-003. A `description` declara que ambas significam "sem imagem", para que o consumidor trate as duas.
- **Nenhum `minItems`**: mesma razão de `T-002-4` — seria afirmação de comportamento, não de forma do documento.
- **Nenhum `readOnly: true`**: verificado que `Product` nunca é corpo de requisição, então não há ambiguidade de escrita a resolver. Declará-lo alteraria o DTO gerado sem necessidade e responderia a uma pergunta que o documento não faz.
- **A convenção posicional passa a estar em dois lugares** (`ProductImageResponse.images` e `Product.images`) — duplicação **deliberada**, não deriva: são dois leitores diferentes, e o de produto não tem por que consultar o schema de resposta de outra operação. O risco de divergência futura é real e fica registrado; a mitigação é que ambas remetem à mesma estratégia de nível de documento.
- **Esta task altera um artefato gerado — a primeira desde `T-002-4` a fazê-lo, e a primeira a alterar um DTO preexistente em vez de criar um novo.** `Product.java` ganha um campo. O gate correspondente exige checksum alterado em **exatamente um** arquivo, não em zero (como `T-002-5`) nem por acréscimo de arquivo (como `T-002-3`/`T-002-4`).
- **Regressão zero é barata aqui, e por um motivo que não deve ser confundido com segurança**: nada em `hb-catalog-service/src/` importa `com.hubinity.contracts`. O build do consumidor passar **não** prova compatibilidade de uso — prova apenas que ninguém usa. Declarar isso evita que a passagem do gate seja lida como garantia mais forte do que é.
- **Divergência contrato × serviço permanece e será aprofundada**: `T-003-4` acrescenta `images` ao `ProductResponse` **do serviço**, um DTO paralelo ao `Product` **do contrato**. Após ambas, o mesmo dado existirá em duas classes sem relação de derivação. Esta task não cria a divergência nem a resolve — registra (ver *Out of Scope*).
- **Produto inativo ou soft-deleted**: `images` não interage com `active` nem com `deleted_at`. Nenhuma semântica condicional é declarada, e nenhuma é necessária.
- **Assimetria deliberada com `ProductImageRequest.url`, que tem `minLength: 1`.** Os itens de `Product.images` **não** o declaram — porque espelham `ProductImageResponse.images`, que também não o declara (escolha de `T-002-4`). A razão é a mesma que dispensa `minItems`: numa **resposta**, restrição não valida nada — descreve o que o servidor produz. `minLength: 1` é útil na **entrada**, onde rejeita string vazia submetida pelo cliente; numa saída seria afirmação sobre o comportamento do serviço, própria das cadeias T-003/T-005. Simetria com a resposta vale mais que simetria com a requisição, porque `Product.images` e `ProductImageResponse.images` descrevem **a mesma coleção**, enquanto `ProductImageRequest.url` descreve uma submissão avulsa. FR-003 fecha o conjunto para que a "correção" de acrescentar `minLength` seja reprovação explícita, e não zelo bem-intencionado.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O schema `Product` MUST receber, em `properties`, uma propriedade `images`.
- **FR-002**: `images` MUST declarar `type: array`.
- **FR-003**: Os itens de `images` MUST declarar `type: string`, `format: uri` e `maxLength: 2048` — e **nenhuma outra restrição**. O conjunto é **fechado**: acrescentar `minLength`, `pattern` ou qualquer outra palavra-chave de validação reprova a entrega, ainda que pareça melhoria. Critério objetivo: as restrições dos itens de `Product.images` MUST ser **exatamente** as de `ProductImageResponse.images`, sem acréscimo nem supressão.
- **FR-004**: `images` MUST declarar uma `description` que afirme (a) que a coleção vem **em ordem** e que o **primeiro elemento é a imagem principal**, e (b) que a propriedade pode estar **ausente ou vazia** quando não há imagem.
- **FR-005**: Essa `description` MUST remeter à estratégia URL-only declarada em nível de documento (`info.description`, entregue por `T-002-5`), sem redeclará-la por extenso.
- **FR-006**: A lista `required` de `Product` MUST permanecer **idêntica** — `images` MUST NOT ser acrescentada a ela.
- **FR-007**: As nove propriedades preexistentes de `Product` (`id`, `sku`, `name`, `description`, `price`, `categoryId`, `active`, `createdAt`, `updatedAt`) MUST permanecer **inalteradas**.
- **FR-008**: `images` MUST NOT declarar `minItems` — é afirmação de comportamento, própria das cadeias T-003/T-005.
- **FR-009**: `images` MUST NOT declarar `maxItems` — teto de cardinalidade pertence ao atributo, alocado à cadeia T-003 por `T-002-3`.
- **FR-010**: `images` MUST NOT declarar `readOnly: true` — `Product` não é corpo de requisição em nenhuma operação, então não há ambiguidade de escrita a resolver.
- **FR-011**: A `description` do próprio schema `Product` MUST permanecer inalterada.
- **FR-012**: Nenhum outro schema MUST ser adicionado, removido ou alterado — em especial `ProductImageRequest` e `ProductImageResponse`, propriedade de `T-002-3` e `T-002-4`.
- **FR-013**: Nenhuma operação, path, parâmetro ou desfecho MUST ser adicionado ou alterado.
- **FR-014**: A mudança MUST ser **estritamente aditiva**. Critério objetivo: no diff, **nenhuma** linha aparece como `-` — usando `git diff -U0 … | grep '^-' | grep -v '^---'`, que também detecta remoção de linha em branco.
- **FR-015**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-016**: A entrega MUST provar que a geração atingiu o alvo pretendido e **apenas** ele: **6 ↔ 6** arquivos, **nenhum nome novo**, checksum alterado em **exatamente um** — `Product.java` — e **idêntico** nos outros cinco. O inventário de baseline MUST ser capturado **antes** da edição **e nesta execução**, em `/tmp/dto-baseline-011.txt`; reaproveitar arquivo de execução anterior é **proibido**, pois `target/` é regenerado a cada build.
- **FR-017**: A entrega MUST confirmar que `Product.java` passou a declarar o campo `images`, e não apenas que seu checksum mudou — checksum diferente prova que algo mudou, não **o quê**.
- **FR-018**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde em `hb-catalog-service`, na mesma contagem de um baseline **medido antes** da edição **e nesta execução**. O relatório MUST registrar que esse gate é fraco por construção — nada em `src/` importa `com.hubinity.contracts` —, para que não seja lido como prova de compatibilidade de uso.

### Key Entities

- **Propriedade `images` (nova)**: coleção ordenada de URLs de imagens do produto; primeiro elemento = imagem principal; opcional.
- **Schema `Product` (existente — recebe uma propriedade; `required` e demais propriedades intocados)**: único schema tocado.
- **DTO gerado `Product.java` (existente — ganha um campo)**: único artefato gerado que muda.
- **Schemas `ProductImageRequest` / `ProductImageResponse` (existentes — intocados)**: propriedade de `T-002-3` / `T-002-4`; o segundo é o molde de forma.
- **`info.description` (existente — intocada)**: entregue por `T-002-5`; alvo da remissão de FR-005.

## Success Criteria *(mandatory)*

- **SC-001**: Quem lê um produto recebe as URLs de imagem no mesmo payload, sem segunda chamada.
- **SC-002**: A imagem principal é identificável por quem lê **apenas** produto, sem consultar o schema de resposta de outra operação.
- **SC-003**: A divergência apontada pela spec 009 — serviço devolveria imagens que o contrato não declara — deixa de existir do lado do contrato.
- **SC-004**: A decisão de posicionamento de `T-002-5` (estratégia em nível de documento) passa a ter o beneficiário que a motivou.
- **SC-005**: O documento permanece válido — build do módulo verde.
- **SC-006**: A geração é previsível e dirigida: exatamente um DTO alterado, nenhum criado ou removido, e a alteração é o campo esperado.
- **SC-007**: Regressão zero no consumidor, com a fraqueza estrutural desse gate declarada em vez de omitida.
- **SC-008**: Restam no tracker, da linhagem T-002, apenas `T-002-7` (`ProblemDetail`) e as lacunas novas registradas por esta spec.

## Assumptions

- **`images` opcional** é decisão desta spec, apoiada em evidência (produto nasce sem imagem; `Product` já tem três propriedades opcionais). Não constava do enunciado da task.
- **`maxLength: 2048` e `format: uri` nos itens** são herdados por simetria de `ProductImageResponse.images`; a alternativa — divergir — seria defeito.
- **A redação exata da `description`** é escolha desta spec; os FRs fixam o conteúdo obrigatório, não as palavras.
- **Posição em `properties`**: anexada após `updatedAt`, por ordem de inserção — critério de `T-002-4`.
- Assume-se que a coleção exposta em `Product.images` é a mesma que `POST …/images` devolve em `ProductImageResponse.images`; nenhuma projeção ou filtragem é declarada.
- Autoridade de validação e workflow herdados da linhagem; `contracts-catalog` reinstalado antes de o consumidor compilar.
- `TASKS.json` permanece intocado por esta spec, como em toda a linhagem.

## Out of Scope

- **Operações de coleção de produtos e de categorias — lacuna nova identificada por esta spec.** O contrato declara **três** paths: `/api/v1/products/{id}`, `/api/v1/products/{productId}/stock` e `/api/v1/products/{productId}/images`. Não há `GET /api/v1/products` (coleção) nem **nenhum** path de `/api/v1/categories`. No entanto, `T-006-1` a `T-006-3` especificam ETag e `If-None-Match` para `GET /api/v1/products`, e `T-006-4` a `T-006-6` o mesmo para `GET /api/v1/categories` — seis tasks que operam sobre endpoints que o contrato **não declara**. Entrada proposta ao usuário:

  ```json
  {
    "id": "T-002-9",
    "phase": "contracts",
    "description": "Declarar em contracts-catalog/openapi/catalog.yaml as operações de coleção ausentes — GET /api/v1/products (listagem paginada) e os paths de /api/v1/categories —, hoje inexistentes no contrato embora o serviço as exponha e as tasks T-006-1..T-006-6 especifiquem ETag/If-None-Match sobre elas. Lacuna identificada na spec 011-product-schema-images",
    "source_reference": "shared-contracts",
    "status": "refined",
    "decomposition_allowed": true,
    "depends_on": ["T-002-6"]
  }
  ```

  `decomposition_allowed: true` — alcança duas famílias de endpoints e provavelmente vale quebrar por recurso. **`TASKS.json` não é editado por esta spec.**

- **Convergência entre `Product` (contrato) e `ProductResponse` (serviço)** — divergência estrutural, não lacuna de uma task. Hoje o serviço não consome nenhum DTO gerado; `T-003-4` acrescentará `images` ao seu `ProductResponse` próprio, deixando o mesmo dado modelado em duas classes sem derivação. Decidir se o serviço deve passar a consumir os DTOs do contrato é decisão de arquitetura com alcance ecossistêmico — cabe a um ADR em `platform-shared-contracts/docs/adr/`, não a uma task de contrato. Registrado para consideração do usuário; nenhuma entrada de tracker é proposta, porque a decisão precede a task.

- **Contradição pendente, ainda não resolvida**: `T-005-3` segue `refined` com a redação "recepção **multipart**", incompatível com a estratégia URL-only ratificada em `T-002-5`. A substituição foi proposta na spec 010 e permanece à espera de decisão do usuário.

- Schema `ProblemDetail` (RFC 7807) nos desfechos de erro (**`T-002-7`**, no tracker).
- Enforcement de HTTPS via `pattern` (**proposta `T-002-8`** na spec 010, ainda não inserida).
- Operação de remoção de imagem — nenhuma task a cobre.
- Atributo, coluna, DTOs do serviço e mapper (**cadeia T-003**); implementação (**cadeia T-005**).
