# Phase 0 — Research: Path do endpoint de imagens de produto

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

A spec entrou na Phase 0 **sem marcadores `NEEDS CLARIFICATION`** — a incógnita dominante (estratégia de armazenamento) foi resolvida por decisão do usuário antes da especificação. As pesquisas abaixo cobrem as decisões estruturais restantes, todas verificadas contra o código real.

---

## R1 — Estratégia de armazenamento de imagens

**Decision**: *URL-only reference*. A API registra apenas URLs de imagens hospedadas externamente; nunca recebe bytes, nunca faz upload multipart.

**Rationale**: Decisão explícita do usuário (2026-07-25). O `PRD-HUBINITY.md` sustenta a *existência* de imagens (`Product … images[]`, linha 319) mas coloca a entrega otimizada via CDN — "Cloudflare R2 ou S3 + CloudFront" — sob **`## 12. Melhorias no MVP`** (linha 1044), isto é, explicitamente fora do MVP. Adotar URL-only mantém o MVP entregável sem antecipar infraestrutura, credenciais e configuração de perfis (`staging`/`prod`) que o PRD ainda não pede, e sem disparar a exigência de ADR das Technology Constraints.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| Object storage (Cloudflare R2 / S3) com upload multipart | Antecipa item pós-MVP (PRD §12); introduz dependência de infraestrutura + credenciais em todos os perfis, exigindo ADR; amplia muito o escopo de uma cadeia de contrato. |
| Bytes em Postgres (`bytea` / large object) | Sem infraestrutura nova, mas incha o banco, complica backup/restore e é mau ponto de partida para entrega via CDN — exatamente o destino declarado no PRD §12. |

**Consequência para a cadeia (registrada)**: **T-002-3 fica invalidada como redigida** — ela especifica formato de requisição *multipart*, que sob URL-only não existe. Precisa ser reescrita ou descartada. `TASKS.json` foi deixado intacto de propósito.

---

## R2 — Endereço do path

**Decision**: `/api/v1/products/{productId}/images` — sub-recurso de produto, orientado a recurso e no plural.

**Rationale**: Espelha o único precedente análogo no documento, `/api/v1/products/{productId}/stock` (sub-recurso de produto). O plural `images` é fiel a `images[]` (array) no PRD linha 319. Nomear pelo **recurso** (`/images`) e não pela **ação** (`/upload`) mantém o estilo do documento e — decisivo aqui — permanece correto se a estratégia de armazenamento mudar no futuro: sob URL-only "upload" seria simplesmente falso, já que nenhum byte trafega.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| `/api/v1/products/{productId}/images/upload` | Orientado a ação; nenhum precedente no documento; sob URL-only o nome mente sobre a semântica. |
| `/api/v1/product-images` (recurso de topo) | Perde o aninhamento sob o dono; obriga a carregar `productId` no corpo; diverge do precedente `/stock`. |
| Sem endpoint — gerir `images[]` pelo `PUT /api/v1/products/{id}` | Defensável sob URL-only (é só um campo do produto) e mais simples, **mas** contraria a task T-002-1, que pede explicitamente um path de imagens, e a cadeia T-002 inteira pressupõe um endpoint próprio. Registrada aqui como a alternativa mais séria; a decisão do usuário de seguir a cadeia prevalece. |

---

## R3 — Nome do parâmetro de path

**Decision**: `{productId}`, declarado no **nível do Path Item**.

**Rationale**: O documento é **internamente inconsistente** — `/api/v1/products/{id}` usa `{id}`; `/api/v1/products/{productId}/stock` usa `{productId}`. Não há convenção única a seguir, então o critério é o **caso análogo**: sub-recursos de produto usam `{productId}`, e é isso que este path é. Declarar no nível do Path Item (e não por operação) faz o parâmetro ser herdado por qualquer operação futura, evitando divergência entre operações do mesmo path e reduzindo o trabalho de T-002-2.

**Alternatives considered**: `{id}` (alinharia ao recurso direto, mas não ao caso análogo, e produziria `/products/{id}/images` ao lado de `/products/{productId}/stock` — a pior das duas inconsistências); declarar por operação (rejeitado: repetição e risco de divergência).

**Não corrigido**: a divergência preexistente em `/api/v1/products/{id}` **não é tocada**. Renomear um parâmetro de path é mudança quebrante para consumidores e está fora da cadeia T-002.

---

## R4 — Requisito de segurança

**Decision**: não declarar nada. O Path Item herda `security: [ { bearerAuth: [] } ]` do nível raiz.

**Rationale**: T-001-5 já instalou o requisito global; qualquer path novo o herda automaticamente. Redeclarar seria redundante e criaria risco de divergir do documento.

**Tensão registrada**: o Princípio VI da constituição exige `@PreAuthorize("hasRole('admin')")` em endpoints de **mutação**, e as operações futuras deste path serão mutações. O contrato modela hoje apenas *autenticação*, seguindo o precedente de T-001-5 (que deferiu explicitamente o reforço de role). Um Path Item não é o lugar para expressar role — a obrigação recai sobre **T-002-2** (contrato da operação) e a **cadeia T-005** (implementação). Deferida com destinatário nomeado, não silenciada.

---

## R5 — Validade de um Path Item sem operações

**Decision**: aceitável e transitório; o Path Item existirá sem operações entre esta task e T-002-2.

**Rationale**: Em OpenAPI 3.1 todos os campos de operação de um Path Item são opcionais, portanto um Path Item apenas com `summary`/`description`/`parameters` é válido. Verificação decisiva no toolchain real: o pom pai configura `generateApis=false` e `generateModels=true` (linhas 114-115) — o gerador produz **somente modelos a partir de schemas**. Como esta task não adiciona nenhum schema, **nenhuma classe Java é gerada ou alterada**, e o build não tem como quebrar por ausência de operação. É o mesmo padrão já exercido pela cadeia T-001, que declarou o path antes da operação.

**Alternatives considered**: declarar a operação POST junto, para não deixar estado transitório — rejeitado: viola a fronteira de escopo da task (`decomposition_allowed: false`) e invadiria T-002-2.

---

## Resumo das decisões

| # | Decisão | Origem |
|---|---|---|
| R1 | Armazenamento URL-only (sem bytes, sem multipart) | Usuário, 2026-07-25 |
| R2 | `/api/v1/products/{productId}/images` (recurso, plural, sub-recurso) | Precedente `/stock` + PRD `images[]` |
| R3 | Parâmetro `{productId}` no nível do Path Item | Caso análogo (`/stock`); documento é ambíguo |
| R4 | Sem `security` próprio — herda `bearerAuth` global | T-001-5 |
| R5 | Path Item sem operações é válido e não gera código | OpenAPI 3.1 + `generateApis=false` verificado |

**Nenhum `NEEDS CLARIFICATION` remanescente.** Phase 0 completa.
