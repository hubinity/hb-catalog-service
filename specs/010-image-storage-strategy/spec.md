# Feature Specification: Estratégia de armazenamento de imagens de produto

**Feature Branch**: `010-image-storage-strategy`

**Created**: 2026-07-25

**Status**: Draft

**Input**: User description: "Definir estratégia de armazenamento de imagens de produto em contracts-catalog/openapi/catalog.yaml com base em NÃO ESPECIFICADO NO PRD."

**Task de origem**: `T-002-5` (TASKS.json, fase `contracts`, `decomposition_allowed: false`) — `depends_on: [T-002-4]` (**concluída**, commit `4fa9056`). **Última task da cadeia T-002** — encerra o contrato da operação de imagens, como `T-001-5` encerrou o da leitura de saldo.

## Contexto técnico verificado (código real)

- **A cadeia T-002 está a uma task do fim**: `T-002-1` (`fd9b905`) criou o Path Item, `T-002-2` (`854c02f`) declarou a operação `addProductImage`, `T-002-3` (`40dd8e0`) o `requestBody` + `ProductImageRequest`, `T-002-4` (`4fa9056`) o `content` do `201` + `ProductImageResponse`.
- **Contagem de geração hoje**: **6 schemas ↔ 6 DTOs** (`Product`, `Category`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`). Verificado empiricamente em `contracts-catalog/target/generated-sources/`.
- **A estratégia URL-only já está no documento — mas dispersa, e apenas como prosa incidental.** Aparece em quatro pontos, sempre como justificativa de outra decisão, nunca como declaração própria:
  1. `description` do Path Item — "Images are stored externally; this API records only their URLs (URL-only reference strategy) and never receives image bytes";
  2. `description` de `ProductImageRequest` — "The catalog stores only the URL; image bytes are never transmitted";
  3. `description` do `201` — "under the URL-only strategy there is no per-image resource URI";
  4. `ProductImageResponse.images` — array de `format: uri`.
- **A `description` do Path Item ainda carrega andaime de proveniência**: "The POST operation is declared; its request body and response body are completed by T-002-3 and T-002-4". Ambas concluíram — a frase agora é falsa como estado e obsoleta como proveniência.
- **Precedente direto de encerramento de cadeia (`T-001-5`, `68873d5`)**: a última task da cadeia T-001 (a) acrescentou o elemento transversal (`securitySchemes` + `security` raiz) e (b) **reescreveu a `description` do Path Item para o estado final**, sem menção a tasks pendentes. O resultado está vivo no documento: "The GET operation, its productId parameter, the response body, and the authorization requirement are fully declared (T-001 chain complete)." Esta task espelha essa forma.
- **`info.description` hoje é puramente descritiva** — "Contract for the Catalog domain. Owns products, categories, and on-hand stock. Backend services consume the generated DTOs from this module." Não carrega nenhuma decisão normativa; é o único ponto do documento acima do nível de path.
- **HTTPS é hoje apenas expectativa textual**, dentro de `ProductImageRequest.description`. Não há `pattern`, nem qualquer restrição de schema que a torne verificável.

## Decisão do usuário (registrada na confirmação do pipeline, 2026-07-25)

**Ratificar a estratégia URL-only.** O PRD não especifica armazenamento de imagens; entre ratificar o URL-only já embutido pela cadeia, migrar para object storage gerenciado pelo serviço (multipart) ou adotar upload por URL pré-assinada, o usuário escolheu **ratificar o URL-only**.

**Justificativa registrada**: as três tasks concluídas `T-002-2`, `T-002-3` e `T-002-4` já congelaram essa estratégia no contrato — corpo JSON com uma única `url`, ausência deliberada de `Location`, resposta como coleção de URIs. Qualquer outra escolha reabriria tasks marcadas `done`. Ratificar é a única opção que preserva a cadeia.

**Consequência downstream aceita pelo usuário**: a task `T-005-3` ("Definir a recepção **multipart** do handler de upload de imagens de produto") contradiz frontalmente esta estratégia e fica **inválida como escrita**. Registrada em *Out of Scope* com proposta de reescrita; **`TASKS.json` não é editado por esta spec.**

## Decisões tomadas por evidência

1. **A declaração canônica da estratégia vai para `info.description`, nível do documento — não para o Path Item de imagens.**

   **Razão decisiva**: a estratégia não governa apenas o path de imagens. `T-002-6` (já no tracker, `refined`) adiciona a propriedade `images` ao schema `Product`, que é servido por `/api/v1/products/{id}` — **outro path**. Uma estratégia declarada dentro do Path Item de imagens seria invisível para quem lê a operação de leitura de produto e encontra um array de URLs sem saber que o serviço não hospeda nada daquilo. `info.description` é o único ponto do documento que alcança ambos.

   Há precedente de forma em `T-001-5`: quando a decisão é transversal, ela sobe para o nível do documento (lá, `securitySchemes` + `security` raiz) em vez de ser repetida por operação.

2. **A `description` do Path Item é reescrita para o estado final**, encerrando a cadeia T-002 e removendo o andaime "completed by T-002-3 and T-002-4". Esta é a única remoção da entrega, e era **alocada explicitamente a esta task** pelas specs 008 e 009 ("a limpeza final da `description` cabe à última task da cadeia — T-002-5, espelhando o que T-001-5 fez").

3. **A estratégia é declarada como decisão normativa, não como paráfrase do que já está lá.** Restringir-se a repetir "armazenamos só a URL" não entregaria nada: o documento já diz isso três vezes. O conteúdo próprio desta task são as **consequências** que hoje não estão declaradas em lugar nenhum — em especial a **fronteira de responsabilidade**: o catálogo não verifica que a URL resolve, e uma referência que deixa de resolver **não é violação de contrato**. Sem isso, um consumidor pode razoavelmente presumir integridade referencial que o serviço nunca prometeu.

4. **Nenhum schema é tocado.** `ProductImageRequest` e `ProductImageResponse` pertencem a `T-002-3` e `T-002-4`, ambas `done`. Em particular, **HTTPS não vira `pattern`** — ver *Out of Scope*.

5. **Consequência de geração: esta é a primeira task da cadeia T-002 que gera zero código.** `info.description` e a `description` de um Path Item não produzem DTO. A contagem permanece **6 ↔ 6**. Isso torna a verificação mais forte que nas tasks anteriores, não mais fraca: o critério deixa de ser "exatamente um DTO novo" e passa a ser "**nenhum** arquivo gerado muda, por checksum".

## Decisão de escopo desta task

Entregáveis: **(a)** declarar a estratégia URL-only como decisão normativa em `info.description`; **(b)** reescrever a `description` do Path Item de imagens para o estado final (cadeia T-002 completa).

**Fora de escopo**: propriedade `images` no schema `Product` (**`T-002-6`**), schema `ProblemDetail` nos desfechos de erro (**`T-002-7`**), qualquer alteração em `ProductImageRequest`/`ProductImageResponse` (**`T-002-3`/`T-002-4`, `done`**), enforcement de HTTPS via `pattern`, operação de remoção de imagem, atributo/coluna/DTOs do serviço (**cadeia T-003**), implementação (**cadeia T-005**).

**Elementos alvo** — uma adição e uma reescrita:

**(1)** parágrafo acrescentado a `info.description` (as duas linhas existentes permanecem intactas acima dele):

```yaml
info:
  title: Hubinity Catalog API
  version: 0.1.0
  description: |
    Contract for the Catalog domain. Owns products, categories, and on-hand stock.
    Backend services consume the generated DTOs from this module.

    Product image storage — URL-only reference strategy: product images are
    hosted outside this system. The catalog persists only absolute URLs and
    never receives, stores, or serves image bytes; there is no upload,
    download, or transformation operation, and no per-image resource URI.
    HTTPS URLs are expected — an http:// URL will be blocked as mixed content
    by browsers rendering the catalog over HTTPS. Availability and lifecycle
    of the referenced file belong to the external host: the catalog does not
    verify that a URL resolves, and a reference that stops resolving is not a
    contract violation. Removing an image removes the reference, never a
    stored file.
```

**(2)** `description` do Path Item `/api/v1/products/{productId}/images`, reescrita para o estado final:

```yaml
  /api/v1/products/{productId}/images:
    summary: Image references for a product
    description: |
      Address for managing a product's image references. The POST operation,
      its request body, its response body, and the authorization requirement
      are fully declared (T-002 chain complete). Images follow the URL-only
      reference strategy declared at the document level: this API records only
      their URLs and never receives image bytes. Access requires a valid
      Bearer JWT (inherited from the document-level bearerAuth security
      scheme); registering an image additionally requires the admin role.
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor descobre a fronteira de responsabilidade antes de integrar (Priority: P1)

Quem vai consumir o catálogo (totem, backoffice, `sc-order-service`) lê o contrato de cima para baixo e, antes de qualquer operação, encontra declarado que o sistema **não hospeda imagens** — só guarda endereços — e que **não garante que esses endereços resolvam**. Com isso planeja o próprio fallback (placeholder, cache, revalidação) em vez de descobrir o vazio em produção.

**Why this priority**: É o entregável central e a única informação genuinamente nova da task. As demais consequências (sem upload, sem `Location`) já são dedutíveis do documento; a **não-garantia de resolução** não é dedutível de nada e é exatamente onde uma integração ingênua quebra.

**Independent Test**: `info.description` declara a estratégia e afirma explicitamente que o catálogo não verifica resolução e que uma URL morta não é violação de contrato; build do módulo verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** o consumidor lê `info.description`, **Then** encontra a estratégia URL-only nomeada e declarada como decisão do contrato.
2. **Given** essa declaração, **When** lida, **Then** afirma que o catálogo nunca recebe, armazena ou serve bytes de imagem.
3. **Given** essa declaração, **When** lida, **Then** afirma que disponibilidade e ciclo de vida do arquivo pertencem ao host externo, que o catálogo não verifica resolução, e que uma referência que deixa de resolver não é violação de contrato.
4. **Given** essa declaração, **When** lida, **Then** afirma que remover uma imagem remove a referência, nunca um arquivo armazenado.
5. **Given** as duas linhas originais de `info.description`, **When** o diff é inspecionado, **Then** permanecem inalteradas.

---

### User Story 2 - Leitor da operação de produto entende URLs que não são nossas (Priority: P2)

Quem lê `/api/v1/products/{id}` — hoje, e sobretudo depois que `T-002-6` acrescentar `images` ao schema `Product` — encontra URLs de imagem sem que o path de produto diga nada sobre hospedagem. A declaração em nível de documento cobre esse leitor, que um texto preso ao path de imagens jamais alcançaria.

**Why this priority**: Importante e é a razão da escolha de posicionamento, mas subordinado: o valor só se materializa por inteiro quando `T-002-6` expuser `images` no schema `Product`.

**Independent Test**: A declaração está em `info.description`, acima de `paths`, e não dentro do Path Item de imagens — verificável por posição no documento.

**Acceptance Scenarios**:

1. **Given** o documento, **When** se localiza a declaração canônica da estratégia, **Then** ela está em `info.description`, não em um Path Item.
2. **Given** o Path Item de imagens, **When** lido, **Then** remete à estratégia declarada em nível de documento em vez de redeclará-la por extenso.

---

### User Story 3 - Leitor do contrato para de ver tasks pendentes que já terminaram (Priority: P3)

A `description` do Path Item de imagens afirma que corpo de requisição e de resposta "are completed by T-002-3 and T-002-4". Ambas concluíram. Quem lê hoje vê um contrato que se descreve como inacabado — e ainda expõe identificadores de um tracker interno a um consumidor externo. A reescrita final encerra a cadeia, como `T-001-5` fez.

**Why this priority**: É higiene de documento, não capacidade nova — mas é dívida explicitamente alocada a esta task pelas specs 008 e 009, e ninguém mais a pagará.

**Independent Test**: A `description` do Path Item não contém "T-002-3", "T-002-4" nem qualquer promessa de trabalho futuro; declara a cadeia completa.

**Acceptance Scenarios**:

1. **Given** a `description` do Path Item, **When** lida após a entrega, **Then** não menciona tasks a completar.
2. **Given** essa `description`, **When** lida, **Then** declara operação, corpo de requisição, corpo de resposta e autorização como plenamente declarados (cadeia T-002 completa).
3. **Given** essa `description`, **When** comparada à do Path Item de saldo (`T-001-5`), **Then** segue a mesma forma de encerramento de cadeia.
4. **Given** essa `description`, **When** lida, **Then** preserva a informação de autorização já presente (Bearer JWT herdado + role admin para registro).

---

### Edge Cases

- **A estratégia já estava escrita no documento — então o que esta task entrega?** Estava escrita como *justificativa incidental* de três outras decisões, nunca como decisão própria, e sempre dentro do escopo de imagens. Esta task a promove a declaração normativa em nível de documento e acrescenta a consequência que não estava em lugar nenhum: a **não-garantia de resolução**. Sem isso, a task seria uma no-op de prosa.
- **"NÃO ESPECIFICADO NO PRD" no `source_reference`**: a task não tem fonte a consultar. A decisão foi tomada pelo usuário nesta sessão e está registrada acima; a spec **não** infere estratégia de armazenamento a partir de código.
- **Ratificar uma decisão já tomada de fato não é formalidade vazia**: sem ratificação explícita, a estratégia permaneceria uma propriedade emergente de três tasks concluídas, que ninguém decidiu deliberadamente e qualquer task futura poderia contradizer sem perceber — exatamente o que `T-005-3` faz hoje.
- **HTTPS continua expectativa, não restrição verificável**: nenhum `pattern` é adicionado. Um `pattern: '^https://'` mudaria a validação gerada (anotação `@Pattern` no DTO), reabriria `ProductImageRequest` — propriedade de `T-002-3`, `done` — e rejeitaria em runtime URLs `http://` que hoje o contrato apenas desaconselha. É mudança de comportamento disfarçada de documentação. Registrada como lacuna com encaminhamento próprio em *Out of Scope*.
- **A estratégia não impede que o serviço um dia hospede imagens** — mas impede que o faça **calado**. Migrar para object storage passaria a exigir mudança de contrato visível, com versão, em vez de deriva silenciosa entre serviço e contrato.
- **Remoção de imagem não existe como operação**: a declaração afirma o que *removeria* uma imagem (a referência, não o arquivo) sem declarar a operação, que nenhuma task cobre. É afirmação sobre a semântica da estratégia, não promessa de endpoint.
- **Ordenação não é redeclarada aqui**: "primeiro elemento = imagem principal" é convenção fixada por `T-002-3` e observável por `ProductImageResponse` (`T-002-4`). Não pertence a uma declaração de *armazenamento* e repeti-la criaria segunda fonte de verdade.
- **Única remoção da cadeia T-002**: `T-002-1`, `T-002-3` e `T-002-4` foram estritamente aditivas. Esta reescreve um bloco de `description` — pela mesma razão que `T-001-5` reescreveu o dela: só a última task da cadeia pode declarar a cadeia encerrada.
- **Zero código gerado**: nenhum schema é criado ou alterado, então nenhum DTO muda. É a primeira task da cadeia com essa propriedade, e ela **fortalece** a verificação — o critério vira igualdade de checksum em todos os 6 arquivos gerados.
- **`T-005-3` fica inválida a partir desta entrega**: a contradição multipart × URL-only já existia (nasceu quando `T-002-3` fixou corpo JSON), mas ficava latente porque nada declarava a estratégia como decisão. Esta task a torna explícita e detectável. Registrada, não corrigida — `TASKS.json` é do usuário.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `info.description` MUST receber um parágrafo declarando a estratégia de armazenamento de imagens de produto, nomeando-a **URL-only reference strategy**.
- **FR-002**: A declaração MUST afirmar que as imagens são hospedadas **fora** deste sistema e que o catálogo persiste **apenas URLs absolutas**.
- **FR-003**: A declaração MUST afirmar que o catálogo **nunca recebe, armazena ou serve bytes** de imagem, e que não existe operação de upload, download ou transformação, nem URI de recurso por imagem.
- **FR-004**: A declaração MUST afirmar que **disponibilidade e ciclo de vida** do arquivo referenciado pertencem ao host externo, que o catálogo **não verifica** que a URL resolve, e que uma referência que deixa de resolver **não é violação de contrato**.
- **FR-005**: A declaração MUST afirmar que remover uma imagem remove a **referência**, nunca um arquivo armazenado.
- **FR-006**: A declaração MUST registrar a expectativa de **HTTPS** e a consequência de `http://` (bloqueio como mixed content), consistente com o texto já presente em `ProductImageRequest`.
- **FR-007**: As duas linhas preexistentes de `info.description` MUST permanecer **inalteradas**; o parágrafo é acrescentado abaixo delas.
- **FR-008**: A declaração canônica MUST estar em `info.description` — MUST NOT ser declarada dentro de um Path Item ou de um schema.
- **FR-009**: A `description` do Path Item `/api/v1/products/{productId}/images` MUST ser reescrita para o estado final: operação, corpo de requisição, corpo de resposta e autorização declarados, **cadeia T-002 completa**.
- **FR-010**: Essa `description` MUST NOT conter menção a `T-002-3`, `T-002-4` ou a qualquer trabalho futuro.
- **FR-011**: Essa `description` MUST preservar a informação de autorização vigente: Bearer JWT herdado do `bearerAuth` em nível de documento, mais role `admin` para registrar imagem.
- **FR-012**: Essa `description` MUST remeter à estratégia declarada em nível de documento. Critério objetivo, para não depender de julgamento sobre "quanto é repetir demais": a remissão MUST caber em **uma frase**, MUST nomear o nível do documento como origem da declaração, e MUST NOT reproduzir o conteúdo exigido por **FR-004** (não-verificação de resolução e não-violação de contrato), que existe em um único lugar do documento.
- **FR-013**: O `summary` do Path Item MUST permanecer **inalterado**.
- **FR-014**: Os schemas `ProductImageRequest` e `ProductImageResponse` MUST permanecer **intocados** — pertencem a `T-002-3` e `T-002-4`, concluídas.
- **FR-015**: Nenhum `pattern` MUST ser adicionado a `ProductImageRequest.url` — enforcement de HTTPS é mudança de comportamento, fora do escopo desta task.
- **FR-016**: Nenhuma operação, parâmetro, desfecho ou schema MUST ser adicionado, removido ou alterado.
- **FR-017**: A mudança MUST se limitar a exatamente **dois blocos** de `description`: `info.description` (adição) e a do Path Item de imagens (reescrita). Critério objetivo: no diff, toda linha `-` pertence ao bloco `description` do Path Item de imagens.
- **FR-018**: O documento MUST permanecer um OpenAPI 3.1 válido, comprovado pelo build do módulo `contracts-catalog`.
- **FR-019**: A entrega MUST provar que **nenhum** artefato gerado muda: os 6 DTOs de `contracts-catalog` MUST ter checksum idêntico antes e depois, e nenhum nome novo MUST aparecer (6 ↔ 6). O inventário de baseline MUST ser capturado **antes** da edição **e nesta execução** — reaproveitar arquivo de execução anterior é **proibido**, pois `target/` é regenerado a cada build. O arquivo de baseline MUST ter nome próprio da feature (`/tmp/dto-baseline-010.txt`).
- **FR-020**: A entrega MUST provar regressão zero no consumidor: `mvn -B verify` verde em `hb-catalog-service`, na mesma contagem de um baseline **medido antes** da edição **e nesta execução**. Herdar contagem registrada em specs anteriores é **proibido**.

### Key Entities

- **`info.description` (existente — recebe o parágrafo da estratégia; linhas originais intocadas)**: passa a ser o ponto canônico da decisão de armazenamento.
- **`description` do Path Item de imagens (existente — reescrita para estado final)**: encerra a cadeia T-002.
- **`summary` do Path Item (existente — intocado)**.
- **Schemas `ProductImageRequest` / `ProductImageResponse` (existentes — intocados)**: propriedade de `T-002-3` / `T-002-4`.
- **Schema `Product` (existente — intocado)**: alvo de `T-002-6`; principal beneficiário do posicionamento em nível de documento.

## Success Criteria *(mandatory)*

- **SC-001**: Um integrador identifica, lendo o contrato de cima para baixo e antes de qualquer operação, que o sistema não hospeda imagens e não garante que as URLs resolvam.
- **SC-002**: A estratégia alcança também quem lê a operação de produto — hoje, e depois que `T-002-6` expuser `images` em `Product`.
- **SC-003**: O contrato não se descreve mais como inacabado: nenhuma `description` menciona tasks pendentes da cadeia T-002.
- **SC-004**: A estratégia deixa de ser propriedade emergente de tasks concluídas e passa a ser decisão declarada, contra a qual contradições futuras são detectáveis — como a de `T-005-3`, já registrada.
- **SC-005**: O documento permanece válido — build do módulo verde.
- **SC-006**: A geração é comprovadamente inerte: nenhum DTO criado, removido ou alterado.
- **SC-007**: Regressão zero no consumidor.
- **SC-008**: A cadeia T-002 é encerrada. Restam no tracker, como lacunas já registradas, `T-002-6` e `T-002-7`.

## Assumptions

- A ratificação do URL-only foi decisão do usuário em 2026-07-25, na confirmação do pipeline, e é a fonte desta spec — o `source_reference` da task (`NÃO ESPECIFICADO NO PRD`) não oferece nenhuma.
- **Posicionamento em `info.description`** é escolha desta spec, justificada pelo alcance de `T-002-6` (schema `Product`, outro path). Não constava da decisão do usuário, que tratou de *qual* estratégia, não de *onde* declará-la.
- **A redação exata dos dois blocos** é escolha desta spec; os FRs fixam o conteúdo obrigatório, não as palavras.
- **"Não verifica que a URL resolve"** é interpretação conservadora e alinhada ao que o serviço faz hoje: nenhuma task da cadeia T-005 prevê validação de alcançabilidade, e fazê-la exigiria I/O de rede em requisição de escrita.
- **Formato do parágrafo**: texto corrido dentro do bloco literal `|` já existente, sem sub-headers markdown — consistente com o estilo de `description` do documento.
- Autoridade de validação e workflow herdados da cadeia; `contracts-catalog` reinstalado (`mvn -B -DskipTests install`) antes de o consumidor compilar.
- `TASKS.json` permanece intocado por esta spec, como em toda a cadeia.

## Out of Scope

- **Reescrita de `T-005-3`** — a task diz "Definir a recepção **multipart** do handler de upload de imagens de produto", incompatível com a estratégia ora ratificada. Substituição proposta ao usuário:

  ```json
  {
    "id": "T-005-3",
    "phase": "api",
    "description": "Definir a recepção JSON do handler de registro de imagem de produto (corpo ProductImageRequest com a URL da imagem hospedada externamente), conforme a estratégia URL-only ratificada em T-002-5. Substitui a redação anterior, que previa recepção multipart — incompatível com o contrato: o serviço nunca recebe bytes de imagem",
    "source_reference": "shared-contracts",
    "status": "refined",
    "decomposition_allowed": false,
    "depends_on": ["T-005-2"]
  }
  ```

  Vale reler, na mesma varredura, `T-005-1` ("upload de imagens") e `T-005-2` ("handler POST de upload"): o termo *upload* é enganoso sob URL-only, ainda que o comportamento descrito não conflite. **`TASKS.json` não é editado por esta spec.**

- **Enforcement de HTTPS via `pattern`** — lacuna registrada. Hoje HTTPS é expectativa textual sem verificação. Torná-la verificável é mudança de comportamento (rejeição em runtime de URLs `http://`) e reabriria `ProductImageRequest`, propriedade de `T-002-3`. Entrada proposta ao usuário:

  ```json
  {
    "id": "T-002-8",
    "phase": "contracts",
    "description": "Restringir ProductImageRequest.url a URLs https:// via pattern em contracts-catalog/openapi/catalog.yaml, tornando verificável a expectativa de HTTPS hoje declarada apenas em prosa. Lacuna identificada na spec 010-image-storage-strategy: sem pattern, uma URL http:// é aceita pelo contrato e só falha no navegador, como mixed content",
    "source_reference": "shared-contracts",
    "status": "refined",
    "decomposition_allowed": false,
    "depends_on": ["T-002-5"]
  }
  ```

- Propriedade `images` no schema `Product` (**`T-002-6`**, já no tracker).
- Schema `ProblemDetail` (RFC 7807) nos desfechos de erro (**`T-002-7`**, já no tracker).
- Operação de remoção de imagem — nenhuma task a cobre; a estratégia declara sua semântica sem declarar o endpoint.
- Semântica de URL duplicada — realocada à cadeia T-003 por `T-002-3`.
- Atributo, coluna, DTOs do serviço e mapper (**cadeia T-003**); implementação (**cadeia T-005**).
