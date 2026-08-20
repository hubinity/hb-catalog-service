# Phase 0 — Research: Schema ProblemDetail (RFC 7807)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-26

Entrou na Phase 0 **sem marcadores `NEEDS CLARIFICATION`**: a forma do schema é transcrição de um comportamento já implementado (`ApiExceptionHandler`), não uma decisão de produto em aberto.

---

## R1 — Forma do schema: os cinco membros canônicos da RFC 7807

**Decision**: `ProblemDetail` com `type` (uri), `title` (string), `status` (integer), `detail` (string), `instance` (uri) — nenhum obrigatório.

**Rationale**: RFC 7807 define exatamente esses cinco membros, todos OPTIONAL. Verificação em código confirma que é isso que o serviço já emite: `com.hubinity.catalog.api.error.ApiExceptionHandler` usa exclusivamente `org.springframework.http.ProblemDetail` (nunca um DTO próprio), chamando `ProblemDetail.forStatusAndDetail(status, detail)` e, na maioria dos handlers, `setTitle(...)` e `setType(URI.create("urn:hubinity:catalog:..."))`. `instance` nunca é setado explicitamente hoje — reforça por que não pode ser `required`.

**Alternatives considered**:

| Alternativa | Rejeitada porque |
|---|---|
| Tornar `type`/`title`/`status` obrigatórios (são os mais usados) | Nenhum handler garante os cinco uniformemente; `instance` nunca aparece. Declarar `required` prometeria uma garantia que o serviço não cumpre. |
| Schema fechado (`additionalProperties: false`) | Quebraria a propriedade de extensão `errors` que `MethodArgumentNotValidException` já emite hoje (ver R3). |
| Modelar por exceção (um schema por tipo de erro) | Excede o escopo desta task — o objetivo é o `ProblemDetail` genérico; especializações ficariam fora de escopo mesmo se decompostas depois. |

---

## R2 — `type` como URI com `about:blank` como padrão implícito

**Decision**: `type: string, format: uri`, com `description` mencionando o comportamento padrão.

**Rationale**: `ProblemDetail.forStatusAndDetail` do Spring usa `about:blank` quando nenhum tipo mais específico é setado — comportamento padrão da RFC 7807 quando o membro `type` está ausente. A maioria dos handlers do serviço **sobrescreve** isso com uma URN própria (`urn:hubinity:catalog:product-not-found`, etc.) via `setType`. Documentar esse comportamento no `description` evita que um consumidor futuro trate `about:blank` como valor anômalo.

---

## R3 — Propriedade de extensão `errors` (validação) fica fora de escopo

**Decision**: não modelar `errors`; permitir implicitamente via `additionalProperties` não declarado.

**Rationale**: `ApiExceptionHandler.handleMethodArgumentNotValid` chama `problem.setProperty("errors", Map<String,String>)` — uma extensão RFC 7807 válida (a spec permite membros de extensão), mas específica de um único tipo de falha (validação de campo). Incluí-la no `ProblemDetail` genérico:

- Implicaria que todo erro tem `errors`, o que é falso para a maioria (ex.: `404 Product not found` não tem).
- Exigiria decidir a forma de um schema especializado (`ValidationProblemDetail`?) — decisão de produto fora do escopo de "declarar o schema ProblemDetail".

Manter o schema sem `additionalProperties: false` já permite essa e outras extensões futuras sem exigir modelagem explícita agora.

---

## R4 — `status` como `integer`, não como enum

**Decision**: `status: integer, format: int32`, sem restrição de valores.

**Rationale**: O catálogo já usa múltiplos códigos de erro (`400, 403, 404, 409, 422`, e potencialmente outros no futuro). Um enum acoplaria o schema genérico à lista atual; a RFC 7807 também não restringe `status` a um conjunto fechado — é "o código HTTP gerado pelo servidor de origem".

---

## R5 — Este schema não referencia nenhuma operação (escopo isolado)

**Decision**: apenas declarar em `components/schemas`; não tocar em nenhum `content` de desfecho de erro.

**Rationale**: `T-002-7` (`decomposition_allowed: true`) foi decomposta em seis subtarefas justamente para separar "declarar a forma" (`T-002-7-1`, esta) de "referenciar em cada desfecho" (`T-002-7-2` a `T-002-7-6`, uma por par operação/status). Resolver os dois de uma vez reintroduziria o problema original de `T-002-7` — uma task com múltiplos verbos.

**Consequência verificada**: um schema não-referenciado por nenhuma operação continua sendo OpenAPI 3.1 válido, e `openapi-generator-maven-plugin` com `generatorName: spring` (sem `modelsToGenerate` restritivo) gera uma classe por entrada em `components/schemas` independentemente de referência — confirmado por leitura de `contracts-catalog/pom.xml`. Logo, o DTO `ProblemDetail.java` é produzido **nesta** task, não posterior.

---

## R6 — Gates reproduzidos, não herdados

**Decision**: capturar de novo o inventário de DTOs (com checksum) e medir de novo o baseline de testes, ambos **antes** da edição, com nome de arquivo próprio desta feature.

**Rationale**: Mesmo critério fixado em `T-002-3`/`T-002-4`: `target/` é regenerado a cada build e a contagem de testes pode ter mudado desde a última spec. Reaproveitar números de `008`/`009`/`011` invalidaria a comparação.

---

## Resumo das decisões

| # | Decisão | Origem |
|---|---|---|
| R1 | Cinco membros canônicos, nenhum obrigatório | RFC 7807 + `ApiExceptionHandler.java` |
| R2 | `type` como URI; `description` documenta o padrão `about:blank` | Comportamento verificado do Spring `ProblemDetail` |
| R3 | `errors` (extensão de validação) fora de escopo; schema permanece aberto | Especialização é decisão de produto separada |
| R4 | `status` como `integer` livre, sem enum | Múltiplos códigos já em uso; RFC não restringe |
| R5 | Schema isolado, sem referenciar nenhuma operação | Decomposição de `T-002-7` em seis subtarefas de verbo único |
| R6 | Ambos os gates recapturados nesta execução | Validade da comparação |

**Nenhum `NEEDS CLARIFICATION` remanescente.** Phase 0 completa.
