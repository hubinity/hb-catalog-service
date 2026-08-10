# Quickstart: Validação da entrega T-001-1 (path canônico de saldo de estoque)

**Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md) · **Data model**: [data-model.md](./data-model.md)

## Pré-requisitos

- Repo irmão `platform-shared-contracts` clonado ao lado deste (`../platform-shared-contracts`)
- Java 21 + Maven 3.9+ (nenhum Docker necessário)

## Mudança-alvo (referência)

Única edição: adicionar à seção `paths:` de `contracts-catalog/openapi/catalog.yaml`, após `/api/v1/products/{id}`, a entrada:

```yaml
  /api/v1/products/{productId}/stock:
    summary: Stock balance for a product (canonical path)
    description: |
      Canonical read address for a product's on-hand stock snapshot
      (see components/schemas/StockItem). The GET operation, its
      productId parameter, response schema, and authorization are
      declared by follow-up contract tasks (T-001-2..T-001-5); this
      entry fixes the address so service and consumers converge on it.
```

Sem operações neste momento (decisão R2); schemas e paths existentes intocados (invariantes em [data-model.md](./data-model.md)).

## Validação (na ordem)

```bash
cd ../platform-shared-contracts

# 1. FR-001/FR-002 — o path existe, com template {productId}
grep -n "/api/v1/products/{productId}/stock:" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha encontrada

# 2. FR-005 — unicidade: nenhum outro path de saldo
grep -cn "stock:" contracts-catalog/openapi/catalog.yaml
# esperado: 1

# 3. FR-003 — mudança aditiva: diff toca só a nova entrada
git diff -- contracts-catalog/openapi/catalog.yaml
# esperado: somente linhas adicionadas (+), nenhuma linha removida (-)

# 4. FR-004 / SC-002 — autoridade de validação (R3): o build do módulo parseia o spec
mvn -B -DskipTests install
# esperado: BUILD SUCCESS (parse do swagger-parser + geração DTO-only passam)
```

### Contingência (só se o passo 4 falhar por path sem operação)

Improvável (R2). Se o parse rejeitar o Path Item vazio, aplicar FR-006: incluir no **mesmo commit** a operação GET mínima de T-001-2 e registrar no PR que T-001-1 permanece como a decisão de endereço.

## Verificação de consumo (opcional, prova SC-003 no futuro)

```bash
cd ../hb-catalog-service
mvn -B verify   # continua verde — nenhum código do serviço depende do path ainda
```

## Resultado esperado

- `catalog.yaml` válido com 2 paths (`/api/v1/products/{id}` intacto + novo path de saldo)
- Artefato `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT` reinstalado no repositório Maven local
- Cadeia T-001-2 → T-001-5 desbloqueada (SC-004)
