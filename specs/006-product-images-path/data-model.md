# Phase 1 — Data Model: Path do endpoint de imagens de produto

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

> **Escopo desta seção**: esta task é de **fase contracts**. Não há entidade de runtime, tabela, coluna ou DTO envolvidos — nenhuma classe Java é sequer gerada (ver `research.md` R5). O "modelo de dados" aqui é o conjunto de **elementos de documento OpenAPI** criados e os elementos existentes que eles tocam.

## Elementos criados por esta task

### 1. Path Item `/api/v1/products/{productId}/images`

| Propriedade | Valor | Origem |
|---|---|---|
| Endereço | `/api/v1/products/{productId}/images` | FR-001 |
| `summary` | Rótulo curto do recurso | FR-004 |
| `description` | Deve declarar (a) semântica URL-only e (b) que as operações vêm das tasks restantes de T-002 | FR-004 |
| `parameters` | Um item: `productId` (ver abaixo) | FR-002 |
| Operações | **Nenhuma** — proibidas nesta task | FR-006 |
| `tags` | **Ausente** — é campo de operação em OpenAPI 3.1, não de Path Item | FR-010 |
| `security` | **Ausente** — herda `bearerAuth` da raiz | FR-005 |

**Estado transitório**: entre esta task e T-002-2 o Path Item não tem operações. Válido em OpenAPI 3.1; não gera código.

### 2. Parâmetro `productId` (nível de Path Item)

| Campo | Valor |
|---|---|
| `name` | `productId` — deve casar exatamente com o template `{productId}` (FR-003) |
| `in` | `path` |
| `required` | `true` |
| `description` | `Product UUID` |
| `schema.type` | `string` |
| `schema.format` | `uuid` |

**Regra de validação**: nome do parâmetro ≡ token do template. Divergência torna o documento inválido.

**Herança**: qualquer operação adicionada por T-002-2 recebe este parâmetro sem redeclará-lo.

## Elementos existentes tocados — nenhum

Esta task é estritamente aditiva (FR-007). Os elementos abaixo são **contexto**, não alvo; nenhum sofre edição:

| Elemento | Relação | Estado |
|---|---|---|
| `security` global (`bearerAuth`) — raiz | **Herdado** pelo novo Path Item | Intocado |
| `components/securitySchemes/bearerAuth` | Alvo da herança acima | Intocado |
| Path Item `/api/v1/products/{productId}/stock` | Precedente de convenção (sub-recurso, `{productId}`) | Intocado |
| Path Item `/api/v1/products/{id}` | Fonte da inconsistência `{id}` vs `{productId}`, deliberadamente não corrigida | Intocado |
| Schema `Product` | Receberá `images[]` na **cadeia T-003** | Intocado |

## Fronteira com o modelo de runtime

O que **não** existe ainda, e a quem pertence:

| Artefato de runtime | Cadeia responsável |
|---|---|
| Atributo `images` na entidade JPA `Product` | T-003-1 |
| Migração Flyway da coluna de imagens | T-003-2 |
| Campo `images` em `ProductRequest` / `ProductResponse` | T-003-3 / T-003-4 |
| Mapeamento MapStruct de `images` | T-003-5 |
| Método de serviço e handler de controller | T-005-x |

**Nota de coerência**: este path passa a existir **antes** do atributo `Product.images` que ele alimenta. É consequência aceita da decomposição em cadeias independentes, registrada nos Edge Cases da spec — transitoriamente o contrato descreve um endereço cujo efeito sobre o recurso `Product` ainda não é observável.

## Transições de estado

Nenhuma. Não há máquina de estados, contador, nem mutação de dados nesta task.
