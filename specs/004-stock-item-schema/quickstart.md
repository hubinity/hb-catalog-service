# Quickstart: Validação da entrega T-001-4 (schema de resposta do saldo)

**Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md) · **Data model**: [data-model.md](./data-model.md) · **Fragmentos-alvo**: [contracts/stock-item-schema.yaml](./contracts/stock-item-schema.yaml)

## Pré-requisitos

- Branch `feature/stock-balance-path` ativa nos dois repos; T-001-3 commitada (`4ddfd2c`)
- Java 21 + Maven 3.9+ (sem Docker)

## Mudança-alvo (referência)

Aplicar os 3 fragmentos de [contracts/stock-item-schema.yaml](./contracts/stock-item-schema.yaml) em `contracts-catalog/openapi/catalog.yaml`: schema `StockItem` reescrito, `content` na `'200'`, description do Path Item.

## Validação (na ordem)

```bash
cd ../platform-shared-contracts

# 1. FR-002 — '200' referencia o schema
grep -n "#/components/schemas/StockItem" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha (na '200' da operação de saldo)

# 2. FR-001 — campos novos presentes
for f in available reserved reorderPoint updatedAt; do echo -n "$f: "; grep -c "        $f:" contracts-catalog/openapi/catalog.yaml; done
# esperado: available: 1, reserved: 1, reorderPoint: 1, updatedAt: 2
# (updatedAt = 2 porque o schema Product já possui updatedAt na mesma indentação)

# 3. FR-003 — campos antigos eliminados
grep -cE "quantityOnHand|reorderLevel|lastMovementAt" contracts-catalog/openapi/catalog.yaml
# esperado: 0

# 4. FR-005 — diff restrito (schema StockItem + '200' + description do Path Item)
git diff -- contracts-catalog/openapi/catalog.yaml
# esperado antes de T008: nenhum hunk em Product/Category/StockMovement,
# paths de produto, tags ou parâmetro
# após T008, o working diff fica vazio; validar o escopo commitado:
git show --format= --no-ext-diff HEAD -- contracts-catalog/openapi/catalog.yaml
# esperado: os mesmos 3 hunks restritos

# 5. FR-005/SC-002 — autoridade de validação + regeneração do DTO
mvn -B -DskipTests install
# esperado: BUILD SUCCESS
grep -l "available" contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/StockItem.java
# esperado: arquivo listado (DTO regenerado com campos novos)
```

## Verificação de consumo (FR-006 — obrigatória: breaking change)

```bash
cd ../hb-catalog-service
grep -rn "com.hubinity.contracts.catalog.dto.StockItem" src/ || echo "sem referências (esperado)"
mvn -B verify
# esperado: verde — regressão zero comprovada (SC-003)
```

## Comparação de paridade (evidência para o commit — SC-001)

```bash
# Lado a lado: schema do contrato × record do serviço
sed -n '/StockItem:/,/StockMovement:/p' ../platform-shared-contracts/contracts-catalog/openapi/catalog.yaml
cat src/main/java/com/hubinity/catalog/api/dto/StockItemResponse.java
# esperado: 5 campos coincidentes em nome e tipo equivalente
# (UUID↔uuid, Integer↔int32, Instant↔date-time)
```

## Resultado esperado

- Schema `StockItem` fiel à realidade do serviço; `'200'` completa; divergência histórica (feature 001) e achado L2 (feature 002) encerrados
- DTO `StockItem` regenerado; consumidor verde
- T-001-5 desbloqueada como última pendência estrutural da operação (SC-004)
