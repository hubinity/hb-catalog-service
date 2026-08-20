# Phase 1 Data Model: Referenciar ProblemDetail no 404 de getStockItemByProductId

Nenhuma entidade de dado nova. Mudança apenas na documentação de um desfecho de resposta já existente.

## Desfecho `404` de `getStockItemByProductId` (modificado)

| Aspecto | Antes | Depois |
|---|---|---|
| `description` | "Stock balance not found for the given product (unknown product or no stock record)" | inalterado |
| `content` | ausente | `application/json` → `schema: $ref '#/components/schemas/ProblemDetail'` |

## Schema `ProblemDetail` (pré-existente, inalterado)

Segunda referência real no documento (primeira: `getProductById`, `T-002-7-2`). Nenhuma propriedade é modificada por esta task.

## Relacionamentos

Nenhum. Referência (`$ref`) de um desfecho de resposta a um schema já existente.
