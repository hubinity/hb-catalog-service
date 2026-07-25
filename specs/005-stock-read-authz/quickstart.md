# Quickstart: Validação da entrega T-001-5 (autorização da leitura de saldo)

**Plan**: [plan.md](./plan.md) · **Research**: [research.md](./research.md) · **Data model**: [data-model.md](./data-model.md) · **Fragmentos-alvo**: [contracts/stock-read-authz.yaml](./contracts/stock-read-authz.yaml)

## Pré-requisitos

- Branch `feature/stock-balance-path` ativa nos dois repos; T-001-4 commitada
- Java 21 + Maven 3.9+ (sem Docker)

## Mudança-alvo (referência)

Aplicar os 3 fragmentos de [contracts/stock-read-authz.yaml](./contracts/stock-read-authz.yaml) em `contracts-catalog/openapi/catalog.yaml`: `securitySchemes/bearerAuth` em `components`, `security` global no nível raiz, description final do Path Item.

## Validação (na ordem)

```bash
cd ../platform-shared-contracts

# 1. FR-001 — securityScheme declarado
grep -n "bearerAuth:" contracts-catalog/openapi/catalog.yaml
# esperado: 2 linhas (definição em securitySchemes + referência no security raiz)
grep -n "scheme: bearer" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha

# 2. FR-002 — security global no nível raiz (coluna 0, fora de components/paths)
grep -n "^security:" contracts-catalog/openapi/catalog.yaml
# esperado: 1 linha

# 3. FR-003 — operação de saldo NÃO tem security próprio
awk '/getStockItemByProductId/,/'\''404'\''/' contracts-catalog/openapi/catalog.yaml | grep -c "security:"
# esperado: 0 (herda o global; nenhum override na operação)

# 4. FR-005 — diff restrito (securitySchemes + security raiz + description do Path Item)
git diff -- contracts-catalog/openapi/catalog.yaml
# esperado antes de T007: hunks apenas em components (securitySchemes), no
# security raiz e na description do Path Item de saldo; schemas/paths/operações
# intactos
# após T007, o working diff fica vazio; validar o escopo commitado:
git show --format= --no-ext-diff HEAD -- contracts-catalog/openapi/catalog.yaml
# esperado: os mesmos 3 hunks restritos

# 5. FR-005/SC-002 — autoridade de validação
mvn -B -DskipTests install
# esperado: BUILD SUCCESS (segurança não afeta geração DTO-only — research R5)
```

## Verificação de consumo (FR-006 — regressão zero)

```bash
cd ../hb-catalog-service
mvn -B verify
# esperado: verde
```

## Fidelidade ao serviço (evidência para o commit — SC-003)

```bash
# Confirmar que o requisito do contrato (autenticado, não admin) espelha o serviço:
grep -n "authenticated()\|hasRole\|permitAll" src/main/java/com/hubinity/catalog/config/SecurityConfig.java
# esperado: /api/** -> authenticated(); reads sem @PreAuthorize (só mutações têm hasRole admin)
```

## Resultado esperado

- Contrato declara `bearerAuth` + `security` global; leitura de saldo exige JWT bearer (sem admin)
- Documento válido; DTOs inalterados; consumidor verde
- **Cadeia T-001 completa** (SC-004): operação de saldo integralmente especificada — endereço, verbo, parâmetro, corpo, autorização
