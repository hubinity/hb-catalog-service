# Phase 1 — Data Model: Propriedade images no schema Product

**Feature**: `011-product-schema-images` · **Task**: `T-002-6` · **Date**: 2026-07-26

## Entidade nova: uma propriedade — e a forma de geração é inédita na linhagem

| Task | Entidade de documento introduzida | Efeito na geração |
|---|---|---|
| `T-002-3` | schema `ProductImageRequest` | +1 arquivo (5 → 6) |
| `T-002-4` | schema `ProductImageResponse` | +1 arquivo (6 → 6, contando a partir do novo baseline) |
| `T-002-5` | nenhuma (só `description`) | **0** — nenhum arquivo muda |
| **`T-002-6`** | **propriedade `images` em `Product`** | **0 arquivos novos, 1 alterado** |

É a primeira task a **alterar uma classe gerada existente** em vez de criar uma. Daí a forma do gate: contar arquivos não basta, é preciso confirmar o conteúdo (FR-016 + FR-017).

## Inventário afetado

| Elemento | Tipo | Ação desta task | Efeito na geração |
|---|---|---|---|
| `Product.properties.images` | Propriedade de schema | **Acrescentada** | Campo novo em `Product.java` |
| `Product.required` | Lista | **Intocada** — `images` não entra (FR-006) | Campo fica opcional/nullable |
| `Product.properties` (9 preexistentes) | Propriedades | **Intocadas** (FR-007) | Nenhum |
| `Product.description` (do schema) | Metadado | **Intocada** (FR-011) | Nenhum |
| `ProductImageRequest` | Schema | **Intocado** — `T-002-3`, `done` | Nenhum |
| `ProductImageResponse` | Schema | **Intocado** — `T-002-4`, `done` (molde de forma) | Nenhum |
| `Category`, `StockItem`, `StockMovement` | Schemas | **Intocados** | Nenhum |
| `info.description` | Metadado | **Intocada** — `T-002-5`, `done` (alvo da remissão) | Nenhum |
| Paths, operações, desfechos | — | **Intocados** (FR-013) | Nenhum |

## Definição da propriedade

```yaml
images:
  type: array                    # FR-002
  description: |                 # FR-004, FR-005
    ordem + primeiro = principal + ausente-ou-vazia + remissão à estratégia
  items:
    type: string                 # FR-003 ─┐
    format: uri                  # FR-003  ├─ conjunto FECHADO: nada além destes três
    maxLength: 2048              # FR-003 ─┘
```

**Ausências obrigatórias** — cada uma com razão própria, todas expressas como `MUST NOT`:

| Ausente | Requisito | Razão |
|---|---|---|
| `minItems` | FR-008 | Afirmação de comportamento, própria das cadeias T-003/T-005 |
| `maxItems` | FR-009 | Teto de cardinalidade pertence ao **atributo**; alocado à cadeia T-003 por `T-002-3` |
| `readOnly` | FR-010 | `Product` **nunca** é corpo de requisição (verificado); não há ambiguidade de escrita a resolver |
| `minLength` | FR-003 (conjunto fechado) | Valida **entrada**, não saída; simetria devida é com `ProductImageResponse.images`, não com `ProductImageRequest.url` |
| qualquer outra palavra-chave | FR-003 (conjunto fechado) | O conjunto é exatamente o de `ProductImageResponse.images` |

## Relação com a coleção equivalente

```text
   POST /api/v1/products/{productId}/images
        ├── entrada:  ProductImageRequest.url
        │              type: string, format: uri, minLength: 1, maxLength: 2048
        │                                          └── só aqui: valida submissão do cliente
        │
        └── saída:    ProductImageResponse.images[]
                       type: string, format: uri, maxLength: 2048
                                     ▲
                                     │  MESMA COLEÇÃO, mesma forma  (FR-003)
                                     ▼
   GET  /api/v1/products/{id}
        └── saída:    Product.images[]          ← esta task
                       type: string, format: uri, maxLength: 2048
```

A assimetria com a entrada é deliberada e está registrada nos *Edge Cases* da spec: restrição em resposta descreve o que o servidor produz; não valida nada.

## Regras de validação

Nenhuma introduzida além das três restrições de item, que são **descritivas** (resposta), não validantes. A expectativa de HTTPS continua sem enforcement em todo o documento — proposta `T-002-8`, ainda não inserida no tracker.

## Semântica de ausência

Sendo opcional, "produto sem imagem" admite **duas** representações: chave ausente ou array vazio. O contrato **não** elege uma — eleger seria afirmação sobre o comportamento do serviço, própria das cadeias T-003/T-005. A `description` declara que ambas significam o mesmo, para que o consumidor trate as duas (FR-004).

## Transições de estado

N/A. `images` não interage com `active` nem com soft delete (`deleted_at`), e nenhuma semântica condicional é declarada. A ordenação é convenção posicional — primeiro elemento é a imagem principal —, não máquina de estados.

## Contrapartida gerada

`Product.java` ganha um campo `images` (lista de `String`, anotado por Jackson conforme configuração do gerador). Nenhuma classe nova: itens de tipo `string` com `format`/`maxLength` não produzem tipo próprio. Por isso o inventário permanece **6 ↔ 6**, com **exatamente um** conteúdo diferente — o que FR-016 mede e FR-017 confirma.
