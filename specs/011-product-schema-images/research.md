# Phase 0 — Research: Propriedade images no schema Product

**Feature**: `011-product-schema-images` · **Task**: `T-002-6` · **Date**: 2026-07-26

Nenhum `NEEDS CLARIFICATION` restou no Technical Context. Diferente de `T-002-5` — cujo `source_reference` era "NÃO ESPECIFICADO NO PRD" e exigiu decisão do usuário — esta task tem fonte clara (lacuna documentada pela spec 009) e forma ditada por um molde existente no próprio documento. Todas as decisões abaixo são por evidência.

---

## D1 — Forma da propriedade

**Decision**: `type: array`, itens `type: string` / `format: uri` / `maxLength: 2048`, e **nenhuma outra restrição** — conjunto fechado.

**Rationale**: `ProductImageResponse.images` já existe e descreve **a mesma coleção**. Duas representações divergentes do mesmo dado, no mesmo documento, seriam defeito. O fechamento do conjunto (FR-003) não é zelo: sem ele, as proibições nominais de `minItems`/`maxItems`/`readOnly` deixariam livre qualquer outra palavra-chave, e a entrega poderia divergir do molde passando em todos os gates.

**Alternatives considered**:

| Alternativa | Por que rejeitada |
|---|---|
| `$ref` para um schema `ImageUrl` compartilhado | Criaria um **sétimo** schema e, portanto, um DTO novo — mudança de superfície de geração desproporcional a uma propriedade. Nenhuma task cobre isso. |
| Array de objetos (`{url, alt, position}`) | Contradiz a estratégia URL-only ratificada em `T-002-5`: o catálogo guarda só a URL. Exigiria reabrir `T-002-3` e `T-002-4`. |
| Herdar `minLength: 1` de `ProductImageRequest.url` | Ver D3. |

---

## D2 — `images` opcional, fora de `required`

**Decision**: **opcional**. `required` de `Product` permanece `[id, sku, name, price, categoryId, active]`.

**Rationale**: produto sem imagem é o **estado inicial de todo produto** — a URL só entra por chamada posterior a `POST …/images`. Tornar `images` obrigatório quebraria a leitura de qualquer produto ainda não ilustrado, que é a maioria logo após a criação. O schema já pratica opcionalidade em `description`, `createdAt` e `updatedAt`, então a decisão não introduz padrão novo.

**Alternatives considered**: torná-la obrigatória com `default: []` — rejeitado: `default` em schema de resposta descreve comportamento do servidor (própria das cadeias T-003/T-005) e obrigaria o serviço a sempre emitir a chave, decisão que esta task não tem mandato para tomar.

---

## D3 — Ausência de `minLength: 1` nos itens

**Decision**: **não declarar** `minLength`, espelhando `ProductImageResponse.images`.

**Rationale**: `minLength: 1` existe em `ProductImageRequest.url` porque lá é **entrada** — rejeita string vazia submetida pelo cliente. Em `Product.images` é **saída**: restrição em resposta não valida nada, apenas descreve o que o servidor produz. É a mesma razão que dispensa `minItems` (FR-008). A simetria que importa é com a coleção equivalente (`ProductImageResponse.images`), não com a submissão avulsa (`ProductImageRequest.url`).

Esta decisão foi **provocada pelo `/speckit-checklist`**: os itens CHK019 e CHK024 falharam porque a spec original nem explicava a assimetria nem fechava o conjunto de restrições — brecha por onde um implementador zeloso acrescentaria `minLength` "por simetria com a requisição". Corrigido em FR-003 e num *Edge Case* próprio.

**Alternatives considered**: declarar `minLength: 1` por simetria com a requisição — rejeitado pelo argumento acima, e porque divergiria de `ProductImageResponse.images`, alterando o significado da mesma coleção conforme a operação que a devolve.

---

## D4 — Ausência de `readOnly: true`

**Decision**: **não declarar**.

**Rationale**: `readOnly` serve para desambiguar schemas que atuam como corpo de requisição **e** de resposta. Varredura do documento: `Product` é referenciado **exatamente uma vez** (linha 52), no `200` de `getProductById`, e o único `requestBody` do documento usa `ProductImageRequest`. Não há ambiguidade de escrita a resolver. Declará-lo responderia a uma pergunta que o documento não faz e alteraria o DTO gerado sem necessidade — inclusive interferindo no gate de FR-016/FR-017, que espera uma alteração dirigida.

**Alternatives considered**: declarar `readOnly: true` "por precaução" — rejeitado: precaução contra risco inexistente é ruído que custa mudança de artefato gerado. Se um dia `Product` virar corpo de requisição, a decisão se reabre naturalmente.

---

## D5 — Redeclarar a convenção posicional

**Decision**: **sim** — a `description` de `Product.images` afirma que a coleção vem em ordem e que o primeiro elemento é a imagem principal.

**Rationale**: a convenção foi fixada em `T-002-3` e está escrita em `ProductImageResponse`, que descreve a resposta de **outra** operação — a de registro de imagem, que o leitor de produto pode nunca invocar. Sem redeclarar, quem consome apenas `getProductById` recebe um array ordenado sem saber que a ordem significa algo.

Isso **não contradiz** a spec 010, que excluiu deliberadamente a ordenação da declaração de *armazenamento* em `info.description`: lá a exclusão foi por pertinência (ordenação não é assunto de armazenamento), não por proibição de declará-la onde for útil.

**Alternatives considered**: remeter a `ProductImageResponse` — rejeitado: obriga o leitor a navegar até o schema de outra operação para interpretar um campo do que está lendo. O custo é duplicação (registrada como risco deliberado nos *Edge Cases*), e é o menor dos dois.

---

## D6 — Forma do gate de geração

**Decision**: **6 ↔ 6 arquivos, nenhum nome novo, checksum alterado em exatamente um (`Product.java`), idêntico nos outros cinco** — mais confirmação explícita de que a alteração **é** o campo `images` (FR-017). Baseline em `/tmp/dto-baseline-011.txt`, capturado nesta execução, antes da edição.

**Rationale**: a linhagem já usou duas formas — incremento (`T-002-3`, `T-002-4`: exatamente um arquivo novo) e inércia (`T-002-5`: nenhum arquivo muda). Esta é a terceira: **alteração dirigida**. A novidade é que contar arquivos deixa de bastar — um erro que alterasse `Product.java` por outro motivo (ou que alterasse outro DTO) passaria por um gate que só conferisse a contagem. Daí o par FR-016 (quantos e quais mudaram) + FR-017 (o que mudou dentro).

O nome do arquivo carrega o número da feature por decisão herdada: `target/` é regenerado a cada build, então baseline de outra execução não descreve o estado de partida desta.

**Alternatives considered**: só contar arquivos (não detectaria alteração de conteúdo indevida); só conferir que `Product.java` contém `images` (não detectaria dano colateral em outro DTO). Os dois juntos cobrem ambos os lados.

---

## D7 — Peso do gate de regressão do consumidor

**Decision**: executar `mvn -B verify` em `hb-catalog-service` **e registrar explicitamente que o gate é fraco por construção** (FR-018).

**Rationale**: varredura de `hb-catalog-service/src/` não encontra **nenhum** import de `com.hubinity.contracts`. A dependência está no `pom.xml` (linha 175), mas os DTOs gerados não são consumidos — o serviço usa DTOs próprios em `api/dto/`. Logo, o build do consumidor passa **porque ninguém usa a classe**, não porque a mudança é compatível com o uso. Omitir isso transformaria um gate barato em falsa garantia; declará-lo mantém o gate (ele ainda detecta quebra de compilação do artefato) sem inflar o que ele prova.

**Alternatives considered**: dispensar o gate por ser vazio — rejeitado: ele ainda prova que o artefato instala e compila no consumidor, e é o mesmo gate que a linhagem executou nas quatro tasks anteriores; removê-lo quebraria a comparabilidade. Relatá-lo como prova de compatibilidade — rejeitado por ser falso.

---

## D8 — Tratamento das lacunas encontradas

**Decision**: **registrar, não corrigir.** Duas pendências novas: (a) o contrato não declara `GET /api/v1/products` nem nenhum path de `/api/v1/categories`, embora `T-006-1`..`T-006-6` especifiquem ETag sobre eles → entrada proposta `T-002-9`; (b) divergência estrutural `Product` × `ProductResponse` → **ADR**, não task.

**Rationale**: convenção firme da linhagem — nenhuma spec (005, 008, 009, 010) editou `TASKS.json`, mesmo ao identificar lacunas. A distinção entre (a) e (b) é deliberada: (a) é trabalho de contrato delimitável, cabe em task; (b) é decisão de arquitetura ecossistêmica (o serviço passar a consumir DTOs gerados) que **precede** qualquer task e não deve ser decidida por quem escreve uma propriedade de schema.

**Alternatives considered**: propor task também para (b) — rejeitado: produziria uma task cuja primeira atividade seria tomar uma decisão que a task não tem autoridade para tomar.

---

## Consolidação

Nenhum `NEEDS CLARIFICATION` pendente. Todas as decisões são rastreáveis a evidência verificada no repositório (D1, D3, D4, D6, D7), a convenção estabelecida pela linhagem (D5, D8) ou à estrutura do próprio schema (D2). Duas delas — D3 e o fechamento do conjunto em D1 — só existem porque o `/speckit-checklist` reprovou a redação anterior.
