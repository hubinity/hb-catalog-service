# Phase 1 — Data Model: Operação POST de registro de imagem de produto

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

> **Escopo desta seção**: task de **fase contracts**. Não há entidade de runtime, tabela, coluna ou DTO envolvidos — e, verificado empiricamente, **nenhuma classe Java é gerada** a partir de uma operação (`generateApis=false`; o diretório `catalog/api/` não existe em `target/generated-sources`). O "modelo de dados" aqui é o conjunto de **elementos de documento OpenAPI** criados ou alterados.

## Elementos criados

### 1. Operação `post` / `addProductImage`

| Propriedade | Valor | Origem |
|---|---|---|
| Verbo | `post` no Path Item de imagens, posicionado após `parameters` | FR-001 |
| `operationId` | `addProductImage` — único no documento | FR-002 |
| `summary` | Rótulo curto da ação | FR-002 |
| `tags` | `[products]` — tag existente, nenhuma criada | FR-002 |
| `description` | Deve afirmar a exigência da role `admin` e que um JWT válido não basta | FR-007 |
| `parameters` | **Ausente** — `productId` é herdado do Path Item | FR-003 |
| `requestBody` | **Ausente** — entregável de T-002-3 | FR-010 |
| `security` | **Ausente** — herda `bearerAuth` da raiz | FR-011 |
| Header `Idempotency-Key` | **Ausente** — decisão registrada (R3) | FR-012 |

**Marco**: primeira operação de mutação declarada no contrato. Até aqui só existiam `getProductById` e `getStockItemByProductId`.

### 2. Desfechos declarados (todos **sem `content`**)

| Código | Significado | `content`? | Origem |
|---|---|---|---|
| `201` | Referência de imagem registrada. `description` deve registrar a ausência de `Location` e a razão. | Não — T-002-4 | FR-004, FR-005 |
| `403` | Principal autenticado sem a role `admin` — causa distinta de falha de autenticação. | Não | FR-006 |
| `404` | Produto inexistente. | Não | FR-008 |

**Não declarados, por decisão**: `400` (depende do corpo — T-002-3), `401` (consistência com T-001-2), `409` (depende da semântica de duplicata — T-002-3). Ver research R4.

## Elemento alterado

### `description` do Path Item `/api/v1/products/{productId}/images`

| | |
|---|---|
| Estado anterior | Afirma que "Operations on this path are declared by the remaining T-002 tasks" |
| Por que muda | A frase torna-se **factualmente falsa** assim que o `post` é declarado |
| Estado novo | Preserva a semântica URL-only; registra que o POST está declarado e que corpo de requisição e de resposta vêm de T-002-3/T-002-4; registra a exigência adicional da role `admin` |
| Origem | FR-013; texto substituto fixado em *Elementos alvo (1)* da spec |

**Consequência**: esta é a **única** linha-fonte de remoções no diff. É o que torna a mudança não-aditiva, ao contrário de T-002-1.

## Elementos existentes tocados — nenhum além do acima

| Elemento | Relação | Estado |
|---|---|---|
| `parameters` do Path Item (`productId`) | **Herdado** pela operação | Intocado |
| `summary` do Path Item | Vizinho do elemento alterado | Intocado |
| `security` global (`bearerAuth`) | **Herdado** pela operação | Intocado |
| Tag `products` em `tags` | **Reutilizada** pela operação | Intocado |
| `getProductById`, `getStockItemByProductId` | Operações existentes | Intocadas |
| `components/schemas` (4 schemas) | Nenhum schema adicionado nesta task | Intocado |

## Fronteira com o modelo de runtime

| Artefato de runtime | Cadeia responsável |
|---|---|
| Schema do corpo da requisição (referência de URL) | T-002-3 |
| Schema do corpo do `201` | T-002-4 |
| Atributo `images` na entidade `Product` + migração Flyway | T-003-1 / T-003-2 |
| Handler `POST` em `ProductController` + `@PreAuthorize("hasRole('admin')")` | T-005-2 |
| Retorno do `ProductResponse` atualizado | T-005-5 |

**Nota de coerência**: a operação passa a existir antes de ter corpo declarado e antes do atributo `Product.images` que ela alimenta. É consequência aceita da decomposição em cadeias, e o mesmo padrão que a cadeia T-001 exerceu (T-001-2 declarou a operação antes do corpo, entregue em T-001-4).

## Transições de estado

Nenhuma nesta task. A operação *declarada* implicará, quando implementada (T-005), a adição de uma referência à coleção `images[]` — sem máquina de estados.
