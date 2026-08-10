# Data Model: Especificação fina do parâmetro productId (T-001-3)

**Date**: 2026-07-23 · **Plan**: [plan.md](./plan.md)

Nenhuma entidade de banco. O "modelo de dados" é a estrutura do documento de contrato.

## Entidade 1 — Parâmetro `productId` (existente — completado)

| Aspecto | Valor |
|---|---|
| Localização | Operação `getStockItemByProductId` → `parameters[0]` |
| Estado atual | `name: productId`, `in: path`, `required: true`, `schema (string, uuid)` — bloco da contingência FR-006/feature 002 |
| Mudança | + `description: Product UUID` (inserida entre `required` e `schema`, espelhando a ordem dos campos em `getProductById`) |
| Regras | FR-001 (paridade 5 campos), FR-002 (ratificação com evidência no commit), FR-005 (nenhum campo além da convenção) |

## Entidade 2 — Path Item do saldo (existente — description atualizada)

| Aspecto | Valor |
|---|---|
| Mudança | `description` reescrita para o texto fixado em FR-003 (operação + parâmetro definidos; pendem T-001-4/5); `summary` e operação intactos |

## Entidade 3 — Parâmetro de `getProductById` (existente — intocado)

Referência de paridade: `name: id`, `in: path`, `required: true`, `description: Product UUID`, `schema (string, uuid)`.

## Invariantes do documento após a edição

1. Parâmetro da operação de saldo com exatamente 5 campos, idênticos aos de `getProductById` exceto `name`.
2. Diff total: +1 linha (`description: Product UUID`) e o bloco de `description` do Path Item reescrito — nada mais (FR-004).
3. Operações, tags, `components/schemas` e o path `/api/v1/products/{id}` byte-a-byte intactos.
4. Documento parseável pela autoridade de validação (build do módulo).

## Estado da cadeia T-001-x após esta entrega

```
com-GET-com-parâmetro-mínimo (T-001-2 + contingência)
  → com-parâmetro-completo (T-001-3, esta feature)   ← encerra pendência do adendo R2
  → com-corpo-de-resposta (T-001-4)
  → com-autorização (T-001-5)
```
