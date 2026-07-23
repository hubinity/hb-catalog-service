# Data Model: Operação GET no path canônico de saldo de estoque (T-001-2)

**Date**: 2026-07-22 · **Plan**: [plan.md](./plan.md)

Nenhuma entidade de banco é criada ou alterada. O "modelo de dados" é a estrutura do documento de contrato.

## Entidade 1 — Operação `get` (nova)

| Aspecto | Valor |
|---|---|
| Localização | Path Item `/api/v1/products/{productId}/stock` → chave `get` |
| Campos | `tags: [stock]`, `operationId: getStockItemByProductId`, `summary: Fetch the current stock balance for a product`, `responses` ('200', '404') |
| Regras de validação | FR-001 (única operação do Path Item), FR-002 (identidade/formatos), FR-004 (respostas com textos fixados, sem `content`), FR-005 (documento válido pós-edição) |
| Relacionamentos | Pertence ao Path Item de T-001-1; futura dona do `parameters` (T-001-3), do `content` da '200' (T-001-4) e do `security` (T-001-5) |

## Entidade 2 — Tag `stock` (nova)

| Aspecto | Valor |
|---|---|
| Localização | Seção `tags` do documento (após `products`) |
| Campos | `name: stock`, `description: Product stock balance and movements` |
| Regras | FR-003 (sem tag órfã: declarada e usada) |

## Entidade 3 — Path Item do saldo (existente — editado)

| Aspecto | Valor |
|---|---|
| Mudanças | Ganha a operação `get`; `description` reescrita (FR-007 — declara GET existente, aponta T-001-3..5 para parâmetro/corpo/auth); `summary` intacto |
| Restrição | Nenhuma outra chave adicionada (sem `parameters` no nível do Path Item — salvo contingência R2/FR-006, que os colocaria no nível da operação) |

## Entidade 4 — Schema `StockItem` (existente — intocado)

Referenciável apenas a partir de T-001-4. Qualquer edição aqui é fuga de escopo (FR-005).

## Invariantes do documento após a edição

1. 2 paths, 2 operações no total (`getProductById` intacta + `getStockItemByProductId` nova); `operationId`s únicos.
2. Seção `tags` com exatamente 2 entradas (`products`, `stock`), ambas usadas.
3. `components/schemas` byte-a-byte idêntico ao estado anterior.
4. Diff restrito ao Path Item do saldo + seção `tags` (FR-005); demais linhas do documento inalteradas.
5. Documento parseável pela autoridade de validação (build do módulo).

## Máquina de estados do Path Item (ciclo da cadeia T-001-x)

```
declarado-sem-operação (T-001-1, done)
  → com-GET-sem-parâmetro (T-001-2, esta feature)   [ou com-GET-com-parâmetro-mínimo, se contingência R2]
  → com-parâmetro (T-001-3)
  → com-corpo-de-resposta (T-001-4)
  → com-autorização (T-001-5)
```
