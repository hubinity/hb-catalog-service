# Phase 1 — Data Model: Schema ProblemDetail (RFC 7807)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-26

> **Escopo**: task de **fase contracts**. Nenhuma entidade JPA, tabela ou coluna. Como em `T-002-3`/`T-002-4`, há contrapartida em código: um DTO **gerado**, sem lógica, ainda não referenciado por nenhuma operação.

## Elemento criado no documento

### Schema `ProblemDetail` (`components/schemas`)

| Propriedade do schema | Valor | Origem |
|---|---|---|
| `type` | `object` | FR-001 |
| `description` | Deve afirmar que a forma é produzida pelo exception handler global do serviço e usada por toda resposta de erro da API | FR-002 |
| `required` | **Não declarado** — nenhum dos cinco membros é obrigatório | FR-009 |
| Propriedades | **Exatamente cinco**: `type`, `title`, `status`, `detail`, `instance` | FR-003 |
| `additionalProperties` | **Não declarado** — leitor tolerante, permite extensões como `errors` | FR-010 |
| Posição | Anexado após `ProductImageResponse` (último hoje) | Assumptions |

#### Propriedade `type`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | FR-004 |
| `format` | `uri` | |
| `description` | Menciona o padrão `"about:blank"` quando o handler não define um tipo mais específico | Comportamento real de `ProblemDetail.forStatusAndDetail` (research R2) |

#### Propriedade `title`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | FR-005 |

#### Propriedade `status`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `integer` | FR-006 |
| `format` | `int32` | |
| Sem enum | Múltiplos códigos já em uso (`400, 403, 404, 409, 422`); RFC 7807 não restringe (research R4) | |

#### Propriedade `detail`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | FR-007 |

#### Propriedade `instance`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | FR-008 |
| `format` | `uri` | |

## Contrapartida em código

| | Antes | Depois |
|---|---|---|
| Schemas em `components/schemas` | 6 | **7** |
| DTOs gerados | `Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse` | os mesmos 6 **+ `ProblemDetail`** |

**Natureza**: portador de dados sem lógica. Nenhuma operação o referencia ainda (isso é `T-002-7-2..6`). A afirmação "nenhum DTO preexistente alterado" só é verificável com **captura prévia** do inventário (FR-014), capturada nesta execução (research R6).

## Elementos existentes tocados — nenhum

| Elemento | Relação | Estado |
|---|---|---|
| Desfechos `404` (`getProductById`, `getStockItemByProductId`) e `400`/`403`/`404` (`addProductImage`) | Permanecem description-only | **Intocados** (FR-011) |
| Schemas `Product`, `Category`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse` | Vizinhos | Intocados |
| `description` de qualquer Path Item ou operação | — | Intocada |

**Consequência**: mudança **estritamente aditiva** — zero remoções (FR-012).

## Propriedade de extensão não modelada (registrada, não resolvida aqui)

| Propriedade | Emitida por | Por que fora de escopo |
|---|---|---|
| `errors` (mapa campo→mensagem) | `ApiExceptionHandler.handleMethodArgumentNotValid` | Específica de erros de validação, não de todo `ProblemDetail`; modelá-la exigiria um schema especializado, decisão de produto separada (research R3) |

A ausência de `additionalProperties: false` permite essa e outras extensões futuras sem exigir declaração explícita.

## Fronteira com as subtarefas seguintes

| Artefato | Cadeia responsável |
|---|---|
| `content` do `404` de `getProductById` → `ProblemDetail` | `T-002-7-2` |
| `content` do `404` de `getStockItemByProductId` → `ProblemDetail` | `T-002-7-3` |
| `content` do `400` de `addProductImage` → `ProblemDetail` | `T-002-7-4` |
| `content` do `403` de `addProductImage` → `ProblemDetail` | `T-002-7-5` |
| `content` do `404` de `addProductImage` → `ProblemDetail` | `T-002-7-6` |
| Schema especializado para `errors` de validação (se algum dia necessário) | Não tracked — lacuna registrada em research R3 |

## Transições de estado

Nenhuma. O schema descreve a forma de uma resposta de erro; não há máquina de estados.
