# Phase 1 — Quickstart / Validation Guide

**Feature**: `011-product-schema-images` · **Task**: `T-002-6` · **Date**: 2026-07-26

Guia de validação executável. A edição acontece no **repo irmão** `platform-shared-contracts`; `hb-catalog-service` é apenas onde a regressão é medida — nenhum arquivo de produção dele é alterado.

## Pré-requisitos

- Java 21 + Maven 3.9+
- Repos lado a lado sob `…/hubinity/`
- **Sem Docker** — todos os gates rodam offline

```bash
export HUB=~/"Área de Trabalho/workspace/projects/active/hubinity"
export CONTRACTS="$HUB/platform-shared-contracts"
export SERVICE="$HUB/hb-catalog-service"
```

---

## Gate 0 — Baseline, ANTES de qualquer edição

> **Obrigatório nesta execução** (FR-016). Reaproveitar `/tmp/dto-baseline-010.txt` ou qualquer arquivo anterior é **proibido**: `target/` é regenerado a cada build, então um inventário de outra execução não descreve o estado de partida desta.

```bash
cd "$CONTRACTS"
mvn -B -pl contracts-catalog -am -DskipTests install

find contracts-catalog/target/generated-sources -name '*.java' \
  | sort | xargs sha256sum > /tmp/dto-baseline-011.txt

wc -l < /tmp/dto-baseline-011.txt        # esperado: 6
```

Confirmar que `Product.java` **ainda não** tem o campo — é o que a edição vai introduzir:

```bash
grep -c 'images' contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/Product.java
# esperado: 0
```

Baseline de regressão do consumidor, também medido agora (FR-018):

```bash
cd "$SERVICE"
mvn -B verify 2>&1 | tee /tmp/verify-before-011.log | tail -5
grep -E 'Tests run:.*Failures' /tmp/verify-before-011.log | tail -1
```

Anote a contagem `Tests run: N`.

---

## Edição

Aplicar o bloco de `contracts/product-images-property.yaml` em
`$CONTRACTS/contracts-catalog/openapi/catalog.yaml`: acrescentar a propriedade `images` ao fim de `components/schemas/Product/properties`, após `updatedAt`.

**Nada mais.** `required` não é tocada; as nove propriedades preexistentes não são tocadas; nenhum outro schema, path ou operação.

---

## Gate 1 — Aditividade e escopo do diff

```bash
cd "$CONTRACTS"
git diff -- contracts-catalog/openapi/catalog.yaml
git diff -U0 -- contracts-catalog/openapi/catalog.yaml | grep '^-' | grep -v '^---'
```

**Esperado**: o segundo comando **sem nenhuma saída** — zero remoções (FR-014).

> Encadeamento `grep '^-' | grep -v '^---'`, e **não** `grep '^-[^-]'`: uma linha em branco removida aparece como `-` sozinho, que o segundo padrão não casa. Defeito C1, encontrado pelo `/speckit-analyze` da spec 010.

Confirmar que só o `catalog.yaml` mudou, e que `required` seguiu intacta:

```bash
git status --porcelain
git diff -- contracts-catalog/openapi/catalog.yaml | grep -c 'categoryId'   # esperado: 0
```

**Escopo das adições** — numa task estritamente aditiva, o risco não são remoções (já cobertas acima), e sim **adições fora do alvo**: acrescentar `maxLength` a `name`, por exemplo, não remove nada e passaria por todos os outros gates, inclusive o de geração (que já espera `Product.java` diferente).

```bash
git diff -U0 -- contracts-catalog/openapi/catalog.yaml | grep '^+' | grep -v '^+++'
```

**Esperado** (FR-007): **todas** as linhas exibidas pertencem ao bloco `images` — a chave `images:`, sua `description`, seus `items` e as três restrições. Qualquer linha adicionada fora desse bloco reprova, ainda que o diff não contenha remoção alguma.

---

## Gate 2 — Documento válido

```bash
cd "$CONTRACTS"
mvn -B -pl contracts-catalog -am -DskipTests install
```

**Esperado**: `BUILD SUCCESS` (FR-015). O build é a autoridade de validade do OpenAPI 3.1 — não há linter separado na linhagem.

---

## Gate 3 — Geração dirigida: quantos e quais mudaram

```bash
cd "$CONTRACTS"
find contracts-catalog/target/generated-sources -name '*.java' \
  | sort | xargs sha256sum > /tmp/dto-after-011.txt

# Contagem inalterada: 6 ↔ 6, nenhum nome novo
diff <(cut -d' ' -f3- /tmp/dto-baseline-011.txt) <(cut -d' ' -f3- /tmp/dto-after-011.txt) \
  && echo "OK: mesmos 6 arquivos, nenhum criado ou removido"

# Exatamente UM checksum diferente, e é Product.java
diff /tmp/dto-baseline-011.txt /tmp/dto-after-011.txt | grep '^[<>]' | grep -c 'Product.java'  # esperado: 2 (< e >)
diff /tmp/dto-baseline-011.txt /tmp/dto-after-011.txt | grep '^[<>]' | grep -vc 'Product.java' # esperado: 0
```

**Esperado**: mesmos seis nomes, e a **única** linha divergente é a de `Product.java` (FR-016). Qualquer outro arquivo divergente reprova — indica que um schema fora do escopo foi tocado.

---

## Gate 4 — Geração dirigida: **o que** mudou dentro

> Gate próprio desta task. Checksum diferente prova que algo mudou, não **o quê** — sem este passo, um erro que alterasse `Product.java` por outro motivo passaria pelo Gate 3.

```bash
cd "$CONTRACTS"
P=contracts-catalog/target/generated-sources/openapi/src/main/java/com/hubinity/contracts/catalog/dto/Product.java

grep -n 'images' $P        # esperado: campo, getter/setter e/ou anotações Jackson
```

**Esperado** (FR-017): `Product.java` declara o campo `images` como coleção de `String`. Reprova se o checksum mudou mas o campo não aparece.

---

## Gate 5 — Regressão no consumidor

```bash
cd "$SERVICE"
mvn -B verify 2>&1 | tee /tmp/verify-after-011.log | tail -5
grep -E 'Tests run:.*Failures' /tmp/verify-after-011.log | tail -1
```

**Esperado**: `BUILD SUCCESS` e `Tests run: N` igual ao de Gate 0, com `Failures: 0, Errors: 0`.

> **Este gate é fraco por construção — registre isso no relatório (FR-018).** Nenhum arquivo em `$SERVICE/src/` importa `com.hubinity.contracts`; o serviço usa DTOs próprios em `api/dto/`. O build passa **porque nada consome a classe alterada**, não porque a mudança é compatível com o uso. O gate ainda prova que o artefato instala e compila no consumidor — mas não mais que isso, e relatá-lo como prova de compatibilidade seria falso.
>
> Confirmação da premissa: `grep -rn 'com\.hubinity\.contracts' "$SERVICE/src/"` → sem saída.

---

## Gate 6 — Conteúdo exigido presente

```bash
cd "$CONTRACTS/contracts-catalog/openapi"

# ── (a) FR-001/FR-002 · a propriedade EXISTE e é array ─────────────────────
# Asserção positiva PRIMEIRO: sem ela os checks seguintes passam a vazio.
BLK=$(sed -n '/^    Product:/,/^    Category:/p' catalog.yaml \
      | sed -n '/^        images:/,/^          items:/p')

[ -n "$BLK" ] || { echo "FALHOU (FR-001): propriedade images ausente"; false; }
echo "$BLK" | grep -q 'type: array' \
  || { echo "FALHOU (FR-002): images não declara type: array"; false; }
echo "FR-001/FR-002 ok: images existe e é array"

# ── (b) FR-006 · images NÃO entrou em required ─────────────────────────────
sed -n '/^    Product:/,/^    Category:/p' catalog.yaml \
  | sed -n '/required:/,/properties:/p' | grep -q 'images' \
  && { echo "FALHOU (FR-006): images está em required"; false; } \
  || echo "FR-006 ok"

# ── (c) FR-003 · conjunto FECHADO, por equivalência literal ────────────────
# Compara os itens das DUAS coleções. Isto implementa o requisito real
# ("exatamente as de ProductImageResponse.images"); uma lista de palavras
# proibidas só cobriria as que alguém lembrou de listar.
items_of() {  # $1 = nome do schema. `items:` está a 10 espaços nos dois schemas.
  sed -n "/^    $1:/,/^    [A-Z][A-Za-z]*:\$/p" catalog.yaml \
    | sed -n '/^          items:/,/^        [a-z]/p' \
    | grep -E '^\s+(type|format|maxLength|minLength|pattern|enum|default|uniqueItems|title|examples):' \
    | sed 's/^[[:space:]]*//' | sort
}

MOLDE=$(items_of ProductImageResponse)
ALVO=$(items_of Product)

# Guarda obrigatória: sem ela, dois vazios "batem" e o diff passa — o mesmo
# falso positivo que (a) existe para impedir.
[ -n "$MOLDE" ] || { echo "FALHOU: molde vazio — extração quebrada, não confie no diff"; false; }
[ -n "$ALVO" ]  || { echo "FALHOU (FR-001): items de Product.images não encontrados"; false; }

diff <(echo "$ALVO") <(echo "$MOLDE") \
  && echo "FR-003 ok: conjunto fechado, idêntico a ProductImageResponse.images" \
  || { echo "FALHOU (FR-003): restrições divergem do molde"; false; }
```

**Esperado** para `$MOLDE` e `$ALVO`, ambos idênticos (verificado contra o arquivo real):

```text
format: uri
maxLength: 2048
type: string
```

**Esperado**: as três mensagens `ok`. A checagem (c) é **allowlist por equivalência**, não blocklist: qualquer palavra-chave acrescentada — inclusive uma que ninguém pensou em proibir — faz o `diff` divergir.

> **Por que a asserção positiva vem primeiro (a)**: sem ela, `sed -n '/^        images:/,…'` devolve vazio quando a propriedade não existe, o `grep` de proibidas não casa nada e o gate **imprime OK sobre uma edição jamais aplicada**. Falso positivo confirmado experimentalmente contra o arquivo pré-edição.

Os itens de conteúdo da `description` (FR-004, FR-005) são verificados por **leitura** contra `contracts/product-images-property.yaml` — `grep` não prova afirmação semântica, e fingir que prova seria pior que ler.

---

## Resumo dos gates

| Gate | Prova | Requisito |
|---|---|---|
| 0 | Baseline capturado **nesta execução**; campo ainda ausente | FR-016, FR-018 |
| 1 | Zero remoções; só o `catalog.yaml` mudou | FR-014, FR-007 |
| 2 | OpenAPI 3.1 válido | FR-015 |
| 3 | 6 ↔ 6, **um** checksum diferente, e é `Product.java` | FR-016 |
| 4 | A alteração **é** o campo `images` | FR-017 |
| 5 | `mvn -B verify` verde, mesma contagem — **com a fraqueza declarada** | FR-018 |
| 6 | `required` intacta; conjunto de restrições fechado | FR-003, FR-006, FR-008–FR-010 |

## Fora deste guia

Não há teste de comportamento a rodar em `hb-catalog-service`: a mudança gera um campo em um DTO sem lógica que, neste repositório, **não tem consumidor** (Princípio III, justificado em `plan.md`). O serviço aparece aqui apenas como medidor de regressão.
