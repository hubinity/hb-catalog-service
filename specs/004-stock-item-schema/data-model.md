# Data Model: Schema de resposta da operação de leitura de saldo (T-001-4)

**Date**: 2026-07-23 · **Plan**: [plan.md](./plan.md)

Nenhuma entidade de banco. O "modelo de dados" é a estrutura do documento de contrato e sua relação com o record real do serviço.

## Entidade 1 — Schema `StockItem` (existente — reescrito)

| Campo (novo) | Tipo OpenAPI | Origem (record do serviço) | Required |
|---|---|---|---|
| `productId` | string/uuid | `UUID productId` | ✅ |
| `available` | integer/int32, min 0 | `Integer available` | ✅ |
| `reserved` | integer/int32, min 0 | `Integer reserved` | ✅ |
| `reorderPoint` | integer/int32, min 0 | `Integer reorderPoint` | ✅ |
| `updatedAt` | string/date-time | `Instant updatedAt` | — (nulo antes da 1ª mudança) |

Campos **removidos** (especulativos, nunca consumidos): `quantityOnHand` (int64), `reorderLevel` (int64), `lastMovementAt` (date-time).

## Entidade 2 — Resposta `'200'` (existente — completada)

| Aspecto | Valor |
|---|---|
| Mudança | + `content: application/json` com `$ref: '#/components/schemas/StockItem'` |
| Intacto | `description: Current stock balance for the product` (feature 002) |

## Entidade 3 — Path Item do saldo (existente — description atualizada)

Pendência restante declarada: autorização (T-001-5). `summary`, operação, parâmetro e `'404'` intactos.

## Entidade 4 — Record `StockItemResponse.java` (serviço — fonte de verdade, intocado)

`hb-catalog-service/src/main/java/com/hubinity/catalog/api/dto/StockItemResponse.java` — projeção pública real; a convergência DTO gerado × record local é decisão de T-004-x.

## Invariantes do documento após a edição

1. `components/schemas` com os mesmos 4 schemas (`Product`, `Category`, `StockItem`, `StockMovement`) — nenhum adicionado/removido; só `StockItem` reescrito.
2. 0 ocorrências de `quantityOnHand`, `reorderLevel`, `lastMovementAt` no documento (FR-003).
3. Diff restrito a: bloco do schema `StockItem`, bloco `'200'` da operação de saldo, description do Path Item (FR-005).
4. `Product`, `Category`, `StockMovement`, paths de produto, tags e parâmetro byte-a-byte intactos.
5. Documento parseável; DTO `StockItem` regenerado com os 5 campos novos (SC-002).

## Estado da cadeia T-001-x após esta entrega

```
com-parâmetro-completo (T-001-3, done)
  → com-corpo-de-resposta (T-001-4, esta feature)   ← encerra divergência da feature 001 + achado L2
  → com-autorização (T-001-5)                        ← última pendência estrutural
```
