# Phase 1 — Data Model: Corpo de requisição JSON do registro de imagem

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Date**: 2026-07-25

> **Escopo**: task de **fase contracts**. Nenhuma entidade JPA, tabela ou coluna é criada. **Diferença em relação a T-002-1 e T-002-2**: aqui existe, pela primeira vez na cadeia, uma contrapartida em código — um DTO **gerado** a partir do schema. Ele não tem lógica e ainda não é referenciado pelo serviço.

## Elementos criados no documento

### 1. Schema `ProductImageRequest` (`components/schemas`)

| Propriedade do schema | Valor | Origem |
|---|---|---|
| `type` | `object` | FR-004 |
| `description` | Deve afirmar que só a URL é armazenada, que bytes nunca trafegam, e a expectativa de HTTPS com a razão (mixed content) | FR-004, FR-007 |
| `required` | `[url]` | FR-005 |
| Propriedades | **Exatamente uma**: `url` | FR-005 |
| `additionalProperties` | **Não declarado** — leitor tolerante, extras ignorados | FR-016 |
| `pattern` | **Não declarado** — HTTPS é expectativa, não imposição | FR-007 |
| Posição | Anexado após `StockMovement` | Assumptions |

#### Propriedade `url`

| Campo | Valor | Nota |
|---|---|---|
| `type` | `string` | Em OpenAPI 3.1, `type: string` já exclui `null` |
| `format` | `uri` | **Documentacional** — não valida; validação efetiva é do serviço (T-005) |
| `minLength` | `1` | Impede string vazia; espelha `sku`/`name` |
| `maxLength` | `2048` | Único limite **estrutural** real; base para dimensionar a coluna em T-003-2 |
| `description` | URL absoluta da imagem hospedada externamente | FR-006 |

### 2. `requestBody` da operação `addProductImage`

| Propriedade | Valor | Origem |
|---|---|---|
| `required` | `true` | FR-001 |
| `description` | Presente | FR-001 |
| `content` | **Exclusivamente** `application/json` — `multipart/form-data` proibido | FR-001 |
| `schema` | `$ref: '#/components/schemas/ProductImageRequest'` — sem inline | FR-002 |
| Posição | Entre a `description` da operação e `responses` | FR-003 |

### 3. Desfecho `400`

| Propriedade | Valor | Origem |
|---|---|---|
| `description` | Nomeia as causas: corpo malformado, `url` ausente, URI inválida, excesso de tamanho | FR-008 |
| `content` | **Ausente** — convenção description-only da cadeia; corpos são de T-002-4 | FR-009 |
| Posição | Em `responses`, antes de `'403'` | Elementos alvo |

## Contrapartida em código — **novidade nesta cadeia**

| | Antes | Depois |
|---|---|---|
| Schemas em `components/schemas` | 4 | **5** |
| DTOs gerados em `.../catalog/dto/` | `Category`, `Product`, `StockItem`, `StockMovement` | os mesmos 4 **+ `ProductImageRequest`** |
| Diretório `catalog/api/` | inexistente | inexistente (`generateApis=false` inalterado) |

**Natureza do artefato gerado**: `ProductImageRequest.java` é um portador de dados — campo, acessores, `equals`/`hashCode`/`toString`. **Sem lógica de negócio, sem comportamento a testar.** O serviço não o referencia; a referência é entregável da cadeia T-005.

**Por que isso importa para o gate**: a afirmação "nenhum DTO preexistente foi alterado" só é verificável se o inventário for **capturado antes** da edição, já que `target/` é reconstruído a cada build (FR-014).

## Elementos existentes tocados — nenhum

| Elemento | Relação | Estado |
|---|---|---|
| Operação `addProductImage` | Recebe `requestBody` e o desfecho `400` | **Adições apenas** — nada reescrito |
| Desfechos `201`, `403`, `404` | Vizinhos do `400` | Intocados |
| `description` do Path Item | Permanece verdadeira como proveniência (R8) | **Intocada** |
| `summary`, `parameters` do Path Item | — | Intocados |
| Schemas `Product`, `Category`, `StockItem`, `StockMovement` | Vizinhos do schema novo | Intocados |
| `security` global, `tags`, `securitySchemes` | — | Intocados |

**Consequência**: mudança **estritamente aditiva** — zero remoções no diff, ao contrário de T-002-2.

## Fronteira com o modelo de runtime

| Artefato | Cadeia responsável |
|---|---|
| Schema do corpo de resposta e `content` do `201` | T-002-4 |
| Estratégia de armazenamento documentada no contrato | T-002-5 |
| Atributo `images` (lista de strings) na entidade `Product` | T-003-1 |
| Coluna **`text[]`** e migração Flyway | T-003-2 |
| Campos `images` em `ProductRequest`/`ProductResponse` do serviço | T-003-3 / T-003-4 |
| Mapeamento MapStruct | T-003-5 |
| Semântica de URL duplicada / eventual restrição de unicidade | **T-003** (realocado — R7) |
| Validação efetiva da URL, rejeição de `http://`, handler | cadeia T-005 |

**Nota**: a decisão de corpo desta task **determina** a forma do atributo a jusante — lista de strings, portanto `text[]`. É a primeira vez na cadeia T-002 que uma decisão de contrato fixa uma escolha de esquema de banco.

## Transições de estado

Nenhuma. O schema descreve uma entrada; não há máquina de estados.
