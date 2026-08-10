# Data Model: Path canônico do endpoint de leitura de saldo de estoque (T-001-1)

**Date**: 2026-07-22 · **Plan**: [plan.md](./plan.md)

Nenhuma entidade de banco é criada ou alterada. O "modelo de dados" desta feature é a estrutura do documento de contrato.

## Entidade 1 — Entrada de Path (nova)

| Aspecto | Valor |
|---|---|
| Localização | `contracts-catalog/openapi/catalog.yaml` → seção `paths` |
| Chave | `/api/v1/products/{productId}/stock` |
| Conteúdo nesta task | Path Item Object com `summary` + `description` apenas (sem operações — R2) |
| Regras de validação | FR-001 (existência), FR-002 (template `{productId}`), FR-005 (unicidade semântica — único path de saldo), FR-003 (aditiva — paths preexistentes intactos), FR-004 (documento parseável — R3) |
| Relacionamentos | Pai lógico do path existente no serviço `.../stock/movements`; endereça o schema `StockItem` (Entidade 2); consumido futuramente por T-001-2..5 (operação/parâmetro/schema/auth) e T-004-x (implementação) |

### Estados / transições

O path não tem máquina de estados; seu ciclo de vida no contrato é:

```
(inexistente) → declarado-sem-operação (T-001-1, esta feature)
             → declarado-com-GET (T-001-2..5)
```

## Entidade 2 — Schema `StockItem` (existente — somente referenciado)

| Aspecto | Valor |
|---|---|
| Localização | `catalog.yaml` → `components/schemas/StockItem` |
| Campos atuais | `productId` (uuid, required), `quantityOnHand` (int64, required), `reorderLevel` (int64), `lastMovementAt` (date-time) |
| Mudança nesta task | **Nenhuma** (FR-003) |
| Observação registrada | Diverge da entidade real do serviço (`available`/`reserved`/`reorderPoint`); reconciliação é escopo de T-001-4 — não tocar aqui |

## Invariantes do documento após a edição

1. Exatamente 2 entradas em `paths`: `/api/v1/products/{id}` (intacta) e `/api/v1/products/{productId}/stock` (nova).
2. `components/schemas` byte-a-byte idêntico ao estado anterior.
3. Primeira declaração do documento: `openapi: 3.1.0` (já garantido — R4).
4. Nomes de parâmetro por path: `{id}` no path de produto (preexistente), `{productId}` na hierarquia de estoque — coexistência válida em OpenAPI (Edge Case do spec).
