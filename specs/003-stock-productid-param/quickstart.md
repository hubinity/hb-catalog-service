# Quickstart: Validação da entrega T-001-3 (parâmetro productId completo)

**Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md) · **Data model**: [data-model.md](./data-model.md) · **Fragmentos-alvo**: [contracts/productid-param.yaml](./contracts/productid-param.yaml)

## Pré-requisitos

- Branch `feature/stock-balance-path` ativa nos dois repos; T-001-2 commitada (`e32df53`)
- Java 21 + Maven 3.9+ (sem Docker)

## Mudança-alvo (referência)

Aplicar os 2 fragmentos de [contracts/productid-param.yaml](./contracts/productid-param.yaml) em `contracts-catalog/openapi/catalog.yaml`: `description: Product UUID` no parâmetro (fragmento 1 mostra o estado final) e description do Path Item reescrita (fragmento 2).

## Validação (na ordem)

```bash
cd ../platform-shared-contracts

# 1. FR-001 — description presente no parâmetro
grep -c "description: Product UUID" contracts-catalog/openapi/catalog.yaml
# esperado: 2 (getProductById + getStockItemByProductId — paridade atingida)

# 2. FR-001/FR-002 — paridade campo a campo (ratificação)
# Extrair os dois blocos de parameters e comparar estrutura (exceto name):
awk '/parameters:/,/responses:/' contracts-catalog/openapi/catalog.yaml
# esperado: dois blocos com os mesmos 5 campos na mesma ordem
# (name, in, required, description, schema type/format) — evidência para o commit

# 3. FR-003 — description do Path Item atualizada
grep -n "productId parameter are declared" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha (texto novo presente)
grep -c "T-001-3\.\." contracts-catalog/openapi/catalog.yaml
# esperado: 0 (texto antigo "(T-001-3..T-001-5)" removido)

# 4. FR-004 — diff restrito
git diff -- contracts-catalog/openapi/catalog.yaml
# esperado: +1 linha (description do parâmetro) e o bloco de description
# do Path Item reescrito; nenhum outro hunk

# Após T007, o working diff estará vazio; validar o escopo já commitado:
git show --format= --no-ext-diff HEAD -- contracts-catalog/openapi/catalog.yaml
# esperado: o mesmo diff restrito descrito acima

# 5. FR-004/SC-002 — autoridade de validação (herdada)
mvn -B -DskipTests install
# esperado: BUILD SUCCESS (sem contingência prevista — research R4)
```

## Verificação de consumo (regressão zero)

```bash
cd ../hb-catalog-service
mvn -B verify   # esperado: verde
```

## Resultado esperado

- Parâmetro com 5 campos em paridade com `getProductById`; pendência do adendo R2 (feature 002) encerrada (SC-003)
- Artefato `contracts-catalog:0.1.0-SNAPSHOT` reinstalado
- T-001-4 desbloqueada (SC-004) — lembrete L2 (nome `StockItemResponse` × `StockItem`) aguarda o specify dela
