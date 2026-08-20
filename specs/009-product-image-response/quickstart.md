# Quickstart — Validação de T-002-4 (Schema de resposta do registro de imagem)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fragmentos**: [contracts/product-image-response.yaml](./contracts/product-image-response.yaml)

Guia de validação da edição de contrato. **Não** contém código de implementação.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Repos irmãos lado a lado; branch `feature/stock-balance-path` ativa em ambos
- T-002-3 concluída (commit `40dd8e0`) — `requestBody`, `ProductImageRequest` e o `400` devem existir
- **Docker não é necessário**

## Passo 1 — Capturar os dois baselines ANTES de editar

> Os mesmos dois gates de T-002-3, **recapturados nesta execução**. Reaproveitar os números da anterior invalidaria as comparações: `target/` é regenerado a cada build e a contagem de testes pode ter mudado.

**1a. Inventário de DTOs gerados** (base do FR-015):

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-baseline-009.txt
cat /tmp/dto-baseline-009.txt
```

**Esperado**: 5 linhas — `Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`.

**1b. Contagem de testes do consumidor** (base do FR-016):

```bash
cd hb-catalog-service
mvn -B verify 2>&1 | grep -E "Tests run:.*Failures" | tail -1
```

Anote a contagem.

## Passo 2 — Aplicar os dois fragmentos

De [`contracts/product-image-response.yaml`](./contracts/product-image-response.yaml), em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`:

1. **Fragmento 1** — `content` acrescentado ao `'201'`, **abaixo** da `description` existente. Não reescrever a `description`.
2. **Fragmento 2** — schema `ProductImageResponse`, anexado ao fim de `components/schemas`, após `ProductImageRequest`.

Não copiar as linhas de comentário. **Não tocar na `description` do Path Item.**

## Passo 3 — Provar que a mudança é estritamente aditiva

```bash
cd platform-shared-contracts
git diff --stat contracts-catalog/openapi/catalog.yaml
git diff -U0 contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
# Esperado: NENHUMA saída
```

Confirmar que as duas `description` sobreviveram intactas:

```bash
grep -c "No Location header is returned" contracts-catalog/openapi/catalog.yaml   # → 1
grep -c "are completed by" contracts-catalog/openapi/catalog.yaml                 # → 1
```

Confirmar que os erros seguem sem corpo:

```bash
sed -n "/'400':/,/'404':/p" contracts-catalog/openapi/catalog.yaml | grep -c "content:"
# Esperado: 0 — 400 e 403 permanecem description-only (FR-011)
```

## Passo 4 — Validar o documento (build do módulo)

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. Prova FR-014 e SC-004.

## Passo 5 — Comparar o inventário de DTOs

```bash
cd platform-shared-contracts
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-after-009.txt

diff /tmp/dto-baseline-009.txt /tmp/dto-after-009.txt
```

**Esperado** (FR-015): **uma única linha adicionada**, `ProductImageResponse.java`. Checksums dos 5 preexistentes **idênticos** ao baseline.

```bash
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" | wc -l
# Esperado: 6
```

## Passo 6 — Provar regressão zero no consumidor

```bash
cd hb-catalog-service
mvn -B verify
```

**Esperado**: `BUILD SUCCESS` com a **mesma contagem do Passo 1b**. Prova FR-016 e SC-006.

## Passo 7 — Conferência do resultado contra os requisitos

| Verificação | Requisito |
|---|---|
| `201` tem `content` com `application/json` como **única** mídia | FR-001 |
| Schema referenciado por `$ref`, sem inline | FR-002 |
| `description` do `201` inalterada (Passo 3) | FR-003 |
| `ProductImageResponse` existe, `type: object`, com `description` sobre coleção completa **e** primeiro = principal | FR-004 |
| `required: [productId, images]`, exatamente duas propriedades | FR-005 |
| `productId` com `type: string`, `format: uuid`, `description` | FR-006 |
| `images` é `array` com `description` afirmando a ordem | FR-007 |
| Itens com `type: string`, `format: uri`, `maxLength: 2048` | FR-008 |
| **Não** há `minItems` | FR-009 |
| **Não** há `additionalProperties: false` | FR-010 |
| `400`/`403`/`404` seguem sem `content` (Passo 3) | FR-011 |
| `description` do Path Item inalterada (Passo 3) | FR-012 |
| Diff sem linhas `-` (Passo 3) | FR-013 |
| Build do módulo verde (Passo 4) | FR-014 |
| Inventário: +1 DTO, 5 preexistentes com checksum idêntico (Passo 5) | FR-015 |
| `mvn -B verify` verde na contagem do Passo 1b (Passo 6) | FR-016 |

## Fora do escopo desta validação

Nenhuma requisição HTTP é exercida: a operação está completa no contrato (endereço, verbo, corpo de requisição e agora de resposta), mas segue sem implementação (cadeia T-005). Os dois DTOs gerados existem no artefato e ainda não são referenciados por nenhuma classe do serviço.

**Duas divergências contrato × serviço seguem abertas** e não são validadas aqui — schema `Product` sem `images` (proposta `T-002-6`) e erros sem `ProblemDetail` (proposta `T-002-7`).
