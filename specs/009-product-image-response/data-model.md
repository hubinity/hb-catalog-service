# Phase 1 — Data Model: Schema de resposta do registro de imagem

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

> **Escopo**: task de **fase contracts**. Nenhuma entidade JPA, tabela ou coluna. Como em T-002-3, há contrapartida em código: um DTO **gerado**, sem lógica, ainda não referenciado pelo serviço.

## Elementos criados no documento

### 1. Schema `ProductImageResponse` (`components/schemas`)

| Propriedade do schema | Valor | Origem |
|---|---|---|
| `type` | `object` | FR-004 |
| `description` | Deve afirmar (a) que a resposta traz a **coleção resultante completa**, não só a entrada nova, e (b) que o **primeiro elemento é a imagem principal** | FR-004 |
| `required` | `[productId, images]` | FR-005 |
| Propriedades | **Exatamente duas**: `productId`, `images` | FR-005 |
| `additionalProperties` | **Não declarado** — leitor tolerante, como em T-002-3 | FR-010 |
| Posição | Anexado após `ProductImageRequest` | Assumptions |

#### Propriedade `productId`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | |
| `format` | `uuid` | |
| `description` | Produto dono das imagens | Redundante com o path **por decisão** — torna a resposta autocontida; precedente: `StockItem` também declara `productId` |

#### Propriedade `images`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `array` | FR-007 |
| `description` | Coleção resultante completa, **em ordem** | A ordem é o que torna observável a convenção "primeiro = principal" |
| `items.type` | `string` | |
| `items.format` | `uri` | Documentacional |
| `items.maxLength` | `2048` | Simetria com `ProductImageRequest.url`; **adição desta spec**, removível |
| `minItems` | **Não declarado** | Seria afirmação de comportamento (cadeia T-005), não de forma — FR-009 |

### 2. `content` acrescentado ao desfecho `201`

| Propriedade | Valor | Origem |
|---|---|---|
| Tipo de mídia | **Exclusivamente** `application/json` | FR-001 |
| `schema` | `$ref: '#/components/schemas/ProductImageResponse'` — sem inline | FR-002 |
| Posição | **Abaixo** da `description` existente do `201`, que não é tocada | FR-003 |

## Contrapartida em código

| | Antes | Depois |
|---|---|---|
| Schemas em `components/schemas` | 5 | **6** |
| DTOs gerados | `Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest` | os mesmos 5 **+ `ProductImageResponse`** |
| Diretório `catalog/api/` | inexistente | inexistente |

**Natureza**: portador de dados sem lógica. O serviço não o referencia — isso é da cadeia T-005. A afirmação "nenhum DTO preexistente alterado" só é verificável com **captura prévia** do inventário (FR-015), recapturada nesta execução (research R8).

## Elementos existentes tocados — nenhum

| Elemento | Relação | Estado |
|---|---|---|
| `description` do `201` | O `content` é acrescentado abaixo dela | **Intocada** |
| Desfechos `400`, `403`, `404` | Permanecem description-only | Intocados |
| `description` do Path Item | Verdadeira como proveniência; limpeza é de T-002-5 | **Intocada** |
| `requestBody` e `ProductImageRequest` | Entregues por T-002-3 | Intocados |
| Schemas `Product`, `Category`, `StockItem`, `StockMovement` | Vizinhos | Intocados |

**Consequência**: mudança **estritamente aditiva** — zero remoções, como em T-002-1 e T-002-3.

## Divergências contrato × serviço conhecidas (registradas, não resolvidas)

| Divergência | Estado | Encaminhamento |
|---|---|---|
| Schema `Product` sem `images` | Aberta; agravaria após T-003-4 | Entrada proposta `T-002-6` |
| Erros sem `ProblemDetail` (RFC 7807) | Aberta; Princípio I exige do serviço | Entrada proposta `T-002-7` |

Nenhuma é criada por esta task; ambas são **preexistentes e agora documentadas**. A escolha de schema próprio (research R1) faz esta task **não depender** da primeira.

## Fronteira com o modelo de runtime

| Artefato | Cadeia responsável |
|---|---|
| Estratégia de armazenamento documentada + limpeza da `description` do Path Item | T-002-5 |
| `images` no schema `Product` do contrato | **proposta T-002-6** |
| `ProblemDetail` nos desfechos de erro | **proposta T-002-7** |
| Atributo `images`, coluna `text[]`, migração, DTOs do serviço, mapper | cadeia T-003 |
| Handler, montagem da resposta, `@PreAuthorize` | cadeia T-005 |

## Transições de estado

Nenhuma. O schema descreve o estado resultante de uma operação; não há máquina de estados.
