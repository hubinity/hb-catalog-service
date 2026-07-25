# Phase 1 — Data Model: Estratégia de armazenamento de imagens de produto

**Feature**: `010-image-storage-strategy` · **Task**: `T-002-5` · **Date**: 2026-07-25

## Entidades novas: nenhuma — e isso é o desenho, não uma omissão

Esta é a **primeira feature da cadeia T-002 que não introduz entidade alguma**. As quatro anteriores introduziram: `T-002-1` um Path Item, `T-002-2` uma operação, `T-002-3` o schema `ProductImageRequest`, `T-002-4` o schema `ProductImageResponse`.

A entrega desta task é integralmente **textual**: dois blocos de `description`. Não há schema, propriedade, operação, parâmetro ou desfecho novo — nem, por consequência, DTO gerado.

**Consequência verificável**: o inventário gerado permanece **6 ↔ 6**, com checksum idêntico em todos os seis arquivos. Ver `quickstart.md` §Gate 2.

## Inventário afetado

| Elemento | Tipo | Ação desta task | Gera código? |
|---|---|---|---|
| `info.description` | Campo de metadado do documento | **Acrescido** de um parágrafo; as 2 linhas existentes permanecem intactas acima | Não |
| `description` do Path Item `/api/v1/products/{productId}/images` | Campo de metadado de Path Item | **Reescrita** para o estado final (única remoção da entrega) | Não |
| `summary` do mesmo Path Item | Campo de metadado | **Intocado** (FR-013) | Não |
| `ProductImageRequest` | Schema | **Intocado** — propriedade de `T-002-3` (`40dd8e0`, `done`) | Sim (já existe) |
| `ProductImageResponse` | Schema | **Intocado** — propriedade de `T-002-4` (`4fa9056`, `done`) | Sim (já existe) |
| `Product`, `Category`, `StockItem`, `StockMovement` | Schemas | **Intocados** | Sim (já existem) |
| Operação `addProductImage` e seus desfechos | Operação | **Intocada** (FR-016) | Não |

## Por que `description` não gera código — e por que ainda assim é verificado

O `openapi-generator-maven-plugin` roda com `generateApis=false`, `generateModels=true` (ADR 0002): produz **apenas modelos**, a partir de `components/schemas`.

- `info.description` é metadado do documento. Sem `generateApis`, não há classe de API onde ele pudesse aterrissar.
- `description` de **Path Item** descreve um endereço, não um modelo. Nenhum modelo deriva dele.

Contraste que justifica manter o gate: `description` de **schema** e de **propriedade** *entra* no Javadoc do DTO gerado. Ou seja, "mexi só em `description`" **não** implica inércia de geração em geral — implica apenas quando as `description` tocadas são as duas acima. Por isso a inércia é **comprovada por checksum**, não presumida pelo tipo da edição.

## Modelo conceitual declarado (não é modelo de dados desta task)

O parágrafo acrescentado descreve o modelo de responsabilidade que já vigora, e passa a vigorar por escrito:

```text
   ┌──────────────────────────┐         ┌───────────────────────────────┐
   │  hb-catalog-service      │         │  Host externo (fora do        │
   │                          │         │  domínio deste contrato)      │
   │  persiste: URL (texto)   │ ──────▶ │  guarda: os bytes da imagem   │
   │  nunca vê bytes          │  refere │  responde pela disponibilidade│
   └──────────────────────────┘         └───────────────────────────────┘

   Fronteira declarada por FR-004:
     · o catálogo NÃO verifica que a URL resolve
     · URL que deixa de resolver NÃO é violação de contrato
     · remover imagem remove a REFERÊNCIA, nunca um arquivo (FR-005)
```

**Onde a URL será persistida** é da cadeia T-003 (coluna `text[]`, fixada por `T-002-3`) e **não** é decidido aqui. Esta task declara apenas que o que se persiste é uma referência, não um arquivo.

## Regras de validação

Nenhuma introduzida. Em particular, a expectativa de **HTTPS permanece sem enforcement**: nenhum `pattern` é adicionado (FR-015), pelas três razões consolidadas em `research.md` §D4 — escopo (`ProductImageRequest` pertence a `T-002-3`, `done`), comportamento (`@Pattern` passaria a rejeitar `http://` em runtime, além de alterar o checksum do DTO e quebrar o gate de inércia) e produto (a decisão de recusar `http://` não foi tomada por ninguém). Encaminhada como proposta `T-002-8`.

As restrições vigentes sobre `url` seguem as de `T-002-3`, intocadas: `format: uri`, `minLength: 1`, `maxLength: 2048`.

## Transições de estado

N/A. Nenhum ciclo de vida é declarado por esta task. A semântica de remoção (FR-005) descreve **o que uma remoção significaria** sob a estratégia — remover a referência, nunca um arquivo — sem declarar operação de remoção, que nenhuma task do tracker cobre.
