# Phase 1 Data Model: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

Nenhuma entidade de dado nova. Mudança apenas na documentação de três desfechos de resposta já existentes, todos na mesma operação.

## Desfecho `400` de `addProductImage` (modificado)

| Aspecto | Antes | Depois |
|---|---|---|
| `description` | "Malformed request body, or url absent, not a valid URI, or too long" | inalterado |
| `content` | ausente | `application/json` → `schema: $ref '#/components/schemas/ProblemDetail'` |

## Desfecho `403` de `addProductImage` (modificado)

| Aspecto | Antes | Depois |
|---|---|---|
| `description` | "Authenticated principal lacks the required admin role" | inalterado |
| `content` | ausente | `application/json` → `schema: $ref '#/components/schemas/ProblemDetail'` |

## Desfecho `404` de `addProductImage` (modificado)

| Aspecto | Antes | Depois |
|---|---|---|
| `description` | "Product not found" | inalterado |
| `content` | ausente | `application/json` → `schema: $ref '#/components/schemas/ProblemDetail'` |

## Schema `ProblemDetail` (pré-existente, inalterado)

Terceira, quarta e quinta referência real no documento (anteriores: `getProductById` 404 — `T-002-7-2`; `getStockItemByProductId` 404 — `T-002-7-3`). Nenhuma propriedade é modificada por esta feature.

## Desfecho `201` de `addProductImage` — inalterado, fora de escopo

Continua referenciando `ProductImageResponse`, sem alteração.

## Relacionamentos

Nenhum. Três referências (`$ref`) independentes, de três desfechos de resposta da mesma operação, ao mesmo schema já existente.
