# Phase 1 Data Model: Referenciar ProblemDetail no 404 de getProductById

Nenhuma entidade de dado nova. Esta task altera apenas a documentação de um desfecho de resposta já existente — não há campo, coluna, ou DTO novo além do que `T-002-7-1` já gerou (`ProblemDetail.java`).

## Desfecho `404` de `getProductById` (modificado)

| Aspecto | Antes | Depois |
|---|---|---|
| `description` | "Product not found" | "Product not found" (inalterado) |
| `content` | ausente | `application/json` → `schema: $ref '#/components/schemas/ProblemDetail'` |

## Schema `ProblemDetail` (pré-existente, inalterado)

Já documentado em `012-problemdetail-schema/data-model.md` (se existir) ou diretamente no schema do contrato — cinco propriedades opcionais (`type`, `title`, `status`, `detail`, `instance`). Esta task não modifica nenhuma delas; apenas cria a primeira referência real ao schema em uma operação.

## Relacionamentos

Nenhum. É uma referência (`$ref`) de um desfecho de resposta a um schema já existente — não uma relação de dado nova.
