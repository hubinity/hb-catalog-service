# Quickstart — Validação de T-002-1 (Path do endpoint de imagens de produto)

**Feature**: [spec.md](./spec.md) · **Plan**: [plan.md](./plan.md) · **Fragmento**: [contracts/product-images-path.yaml](./contracts/product-images-path.yaml)

Guia de validação da edição de contrato. **Não** contém código de implementação — esta task não tem nenhum.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Os dois repos irmãos lado a lado: `platform-shared-contracts/` e `hb-catalog-service/`
- Branch `feature/stock-balance-path` ativa em ambos (herdada da cadeia T-001)
- **Docker não é necessário** — nenhum teste de integração é exercido por esta mudança

## Passo 1 — Aplicar a edição

Inserir o bloco de [`contracts/product-images-path.yaml`](./contracts/product-images-path.yaml) em:

```text
platform-shared-contracts/contracts-catalog/openapi/catalog.yaml
```

sob `paths:`, imediatamente após o Path Item `/api/v1/products/{productId}/stock` e antes de `components:`.

## Passo 2 — Provar que a mudança é estritamente aditiva

```bash
cd platform-shared-contracts
git diff --stat contracts-catalog/openapi/catalog.yaml
git diff contracts-catalog/openapi/catalog.yaml
```

**Esperado** (critério objetivo de FR-007): exatamente **1 arquivo alterado**, e no diff **apenas linhas `+`** — nenhuma linha `-`. Qualquer linha `-` significa que algo preexistente foi alterado, removido ou reindentado, e reprova a entrega.

Verificação dirigida de que os paths existentes seguem intactos:

```bash
git diff -U0 contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
# Esperado: NENHUMA saída
```

## Passo 3 — Validar o documento (build do módulo)

```bash
cd platform-shared-contracts
mvn -B -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. Prova FR-008 — o documento continua sendo um OpenAPI 3.1 válido e parseável pelo `openapi-generator-maven-plugin`.

## Passo 4 — Confirmar que nenhuma classe Java foi gerada a partir do novo path

```bash
cd platform-shared-contracts
ls contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/
```

**Esperado**: exatamente os mesmos modelos de antes (`Product`, `Category`, `StockItem`, `StockMovement`, …) — **nenhum artefato novo**, nenhum relacionado a imagens.

Por quê: o pom pai fixa `generateApis=false` / `generateModels=true` (ADR 0002), então só schemas geram código. Esta task não adiciona schema algum — o Path Item, sem operações, é inerte para o gerador. É esta a evidência que sustenta o Constitution Check do Princípio III (nenhum comportamento de runtime muda ⇒ nenhum teste novo a escrever).

## Passo 5 — Provar regressão zero no consumidor

```bash
cd hb-catalog-service
mvn -B verify
```

**Esperado**: `BUILD SUCCESS`, com a mesma contagem de testes de antes da mudança. Prova FR-009.

> O `install` do Passo 3 é pré-requisito: sem ele o consumidor compila contra o artefato antigo e o teste não prova nada.

## Passo 6 — Conferência do resultado contra os requisitos

| Verificação | Requisito |
|---|---|
| `paths` contém `/api/v1/products/{productId}/images` | FR-001 |
| `parameters` no nível do Path Item, com `productId` / `in: path` / `required: true` / `string` + `uuid` | FR-002 |
| Nome do parâmetro idêntico ao token `{productId}` do endereço | FR-003 |
| `description` declara a semântica URL-only **e** que as operações vêm das tasks restantes de T-002 | FR-004 |
| Nenhum bloco `security` no Path Item | FR-005 |
| Nenhuma operação (`post`, `get`, …), nenhum `requestBody`, nenhum `responses` | FR-006 |
| Diff sem linhas `-` (Passo 2) | FR-007 |
| Build do módulo verde (Passo 3) | FR-008 |
| `mvn -B verify` verde no consumidor (Passo 5) | FR-009 |
| Nenhum `tags` no Path Item | FR-010 |

## Fora do escopo desta validação

Não há endpoint para chamar — nenhuma requisição HTTP é exercida, porque nenhuma operação foi declarada e nada foi implementado no serviço. Validação de comportamento entra quando T-002-2 declarar a operação e a cadeia T-005 a implementar.
