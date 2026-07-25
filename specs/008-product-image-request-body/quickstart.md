# Quickstart — Validação de T-002-3 (Corpo de requisição do registro de imagem)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fragmentos**: [contracts/product-image-request.yaml](./contracts/product-image-request.yaml)

Guia de validação da edição de contrato. **Não** contém código de implementação.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Repos irmãos lado a lado; branch `feature/stock-balance-path` ativa em ambos
- T-002-2 concluída (commit `854c02f`) — a operação `addProductImage` deve existir
- **Docker não é necessário**

## Passo 1 — Capturar os DOIS baselines ANTES de editar

> Esta task tem duas afirmações que só são verificáveis com estado capturado previamente. Ambas as capturas são **obrigatórias** e devem ocorrer antes de qualquer edição.

**1a. Inventário de DTOs gerados** (base do FR-014):

```bash
cd platform-shared-contracts
mvn -B -DskipTests install    # garante target/ atualizado no estado ANTERIOR
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-baseline.txt
cat /tmp/dto-baseline.txt
```

**Esperado**: 4 linhas — `Category.java`, `Product.java`, `StockItem.java`, `StockMovement.java`, cada uma com seu checksum.

> Sem esta captura, "nenhum DTO preexistente foi alterado" seria **inverificável**: `target/` é regenerado a cada build, então não há como comparar depois do fato.

**1b. Contagem de testes do consumidor** (base do FR-015):

```bash
cd hb-catalog-service
mvn -B verify 2>&1 | grep -E "Tests run:.*Failures" | tail -1
```

Anote a contagem — medida agora, não herdada de specs anteriores.

## Passo 2 — Aplicar os três fragmentos

De [`contracts/product-image-request.yaml`](./contracts/product-image-request.yaml), em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`:

1. **Fragmento 1** — `requestBody` na operação `post`, entre a `description` da operação e `responses`.
2. **Fragmento 2** — desfecho `'400'` em `responses`, antes de `'403'`.
3. **Fragmento 3** — schema `ProductImageRequest`, anexado ao fim de `components/schemas`, após `StockMovement`.

Não copiar as linhas de comentário. **Não tocar na `description` do Path Item.**

## Passo 3 — Provar que a mudança é estritamente aditiva

> Diferente de T-002-2 (que substituía uma `description`), aqui vale o critério de T-002-1: **zero remoções**.

```bash
cd platform-shared-contracts
git diff --stat contracts-catalog/openapi/catalog.yaml
git diff -U0 contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
# Esperado: NENHUMA saída
```

Confirmar que a `description` do Path Item seguiu intacta:

```bash
grep -c "are completed by" contracts-catalog/openapi/catalog.yaml
# Esperado: 1 (a frase de proveniência continua lá)
```

## Passo 4 — Validar o documento (build do módulo)

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. Prova FR-013 e SC-005.

## Passo 5 — Comparar o inventário de DTOs

> Este é o **gate novo** desta task. As duas anteriores não geravam código; esta gera.

```bash
cd platform-shared-contracts
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-after.txt

diff /tmp/dto-baseline.txt /tmp/dto-after.txt
```

**Esperado** (FR-014): a **única** diferença é **uma linha adicionada**, correspondente a `ProductImageRequest.java`. Os checksums dos 4 modelos preexistentes devem ser **idênticos** aos do baseline — nenhuma linha alterada, nenhuma removida.

Conferência da contagem e da ausência de APIs geradas:

```bash
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" | wc -l
# Esperado: 5

ls contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/api/ 2>/dev/null \
  || echo "OK: nenhum diretório api/ — generateApis=false segue valendo"
```

## Passo 6 — Provar regressão zero no consumidor

```bash
cd hb-catalog-service
mvn -B verify
```

**Esperado**: `BUILD SUCCESS` com a **mesma contagem do Passo 1b**. Prova FR-015 e SC-007.

> Aqui este passo **deixa de ser formalidade**: nas tasks anteriores nada era gerado, então "não quebrou" era quase tautológico. Agora existe uma classe nova no artefato, e este é o gate que confirma que ela compila e não colide com nada existente.

## Passo 7 — Conferência do resultado contra os requisitos

| Verificação | Requisito |
|---|---|
| `requestBody` com `required: true`, `description`, e `application/json` **como única** mídia | FR-001 |
| Schema referenciado por `$ref`, sem inline | FR-002 |
| `requestBody` entre a `description` da operação e `responses` | FR-003 |
| `ProductImageRequest` existe, `type: object`, com `description` sobre URL-only | FR-004 |
| `required: [url]` e **exatamente uma** propriedade | FR-005 |
| `url` com `type: string`, `format: uri`, `minLength: 1`, `maxLength: 2048`, `description` | FR-006 |
| `description` do schema declara HTTPS + razão; **não** há `pattern` | FR-007 |
| `'400'` declarado, nomeando as quatro causas | FR-008 |
| `'400'` **sem** `content` | FR-009 |
| **Não** há `'409'` | FR-010 |
| O corpo descreve **uma** referência (sem array) | FR-011 |
| Diff sem linhas `-` (Passo 3) | FR-012 |
| Build do módulo verde (Passo 4) | FR-013 |
| Inventário: +1 DTO, 4 preexistentes com checksum idêntico (Passo 5) | FR-014 |
| `mvn -B verify` verde na contagem do Passo 1b (Passo 6) | FR-015 |
| **Não** há `additionalProperties: false` | FR-016 |

## Fora do escopo desta validação

Nenhuma requisição HTTP é exercida: a operação tem endereço, verbo e agora corpo declarados, mas segue sem implementação (cadeia T-005) e sem corpo de resposta (T-002-4). O DTO gerado existe no artefato mas ainda não é referenciado por nenhuma classe do serviço.
