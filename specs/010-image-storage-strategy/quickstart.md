# Phase 1 — Quickstart / Validation Guide

**Feature**: `010-image-storage-strategy` · **Task**: `T-002-5` · **Date**: 2026-07-25

Guia de validação executável. A edição acontece no **repo irmão** `platform-shared-contracts`; `hb-catalog-service` é apenas onde a regressão é medida — nenhum arquivo dele é alterado.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Repos lado a lado sob `…/hubinity/`
- **Sem Docker** — todos os gates rodam offline (Failsafe não é acionado)

```bash
export HUB=~/"Área de Trabalho/workspace/projects/active/hubinity"
export CONTRACTS="$HUB/platform-shared-contracts"
export SERVICE="$HUB/hb-catalog-service"
```

---

## Gate 0 — Baseline, ANTES de qualquer edição

> **Obrigatório nesta execução.** Reaproveitar `/tmp/dto-baseline-009.txt` (ou qualquer arquivo de execução anterior) é **proibido** por FR-019: `target/` é regenerado a cada build, então um inventário de outra execução não descreve o estado de partida desta. O nome carrega o número da feature justamente para tornar a reutilização acidental estruturalmente improvável.

```bash
cd "$CONTRACTS"
mvn -B -pl contracts-catalog -am -DskipTests install

find contracts-catalog/target/generated-sources -name '*.java' \
  | sort | xargs sha256sum > /tmp/dto-baseline-010.txt

wc -l < /tmp/dto-baseline-010.txt        # esperado: 6
cut -d' ' -f3- /tmp/dto-baseline-010.txt | xargs -n1 basename
```

**Esperado** — exatamente 6 DTOs:

```text
Category.java  Product.java  ProductImageRequest.java  ProductImageResponse.java
StockItem.java  StockMovement.java
```

Baseline de regressão do consumidor, também **medido agora**, nunca herdado de spec anterior (FR-020):

```bash
cd "$SERVICE"
mvn -B verify 2>&1 | tee /tmp/verify-before-010.log | tail -5
grep -E 'Tests run:.*Failures' /tmp/verify-before-010.log | tail -1
```

Anote a contagem `Tests run: N`. É contra **este** número que o pós-edição será comparado.

---

## Edição

Aplicar os dois blocos de `contracts/image-storage-strategy.yaml` em
`$CONTRACTS/contracts-catalog/openapi/catalog.yaml`:

1. **`info.description`** — acrescentar o parágrafo da estratégia; as 2 linhas existentes permanecem intactas acima dele.
2. **`description` do Path Item `/api/v1/products/{productId}/images`** — reescrever para o estado final; `summary` intocado.

Nada mais. Nenhum schema, operação, parâmetro ou desfecho.

---

## Gate 1 — Documento continua válido

```bash
cd "$CONTRACTS"
mvn -B -pl contracts-catalog -am -DskipTests install
```

**Esperado**: `BUILD SUCCESS`. O build é a autoridade de validade do OpenAPI 3.1 (FR-018) — não há linter separado na cadeia.

---

## Gate 2 — Inércia de geração (o gate endurecido desta task)

T-002-3 e T-002-4 provaram "exatamente 1 DTO novo". Aqui o critério é mais forte: **nada muda**.

```bash
cd "$CONTRACTS"
find contracts-catalog/target/generated-sources -name '*.java' \
  | sort | xargs sha256sum > /tmp/dto-after-010.txt

diff /tmp/dto-baseline-010.txt /tmp/dto-after-010.txt && echo "GATE 2 OK: 6 ↔ 6, checksums idênticos"
```

**Esperado**: `diff` sem saída e a mensagem de OK. Qualquer linha divergente significa que um schema foi tocado — provavelmente `ProductImageRequest` (FR-014/FR-015) — e **reprova a entrega**.

> Por que verificar, se "só mudou `description`": `description` de **schema** e de **propriedade** entra no Javadoc do DTO gerado. A inércia vale para `info.description` e para `description` de Path Item, não para `description` em geral. O gate comprova que as duas tocadas foram só essas.

---

## Gate 3 — Regressão zero no consumidor

```bash
cd "$SERVICE"
mvn -B verify 2>&1 | tee /tmp/verify-after-010.log | tail -5
grep -E 'Tests run:.*Failures' /tmp/verify-after-010.log | tail -1
```

**Esperado**: `BUILD SUCCESS` e `Tests run: N` **igual** ao de Gate 0, com `Failures: 0, Errors: 0`.

---

## Gate 4 — Escopo do diff (o gate próprio da primeira remoção da cadeia)

```bash
cd "$CONTRACTS"
git diff -- contracts-catalog/openapi/catalog.yaml
git diff -U0 -- contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
```

**Esperado** (FR-017): as únicas linhas `-` são as do bloco `description` do Path Item de imagens — em particular a frase de andaime *"The POST operation is declared; its request body and response body are completed by T-002-3 and T-002-4."*

Reprova se aparecer qualquer remoção fora desse bloco: em `info.description` (FR-007), no `summary` (FR-013), em schemas (FR-014) ou em operações (FR-016).

> **Por que `grep '^-' | grep -v '^---'` e não `grep '^-[^-]'`**: uma linha em branco removida aparece no diff como `-` sozinho, que `^-[^-]` **não** casa — o gate deixaria passar a remoção. O encadeamento acima captura a linha vazia e ainda descarta o cabeçalho `--- a/…`. É a forma usada em `T-002-4`, mantida aqui de propósito: numa task que remove linhas, o gate não pode ter ponto cego.

```bash
# Nenhum arquivo além do catalog.yaml pode aparecer:
git status --porcelain
```

---

## Gate 5 — Conteúdo exigido presente

Checagem textual dos requisitos de conteúdo, não de forma:

```bash
cd "$CONTRACTS/contracts-catalog/openapi"

# FR-001 · estratégia nomeada
grep -q 'URL-only reference strategy' catalog.yaml && echo "FR-001 ok"

# FR-004 · fronteira de responsabilidade — o conteúdo NOVO da task
grep -q 'does not' catalog.yaml && grep -q 'not a' catalog.yaml && echo "FR-004 conferir manualmente no parágrafo"

# FR-010 · nenhuma menção a tasks na description do Path Item
grep -n 'T-002-3\|T-002-4' catalog.yaml || echo "FR-010 ok: sem referência a tasks pendentes"

# FR-015 · nenhum pattern introduzido
grep -n 'pattern:' catalog.yaml || echo "FR-015 ok: nenhum pattern no documento"
```

**Esperado**: `FR-010 ok` e `FR-015 ok`. Os itens de conteúdo (FR-002 a FR-006) são verificados por leitura do parágrafo contra `contracts/image-storage-strategy.yaml` — `grep` isolado não prova afirmação semântica, e fingir que prova seria pior que ler.

---

## Resumo dos gates

| Gate | Prova | Requisito |
|---|---|---|
| 0 | Baseline capturado **nesta execução** | FR-019, FR-020 |
| 1 | OpenAPI 3.1 válido | FR-018 |
| 2 | 6 ↔ 6, checksums idênticos | FR-019 |
| 3 | `mvn -B verify` verde, mesma contagem | FR-020 |
| 4 | Toda linha `-` no bloco autorizado | FR-017, FR-007, FR-013, FR-014, FR-016 |
| 5 | Conteúdo normativo presente; sem `pattern`; sem menção a tasks | FR-001–FR-006, FR-010, FR-015 |

## Fora deste guia

Não há teste de comportamento a rodar em `hb-catalog-service`: a mudança é textual e não gera código (Princípio III, justificado em `plan.md`). O serviço aparece aqui apenas como **medidor de regressão**, nunca como alvo de edição.
