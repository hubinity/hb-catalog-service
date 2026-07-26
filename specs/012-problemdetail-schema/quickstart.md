# Quickstart — Validação de T-002-7-1 (Schema ProblemDetail)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fragmento**: [contracts/problemdetail-schema.yaml](./contracts/problemdetail-schema.yaml)

Guia de validação da edição de contrato. **Não** contém código de implementação.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Repos irmãos lado a lado; branch `feature/stock-balance-path` ativa em ambos
- `T-002-4` concluída — schema `ProductImageResponse` deve existir em `components/schemas`
- **Docker não é necessário**

## Passo 1 — Capturar os dois baselines ANTES de editar

> Mesmo critério de `T-002-3`/`T-002-4`: reaproveitar números de specs anteriores invalidaria a comparação — `target/` é regenerado a cada build.

**1a. Inventário de DTOs gerados** (base do FR-014):

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-baseline-012.txt
cat /tmp/dto-baseline-012.txt
```

**Esperado**: 6 linhas — `Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`.

**1b. Contagem de testes do consumidor** (base do FR-015):

```bash
cd hb-catalog-service
mvn -B verify 2>&1 | grep -E "Tests run:.*Failures" | tail -1
```

Anote a contagem.

## Passo 2 — Aplicar o fragmento

De [`contracts/problemdetail-schema.yaml`](./contracts/problemdetail-schema.yaml), anexar o schema `ProblemDetail` ao final de `components/schemas` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`, após `ProductImageResponse`.

Não copiar as linhas de comentário. **Não tocar em nenhum outro schema, operação ou desfecho.**

## Passo 3 — Provar que a mudança é estritamente aditiva

```bash
cd platform-shared-contracts
git diff --stat contracts-catalog/openapi/catalog.yaml
git diff -U0 contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
# Esperado: NENHUMA saída
```

Confirmar que nenhum desfecho de erro ganhou `content` (FR-011):

```bash
grep -A1 "'404':" contracts-catalog/openapi/catalog.yaml | grep -c "content:"
# Esperado: 0
sed -n "/'400':/,/'404':/p" contracts-catalog/openapi/catalog.yaml | grep -c "content:"
# Esperado: 0
```

## Passo 4 — Validar o documento (build do módulo)

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. Prova FR-013.

## Passo 5 — Comparar o inventário de DTOs

```bash
cd platform-shared-contracts
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" \
  -exec md5sum {} \; | awk '{print $1, substr($2, match($2, /[^/]+$/))}' | sort \
  > /tmp/dto-after-012.txt

diff /tmp/dto-baseline-012.txt /tmp/dto-after-012.txt
```

**Esperado** (FR-014): **uma única linha adicionada**, `ProblemDetail.java`. Checksums dos 6 preexistentes **idênticos** ao baseline.

```bash
find contracts-catalog/target/generated-sources/openapi -path "*/dto/*.java" | wc -l
# Esperado: 7
```

## Passo 6 — Provar regressão zero no consumidor

```bash
cd hb-catalog-service
mvn -B verify
```

**Esperado**: `BUILD SUCCESS` com a **mesma contagem do Passo 1b**. Prova FR-015 e SC-005.

## Passo 7 — Conferência do resultado contra os requisitos

| Verificação | Requisito |
|---|---|
| `ProblemDetail` existe em `components/schemas`, `type: object` | FR-001 |
| `description` afirma que a forma é produzida pelo exception handler global | FR-002 |
| Exatamente cinco propriedades: `type`, `title`, `status`, `detail`, `instance` | FR-003 |
| `type`: `string`/`format: uri`, `description` menciona `about:blank` | FR-004 |
| `title`: `string` | FR-005 |
| `status`: `integer`/`format: int32` | FR-006 |
| `detail`: `string` | FR-007 |
| `instance`: `string`/`format: uri` | FR-008 |
| **Não** há `required` no schema | FR-009 |
| **Não** há `additionalProperties: false` | FR-010 |
| Os cinco desfechos de erro existentes seguem sem `content` (Passo 3) | FR-011 |
| Diff sem linhas `-` (Passo 3) | FR-012 |
| Build do módulo verde (Passo 4) | FR-013 |
| Inventário: +1 DTO, 6 preexistentes com checksum idêntico (Passo 5) | FR-014 |
| `mvn -B verify` verde na contagem do Passo 1b (Passo 6) | FR-015 |

## Fora do escopo desta validação

Nenhum desfecho de erro é exercido nem referenciado: o schema existe isoladamente, para ser referenciado pelas cinco subtarefas seguintes (`T-002-7-2` a `T-002-7-6`). A propriedade de extensão `errors` (erros de validação de campo) não é modelada nem validada aqui (research R3).
