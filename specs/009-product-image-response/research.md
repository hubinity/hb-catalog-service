# Phase 0 — Research: Schema de resposta do registro de imagem

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

Entrou na Phase 0 **sem marcadores `NEEDS CLARIFICATION`**: a única incógnita real (forma do corpo) foi decidida pelo usuário; as demais seguem precedente da cadeia ou foram resolvidas por evidência.

---

## R1 — Forma do corpo: schema próprio devolvendo a coleção

**Decision**: `ProductImageResponse` — `{ productId, images[] }`, com a **coleção resultante completa, em ordem**.

**Rationale**: Decisão do usuário (2026-07-25). Três ganhos:

1. **Não depende do schema `Product`**, que não tem `images` e cuja correção nenhuma task possui (ver R5). Um `$ref: Product` descreveria uma resposta que omite justamente a imagem recém-registrada.
2. **Mantém a task no escopo declarado** — "schema de resposta do endpoint", não "reforma do schema de produto".
3. **Confirma o efeito da chamada**: o consumidor vê o estado resultante sem emitir um GET adicional.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| `$ref: Product` | Alinharia com T-005-5, mas exigiria adicionar `images` ao `Product` — schema referenciado por outras operações, trabalho que nenhuma task possui e muito além do escopo desta. |
| Devolver só a URL criada | Mais próximo da semântica clássica de `201`, mas não permite observar a ordem nem confirmar o estado resultante — justamente o que torna a convenção "primeiro = principal" utilizável. |
| `204 No Content` | Descartado em T-002-2, que já fixou `201`; e T-005-5 prevê corpo. |

---

## R2 — Por que a coleção inteira, e não o item criado

**Decision**: a resposta representa a **coleção**, não o recurso criado.

**Rationale**: É incomum para um `201`, e a razão é a mesma que já levou T-002-2 a dispensar o header `Location`: **sob URL-only não existe recurso individual endereçável por imagem**. Não havendo entidade "imagem" com identidade própria, a unidade observável é a coleção do produto. Devolver só a URL enviada seria eco inútil — o cliente acabou de mandá-la.

A `description` do schema declara isso explicitamente, para que um revisor não "corrija" a resposta para o formato convencional.

---

## R3 — Ordenação e imagem principal

**Decision**: `images` vem **em ordem**, e a `description` afirma que o **primeiro elemento é a imagem principal**.

**Rationale**: T-002-3 fixou que `Product.images[]` é lista de strings e que a principal é a primeira, por convenção posicional. Essa convenção seria inútil se a resposta não expusesse a ordem. Declará-la **aqui**, na `description` do schema, evita que o consumidor precise ter lido a spec da task anterior para entendê-la — o contrato passa a ser autossuficiente nesse ponto.

---

## R4 — Restrições declaradas e omitidas

**Decision**: itens com `format: uri` e `maxLength: 2048`; **sem** `minItems`; **sem** `additionalProperties: false`.

**Rationale**:

- **`maxLength: 2048`** — simetria com `ProductImageRequest.url` (T-002-3) e convenção do documento de não deixar string sem teto. Numa resposta a restrição é documentacional: descreve o que o servidor produz, e é verdadeira por construção, já que a entrada limita a URL. **Registrado como adição desta spec**: não constava do esboço aprovado pelo usuário, e é removível sem afetar nenhum outro requisito.
- **Sem `minItems`** — seria verdade que a coleção tem ≥ 1 elemento após um `201`, mas isso é afirmação sobre o **comportamento** do serviço, não sobre a **forma** do documento. Declará-la aqui prometeria garantia que só a cadeia T-005 pode cumprir.
- **Sem `additionalProperties: false`** — mantém a postura de leitor tolerante adotada em T-002-3, coerente em todo o documento.

---

## R5 — Lacuna: schema `Product` sem `images`

**Decision**: registrar com entrada proposta (`T-002-6`); **não** resolver aqui.

**Rationale**: Varredura das 33 tasks do tracker: nenhuma adiciona `images` ao schema `Product` do contrato. A cadeia T-003 cobre só o lado do serviço — entidade, migração, DTOs `ProductRequest`/`ProductResponse` e mapper.

**Impacto se ignorada**: após T-003-4 o serviço devolverá imagens em `ProductResponse`, mas o contrato descreverá um `Product` sem elas. A divergência apareceria tarde, na cadeia T-005.

**Por que não resolver aqui**: exigiria alterar um schema referenciado por outras operações — escopo alheio a "schema de resposta do endpoint". A escolha de R1 (schema próprio) **evita depender** da lacuna, então esta task não a agrava.

---

## R6 — Lacuna: corpos de erro sem `ProblemDetail`

**Decision**: registrar com entrada proposta (`T-002-7`); **não** resolver aqui.

**Rationale**: O Princípio I da constituição exige que toda resposta de erro do serviço seja RFC 7807 `ProblemDetail`, via `ApiExceptionHandler`. O contrato, porém, declara `400`/`403`/`404` apenas com `description` — e nenhuma task modela `ProblemDetail`.

**Não é violação constitucional**: o princípio governa o que o **serviço responde**, e isso não muda aqui — o serviço cumprirá. O que existe é **documentação incompleta** no contrato.

**Por que não resolver aqui**: alcança **todas** as operações do documento, não só a de imagens; embutir isso numa task de "schema de resposta do endpoint" seria absorver escopo alheio. Daí a entrada proposta vir com `decomposition_allowed: true`, ao contrário da `T-002-6`.

---

## R7 — `description` do `201` e do Path Item permanecem intocadas

**Decision**: nenhuma reescrita; mudança estritamente aditiva.

**Rationale**:

- A `description` do `201` registra a ausência de `Location`, que continua verdadeira. O `content` é acrescentado **abaixo** dela.
- A `description` do Path Item diz que corpo de requisição e de resposta "are completed by T-002-3 and T-002-4" — proveniência verdadeira antes e depois. Pela regra fixada em T-002-3, a limpeza final cabe à **última** task da cadeia (T-002-5).

**Consequência**: o critério de diff volta a ser o de T-002-1 e T-002-3 — **zero remoções**.

---

## R8 — Gates reproduzidos, não herdados

**Decision**: capturar de novo o inventário de DTOs (com checksum) e medir de novo o baseline de testes, ambos **antes** da edição.

**Rationale**: T-002-3 introduziu esses dois gates. Como `target/` é regenerado a cada build e a contagem de testes pode ter mudado desde então, reaproveitar números da execução anterior tornaria as comparações sem valor. O gate só prova algo se a captura for **desta** execução.

---

## Resumo das decisões

| # | Decisão | Origem |
|---|---|---|
| R1 | `ProductImageResponse` próprio, com a coleção resultante | Usuário, 2026-07-25 |
| R2 | Coleção e não item criado — mesma razão da ausência de `Location` | Análise desta spec |
| R3 | Ordem declarada; primeiro = principal, afirmado no contrato | Convenção de T-002-3 |
| R4 | `maxLength` sim; `minItems` e `additionalProperties` não | Simetria + separação forma/comportamento |
| R5 | Lacuna do `Product` registrada — proposta `T-002-6` | Varredura do tracker |
| R6 | Lacuna do `ProblemDetail` registrada — proposta `T-002-7` | Princípio I |
| R7 | Nenhuma `description` reescrita ⇒ zero remoções | Regra de T-002-3 |
| R8 | Ambos os gates recapturados nesta execução | Validade da comparação |

**Nenhum `NEEDS CLARIFICATION` remanescente.** Phase 0 completa.
