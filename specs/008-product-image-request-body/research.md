# Phase 0 — Research: Corpo de requisição JSON do registro de imagem

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

Entrou na Phase 0 **sem marcadores `NEEDS CLARIFICATION`**: as duas incógnitas sem default defensável foram decididas pelo usuário; as demais, por evidência.

---

## R1 — Forma do corpo: só a URL

**Decision**: `{ "url": "…" }` — objeto com uma única propriedade obrigatória, sem metadados.

**Rationale**: Decisão do usuário (2026-07-25). O PRD (linha 319) diz apenas `images[]`, sem estrutura de elemento. Introduzir `altText` ou flag de imagem principal seria criar requisito que ninguém pediu e encarecer três tasks a jusante (entidade, migração, mapper).

**Consequência determinante para a cadeia T-003**: `Product.images[]` é **lista de strings**, logo a coluna é `text[]` — não `jsonb`. Imagem principal = primeiro elemento, por convenção posicional.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| `{url, altText, primary}` | Ganha acessibilidade e imagem principal explícita, mas antecipa requisitos ausentes do PRD e força `jsonb` em T-003-2, com mapper e migração mais caros. |
| String nua no corpo (`"https://…"`) | Um corpo JSON escalar é legal mas incomum, impede evolução aditiva (não há onde acrescentar campo) e destoa de todo DTO `*Request` do serviço, que são objetos. |

---

## R2 — Rigor da validação da URL

**Decision**: `type: string`, `format: uri`, `minLength: 1`, `maxLength: 2048`. HTTPS afirmado **em prosa**, sem `pattern`.

**Rationale**: Decisão do usuário (2026-07-25). Segue a convenção do documento — nenhuma string existente fica sem teto (`sku` 1–64, `name` 1–200, categoria 1–120). Evita regex, que em contrato é rígida e difícil de afrouxar, e que bloquearia cenários futuros de CDN (PRD §12).

**Consequência declarada**: o contrato **não rejeita** `http://`. A prosa nomeia a razão concreta (mixed content bloqueado por navegadores em página HTTPS) para que a escolha não pareça descuido, e a rejeição efetiva, se desejada, fica com o serviço (T-005).

**Origem dos números** — nenhum é arbitrário:
- **2048**: menor teto historicamente praticado por navegadores e CDNs; abaixo dele nenhum cliente relevante trunca. Nenhuma RFC impõe limite a URIs, então o número é convenção prática — e serve de base explícita ao dimensionamento da coluna em T-003-2.
- **1**: impede string vazia, espelhando `minLength: 1` de `sku` e dos `name`.

**Alternatives considered**: `pattern: ^https://` (rejeitado: rígido, bloqueia CDN futuro, e o contrato não é o ponto de aplicação); só `format: uri` sem teto (rejeitado: única string do documento sem limite, e deixaria T-003-2 sem base para dimensionar a coluna).

---

## R3 — Uma imagem por requisição, não lote

**Decision**: o corpo descreve **uma** referência.

**Rationale**: Decisão por evidência. O texto já commitado em `854c02f` diz *"Records **the URL** of an externally hosted image"* e *"Register **an image reference**"* — singular em ambos. Aceitar lote exigiria reescrever contrato entregue, e esvaziaria o `201 Created` (criação de quê, se são várias?).

**Alternatives considered**: aceitar `{ "urls": [...] }` — rejeitado pelo conflito com texto commitado; registrar várias imagens é obtenível por chamadas repetidas, sem custo relevante para o volume esperado de um catálogo.

---

## R4 — Schema nomeado, não inline

**Decision**: `ProductImageRequest` em `components/schemas`, referenciado por `$ref`; anexado após `StockMovement`.

**Rationale**: Todo corpo do documento usa `$ref` — não há um único schema inline. O serviço já nomeia entradas com sufixo `Request` (`CategoryRequest`, `ProductRequest`, `StockMovementRequest`, `StockReservationRequest`). A ordem das chaves não tem significado semântico em OpenAPI e a lista existente é de inserção, não alfabética; anexar minimiza o diff.

**Consequência assumida**: gera `ProductImageRequest.java`. É desejável — um DTO nomeado é justamente o que o consumidor precisa importar em T-005.

**Alternatives considered**: schema inline no `requestBody` — rejeitado: destoa do documento e faria o gerador produzir um modelo de nome sintético (`InlineObject`-like), pior para o consumidor.

---

## R5 — Propriedades desconhecidas: leitor tolerante

**Decision**: **não** declarar `additionalProperties: false`.

**Rationale**: Um cliente que envie campo extra tem o campo ignorado, não a requisição rejeitada. Escolhido para que a eventual introdução de metadados (se T-003 os quiser) seja mudança **aditiva** que não quebra clientes existentes.

**Contrapartida aceita e declarada**: erro de digitação em nome de campo passa despercebido — enviar `{ "ur1": … }` produz `400` (porque `url` é obrigatória e está ausente), mas enviar `{ "url": …, "altTxt": … }` produz `201` com o campo descartado em silêncio.

**Alternatives considered**: `additionalProperties: false` — dá semântica de erro mais nítida, mas torna qualquer extensão futura uma quebra para clientes que já enviem o campo, e é postura mais rígida do que qualquer outro ponto do documento pratica.

---

## R6 — Desfecho `400`: obrigação herdada, encerrada aqui

**Decision**: declarar `'400'`, description-only, nomeando as causas.

**Rationale**: A spec 007 (T-002-2) **deferiu explicitamente** este desfecho a esta task, por ele depender da existência de um formato de requisição. Com o formato definido, a obrigação vence e é cumprida — não repassada.

Mantém-se a convenção description-only da cadeia: corpos de resposta são de T-002-4.

---

## R7 — `409`/URL duplicada: realocada, não redeferida

**Decision**: **não** declarar `409`; a semântica de duplicata passa à cadeia **T-003**.

**Rationale**: Distinção que importa, para não parecer deferimento circular:

- **Motivo da 007** para adiar: "não dá para saber o que é duplicata antes de saber se o corpo carrega uma URL ou uma lista".
- **Esse motivo foi resolvido** aqui (uma URL). O que restou é outra pergunta: *é erro registrar de novo uma URL já presente na coleção?* Isso não é propriedade do **payload**, e sim do **estado da coleção** — pertence a quem define o atributo, a coluna e uma eventual restrição de unicidade, isto é, T-003.

Mudaram o motivo **e** o destinatário; não é a mesma pergunta empurrada adiante.

**Alternatives considered**: declarar `409` agora — imporia restrição de unicidade que T-003 teria de implementar sem que ninguém a tenha pedido; sob `text[]` sem constraint, duplicatas simplesmente se acumulam.

---

## R8 — `description` do Path Item não é reescrita

**Decision**: manter intacta; a mudança é estritamente aditiva.

**Rationale**: A frase diz que corpo de requisição e de resposta "are completed by T-002-3 and T-002-4" — declaração de **proveniência**, que continua verdadeira após esta entrega (T-002-4 segue pendente). Diferente de T-002-2, onde a frase se tornava **falsa** e por isso teve de ser trocada.

Consequência boa: o critério de diff volta ao de T-002-1 — **zero remoções**.

**Alternatives considered**: atualizar a cada task da cadeia — rejeitado: gera churn e reescreve a mesma linha três vezes; a limpeza final cabe à última task, como T-001-5 fez para a sua cadeia.

---

## Resumo das decisões

| # | Decisão | Origem |
|---|---|---|
| R1 | Corpo com apenas `url` ⇒ `images[]` lista de strings ⇒ coluna `text[]` | Usuário, 2026-07-25 |
| R2 | `format: uri` + 1–2048, HTTPS em prosa, sem `pattern` | Usuário, 2026-07-25 |
| R3 | Uma imagem por requisição | Texto commitado em `854c02f` |
| R4 | `ProductImageRequest` nomeado, anexado ao fim de `components/schemas` | Convenção do documento + `api/dto/` |
| R5 | Leitor tolerante (sem `additionalProperties: false`) | Compatibilidade futura |
| R6 | `400` declarado — obrigação herdada **encerrada** | Deferimento de T-002-2 |
| R7 | Sem `409` — questão **realocada** a T-003 (novo motivo, novo destinatário) | Análise desta spec |
| R8 | `description` do Path Item intocada ⇒ mudança aditiva | Frase segue verdadeira |

**Nenhum `NEEDS CLARIFICATION` remanescente.** Phase 0 completa.
