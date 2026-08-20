# Quickstart — Validação de T-002-2 (Operação POST de registro de imagem)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fragmentos**: [contracts/product-images-post.yaml](./contracts/product-images-post.yaml)

Guia de validação da edição de contrato. **Não** contém código de implementação — esta task não tem nenhum.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Os dois repos irmãos lado a lado: `platform-shared-contracts/` e `hb-catalog-service/`
- Branch `feature/stock-balance-path` ativa em ambos
- T-002-1 concluída (commit `fd9b905` no repo de contratos) — o Path Item de imagens deve existir
- **Docker não é necessário** — nenhum teste de integração é exercido

## Passo 1 — Registrar o baseline ANTES de editar

```bash
cd hb-catalog-service
mvn -B verify 2>&1 | grep -E "Tests run:.*Failures" | tail -1
```

Anote a contagem. Ela é o baseline do Passo 6 — **medida agora**, não herdada de execuções anteriores.

## Passo 2 — Aplicar os dois fragmentos

De [`contracts/product-images-post.yaml`](./contracts/product-images-post.yaml), em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml`:

1. **Fragmento 1** — substituir o bloco `description:` do Path Item `/api/v1/products/{productId}/images`. `summary` e `parameters` ficam intocados.
2. **Fragmento 2** — inserir o bloco `post:` após `parameters`, como último elemento do Path Item.

Não copiar as linhas de comentário do arquivo de fragmentos.

## Passo 3 — Provar que a mudança está delimitada

> Esta é a diferença crítica em relação a T-002-1: aqui **existem** linhas removidas legitimamente. O critério não é "zero remoções", e sim "toda remoção pertence ao `description:` do Path Item de imagens".

```bash
cd platform-shared-contracts
git diff --stat contracts-catalog/openapi/catalog.yaml
git diff contracts-catalog/openapi/catalog.yaml
```

**Esperado**: exatamente **1 arquivo alterado**. Inspecione cada linha `-` do diff e confirme que **todas** pertencem ao bloco `description:` do Path Item de imagens.

Verificação dirigida de que nada mais foi tocado:

```bash
# Os dois GETs existentes devem estar intactos:
git diff -U0 contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -vE '^---' \
  | grep -E 'getProductById|getStockItemByProductId|securitySchemes|^-\s*(summary|parameters):'
# Esperado: NENHUMA saída

# A frase falsa deve ter desaparecido do documento:
grep -c "declared by the remaining T-002 tasks" contracts-catalog/openapi/catalog.yaml
# Esperado: 0
```

## Passo 4 — Validar o documento (build do módulo)

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. Prova FR-015 e SC-005 — o documento segue sendo um OpenAPI 3.1 válido e parseável.

## Passo 5 — Confirmar que declarar uma operação não gera código

```bash
cd platform-shared-contracts
ls contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/
ls contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/api/ 2>/dev/null \
  || echo "OK: nenhum diretório api/ — operações não geram código"
```

**Esperado**: os mesmos 4 DTOs (`Category`, `Product`, `StockItem`, `StockMovement`) e **nenhum** diretório `api/`.

Por quê: o pom pai fixa `generateApis=false` / `generateModels=true` (ADR 0002) — só schemas geram código, e esta task não adiciona schema (o `content` do `201` é T-002-4). É a evidência que sustenta o Constitution Check do Princípio III. **Este passo é mais informativo aqui do que em T-002-1**: lá o path não tinha operação, então "nada gerado" era quase trivial; aqui declara-se uma operação de verdade e continua não gerando nada.

## Passo 6 — Provar regressão zero no consumidor

```bash
cd hb-catalog-service
mvn -B verify
```

**Esperado**: `BUILD SUCCESS`, com a **mesma contagem do Passo 1**. Prova FR-016 e SC-006.

> O `install` do Passo 4 é pré-requisito: sem ele o consumidor compila contra o artefato antigo e o teste não prova nada.

## Passo 7 — Conferência do resultado contra os requisitos

| Verificação | Requisito |
|---|---|
| Bloco `post` existe, após `parameters`, como último elemento do Path Item | FR-001 |
| `operationId: addProductImage` (único), `summary`, `tags: [products]` | FR-002 |
| A operação **não** redeclara `productId` | FR-003 |
| `'201'` declarado, com `description` e **sem** `content` | FR-004 |
| A `description` do `201` registra a ausência de `Location`; **não** há bloco `headers` | FR-005 |
| `'403'` declarado, atribuindo a causa à falta da role `admin` | FR-006 |
| A `description` da operação afirma a exigência de role `admin` | FR-007 |
| `'404'` declarado para produto inexistente | FR-008 |
| **Não** há `'400'` nem `'401'` | FR-009 |
| **Não** há `requestBody` | FR-010 |
| **Não** há `security` na operação | FR-011 |
| **Não** há parâmetro de header `Idempotency-Key` | FR-012 |
| `description` do Path Item substituída; frase antiga ausente (Passo 3) | FR-013 |
| Toda linha removida pertence ao `description:` do Path Item (Passo 3) | FR-014 |
| Build do módulo verde (Passo 4) | FR-015 |
| `mvn -B verify` verde com a contagem do Passo 1 (Passo 6) | FR-016 |

## Fora do escopo desta validação

Nenhuma requisição HTTP é exercida: a operação está declarada no contrato, mas não implementada (cadeia T-005) e sem corpo definido (T-002-3/T-002-4). Validação de comportamento — inclusive a aplicação real do `403` por `@PreAuthorize` — entra na cadeia T-005.
