# Quickstart: Validação da entrega T-001-2 (operação GET de saldo de estoque)

**Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md) · **Data model**: [data-model.md](./data-model.md) · **Fragmentos-alvo**: [contracts/stock-balance-get.yaml](./contracts/stock-balance-get.yaml)

## Pré-requisitos

- Branch `feature/stock-balance-path` ativa em `platform-shared-contracts` e `hb-catalog-service` (herdada da feature 001)
- Java 21 + Maven 3.9+ (sem Docker)
- T-001-1 entregue (Path Item presente no `catalog.yaml`)

## Mudança-alvo (referência)

Aplicar os 2 fragmentos de [contracts/stock-balance-get.yaml](./contracts/stock-balance-get.yaml) em `contracts-catalog/openapi/catalog.yaml`: tag `stock` na seção `tags` e operação `get` + description reescrita no Path Item do saldo. Invariantes pós-edição em [data-model.md](./data-model.md).

## Validação (na ordem)

```bash
cd ../platform-shared-contracts

# 1. FR-001/FR-002 — operação com identidade correta
grep -n "operationId: getStockItemByProductId" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha

# 2. FR-003 — tag declarada e usada (sem tag órfã)
grep -n "name: stock" contracts-catalog/openapi/catalog.yaml
grep -n "tags: \[stock\]" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha cada

# 3. FR-004 — respostas com os textos fixados
grep -n "Current stock balance for the product" contracts-catalog/openapi/catalog.yaml
grep -n "Stock balance not found for the given product" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha cada

# 4. FR-005 — diff restrito ao Path Item do saldo + seção tags
git diff -- contracts-catalog/openapi/catalog.yaml
# esperado: hunks apenas na seção tags e no bloco do Path Item do saldo;
# nenhuma linha de components/schemas ou do path /api/v1/products/{id} alterada

# 5. FR-005/SC-002 — autoridade de validação (herdada: feature 001, R3)
mvn -B -DskipTests install
# esperado: BUILD SUCCESS
```

### Contingência FR-006/R2 (só se o passo 5 falhar por parâmetro não declarado)

Gatilho objetivo: erro de validação citando "missing path parameter 'productId'" (ou equivalente). Ação: aplicar o bloco `parameters` comentado no fim de [contracts/stock-balance-get.yaml](./contracts/stock-balance-get.yaml) dentro da operação, re-rodar o passo 5 e registrar o acionamento como adendo em [research.md](./research.md) (R2). T-001-3 permanece dona da especificação fina do parâmetro.

## Verificação de consumo (regressão zero)

```bash
cd ../hb-catalog-service
mvn -B verify   # esperado: verde, com o artefato reinstalado
```

## Resultado esperado

- `catalog.yaml` com 2 operações (`getProductById` intacta + `getStockItemByProductId`), 2 tags declaradas e usadas
- Artefato `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT` reinstalado localmente
- T-001-3 desbloqueada (SC-004); decisão do 404 único registrada como vinculante para T-004-x
