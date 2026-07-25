# Phase 0 — Research: Operação POST de registro de imagem de produto

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

A spec entrou na Phase 0 **sem marcadores `NEEDS CLARIFICATION`**: as duas incógnitas que não tinham default defensável (código de sucesso; como expressar a role) foram decididas pelo usuário antes da especificação, e a terceira (idempotência) foi resolvida por evidência de código.

---

## R1 — Código de sucesso: `201 Created`

**Decision**: `201`, **sem** header `Location`.

**Rationale**: Decisão do usuário (2026-07-25). `201` é a convenção canônica de POST em coleção que cria um subrecurso. A ausência de `Location` não é descuido: sob a estratégia URL-only não existe recurso individual endereçável por imagem — a imagem vive fora do sistema e o catálogo guarda apenas sua URL. Inventar um `Location` para satisfazer a convenção apontaria para um endereço inexistente, o que é pior que omiti-lo. A `description` do `201` registra a ausência e a razão.

**Tensão examinada e resolvida**: T-005-5 (tracker) devolve o `ProductResponse` atualizado, o que à primeira vista soa como "200 OK" em vez de "201 Created". Não há conflito: uma resposta `201` **pode** carregar representação, e nada na especificação HTTP exige que essa representação seja o recurso criado em isolamento. A amarração formal do corpo é de T-002-4.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| `200 OK` | Alinharia trivialmente com "devolve o produto atualizado", mas apaga a informação de que **algo foi criado**. Sob POST em coleção, `201` carrega mais significado sem custo. |
| `201` **com** `Location` | Exigiria uma URI por imagem que a estratégia URL-only não produz. Apontaria para endereço inexistente. |
| `204 No Content` | Incompatível com T-005-5, que devolve corpo. |

---

## R2 — Como expressar a exigência de role `admin`

**Decision**: declarar o desfecho `403` **e** afirmar o requisito em prosa na `description` da operação.

**Rationale**: Decisão do usuário (2026-07-25). O contrato **não consegue** expressar "exige role admin" de forma machine-readable, e a razão é estrutural, não preguiça:

- O esquema de segurança do documento é `bearerAuth`, `type: http` / `scheme: bearer`. Em OpenAPI, **scopes só existem para `type: oauth2` e `openIdConnect`** — um esquema `http`/`bearer` não tem onde declará-los.
- Mesmo que houvesse scopes, seria a modelagem errada: as roles do Keycloak chegam por `realm_access.roles` (extraídas por `KeycloakRealmRoleConverter`), **não** por scopes OAuth. Declarar scopes descreveria um mecanismo que não é o usado.

Restam duas coisas que o contrato *pode* fazer, e ambas são feitas: declarar o **desfecho** (`403`) e afirmar o requisito em **prosa**. A aplicação é e continua sendo server-side (`@PreAuthorize`, T-005-2).

**Encerra a pendência do Princípio VI** deferida por T-002-1.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| Migrar `bearerAuth` para `oauth2` com scopes | Descreveria um mecanismo que o serviço não usa (roles ≠ scopes); mudaria o esquema de segurança de **todas** as operações; muito além do escopo de T-002-2. |
| Declarar só `403`, sem prosa | Sinaliza que existe negação por privilégio mas não diz **qual** privilégio — o consumidor continua adivinhando. |
| Não declarar nada, deferir a T-005 | A cadeia T-002 **não tem** task de autorização (diferente de T-001, que tinha a T-001-5). A pendência simplesmente se perderia. |

---

## R3 — Idempotência

**Decision**: a operação **não** exige `Idempotency-Key`; nenhum parâmetro de header é declarado.

**Rationale**: Decisão por evidência de código, registrada como consciente:

- `IdempotencyFilter.java` protege **exatamente 4 paths**: `/api/v1/products/*/stock/movements`, `/api/v1/stock/reservations`, `/api/v1/stock/reservations/*/release`, `/api/v1/stock/reservations/*/commit`.
- `ProductController.java` tem `@PostMapping`, `@PutMapping` e `@DeleteMapping` e **nenhum** deles exige a chave.
- O Princípio V redige a obrigação como sendo dos "POSTs mutantes **de estoque**". Esta é uma mutação de **produto**.

Exigir a chave aqui seria inconsistente com todas as demais mutações de produto **e** dependeria de alterar o array `PROTECTED` do filtro — mudança de serviço que nenhuma task da cadeia agendou.

**Encerra a pendência do Princípio V** deferida por T-002-1.

**Alternatives considered**: exigir a chave e agendar a alteração do filtro — rejeitado: inventaria trabalho de serviço não pedido, e tornaria o registro de imagem mais rígido que a criação e a exclusão de produtos, o que não se justifica pelo risco.

---

## R4 — Escopo dos desfechos declarados

**Decision**: declarar `201`, `403`, `404`. **Não** declarar `400`, `401` nem `409`.

**Rationale**:

- **`404`** espelha o desfecho único de T-001-2 (produto inexistente).
- **`400`** depende do formato do corpo, que é T-002-3. Declarar o desfecho antes do corpo inverteria a ordem da cadeia.
- **`401`** não é declarado porque T-001-2 não o declarou para o GET, mesmo com autenticação exigida. Introduzi-lo agora criaria assimetria entre operações; padronizar `401` em todo o documento é mudança de convenção própria, não desta task.
- **`409`** (URL duplicada) depende de "o que conta como duplicata", indefinível antes de T-002-3 fixar se o corpo carrega uma URL ou uma lista.

**Alternatives considered**: declarar o conjunto completo de desfechos agora, para poupar edições futuras — rejeitado: três dos quatro dependem de decisões que ainda não foram tomadas, e fixá-los aqui equivaleria a decidir por T-002-3.

---

## R5 — Reescrita da `description` do Path Item

**Decision**: substituir a `description`, tornando a mudança **não estritamente aditiva** — ao contrário de T-002-1.

**Rationale**: O texto atual afirma *"Operations on this path are declared by the remaining T-002 tasks."* Assim que o `post` for declarado, a frase passa a ser **falsa**. Manter uma afirmação falsa no contrato para preservar a pureza do diff seria trocar correção por conveniência. Precedente direto: T-001-5 reescreveu a `description` do Path Item de estoque ao fechar aquela cadeia.

O risco de uma mudança não-aditiva é o escorregão — reformatar ou "melhorar" elementos vizinhos de passagem. Mitigação em FR-014: o texto anterior e o substituto estão **ambos fixados na spec**, e o critério de diff exige que toda linha removida esteja contida no bloco `description:` do Path Item de imagens.

**Alternatives considered**: manter a `description` intacta e aceitar a frase falsa — rejeitado (correção > pureza de diff); reescrever só no fim da cadeia, em T-002-5 — rejeitado: deixaria o documento mentindo durante três tasks.

---

## Resumo das decisões

| # | Decisão | Origem |
|---|---|---|
| R1 | `201 Created`, sem `Location` | Usuário, 2026-07-25 |
| R2 | Role `admin` via `403` + prosa (limitação estrutural do OpenAPI) | Usuário, 2026-07-25 — encerra Princípio VI |
| R3 | Sem `Idempotency-Key` | Evidência de código — encerra Princípio V |
| R4 | Desfechos `201`/`403`/`404`; sem `400`/`401`/`409` | Precedente T-001-2 + ordem da cadeia |
| R5 | Reescrever a `description` do Path Item (mudança não-aditiva delimitada) | Precedente T-001-5 |

**Nenhum `NEEDS CLARIFICATION` remanescente.** Phase 0 completa.
