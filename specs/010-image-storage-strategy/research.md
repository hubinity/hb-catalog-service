# Phase 0 — Research: Estratégia de armazenamento de imagens de produto

**Feature**: `010-image-storage-strategy` · **Task**: `T-002-5` · **Date**: 2026-07-25

Nenhum `NEEDS CLARIFICATION` restou no Technical Context — a única incógnita real da task (**qual** estratégia, já que o `source_reference` é "NÃO ESPECIFICADO NO PRD") foi levada ao usuário antes da spec e resolvida. As demais decisões abaixo foram tomadas por evidência no repositório.

---

## D1 — Qual estratégia de armazenamento ratificar

**Decision**: **URL-only reference strategy**. As imagens são hospedadas fora do sistema; o catálogo persiste apenas URLs absolutas e nunca recebe, armazena ou serve bytes.

**Rationale**: decisão do usuário, tomada na confirmação do pipeline em 2026-07-25. O fator determinante foi que as tasks `T-002-2`, `T-002-3` e `T-002-4` — todas `done` — **já congelaram essa estratégia no contrato**: corpo JSON com uma única `url` (`40dd8e0`), ausência deliberada de `Location` (`854c02f`), resposta como coleção de URIs (`4fa9056`). Ratificar é a única opção que não reabre task concluída.

**Alternatives considered**:

| Alternativa | Por que rejeitada |
|---|---|
| Object storage gerenciado pelo serviço (multipart → Supabase/S3) | Reabriria `T-002-3` (corpo JSON → multipart) e `T-002-4` (schema de resposta), ambas `done`. Casaria com a redação de `T-005-3`, mas ao custo de desfazer três commits de contrato. |
| Upload por URL pré-assinada (presigned PUT) | Preserva `T-002-3`/`T-002-4`, mas exige **operação nova** no contrato (`POST …/images/upload-url`) que nenhuma task do tracker cobre — inventaria escopo em uma task marcada `decomposition_allowed: false`. |

---

## D2 — Onde declarar a estratégia no documento

**Decision**: declaração canônica em **`info.description`** (nível do documento). O Path Item de imagens **remete** a ela em uma frase, sem redeclarar seu conteúdo.

**Rationale**: a estratégia não governa apenas o path de imagens. `T-002-6` (no tracker, `refined`) adiciona `images` ao schema `Product`, servido por `/api/v1/products/{id}` — **outro path**. Uma declaração presa ao Path Item de imagens seria invisível para quem lê a operação de leitura de produto e encontra um array de URLs sem saber que o serviço não hospeda nada daquilo. `info.description` é o único ponto do documento que alcança ambos os leitores.

Há precedente de forma: `T-001-5` (`68873d5`), ao encerrar a cadeia anterior, colocou a decisão transversal no nível do documento (`components/securitySchemes` + `security` raiz) em vez de repeti-la por operação.

**Alternatives considered**:

| Alternativa | Por que rejeitada |
|---|---|
| Somente no Path Item de imagens | Não alcança o leitor do schema `Product` após `T-002-6` — o principal beneficiário. |
| Extensão `x-image-storage-strategy` | O documento não usa nenhuma extensão `x-`; introduzir uma cria precedente de metadado proprietário sem consumidor. |
| Novo `tag` "images" com `description` | Alteraria agrupamento e exigiria tocar a operação (`tags:`), violando FR-016 por uma decisão puramente redacional. |
| ADR em `docs/adr/` em vez do contrato | A task pede explicitamente a estratégia **em `catalog.yaml`**. Um ADR não é lido por quem consome o contrato gerado. |

---

## D3 — O que a declaração precisa conter para não ser no-op

**Decision**: além de nomear a estratégia, a declaração **deve** afirmar a fronteira de responsabilidade — disponibilidade e ciclo de vida pertencem ao host externo, o catálogo **não verifica** que a URL resolve, e uma referência que deixa de resolver **não é violação de contrato** — e a semântica de remoção (remove-se a referência, nunca um arquivo).

**Rationale**: a frase "armazenamos só a URL" já aparece três vezes no documento. Repeti-la em `info.description` não entregaria informação nova, e a task viraria prosa. A varredura do `catalog.yaml` confirma que a **não-garantia de resolução não está declarada em lugar nenhum** — e é exatamente a suposição que um consumidor faria por padrão (integridade referencial) e que o serviço nunca prometeu. É o conteúdo próprio desta task.

**Alternatives considered**: limitar-se a consolidar o texto existente — rejeitado por não acrescentar nada verificável; o checklist `contract.md` (CHK031) trata esse modo de falha como risco explícito.

---

## D4 — HTTPS: expectativa documentada ou restrição verificável

**Decision**: permanece **expectativa textual**. Nenhum `pattern` é adicionado a `ProductImageRequest.url`.

**Rationale**: três razões independentes convergem. (1) **Escopo**: `ProductImageRequest` é propriedade de `T-002-3`, `done`; alterá-lo reabre task concluída. (2) **Comportamento**: `pattern: '^https://'` gera anotação `@Pattern` no DTO e passa a **rejeitar em runtime** URLs `http://` que hoje o contrato apenas desaconselha — é mudança de comportamento disfarçada de documentação, e alteraria o checksum de um DTO, quebrando o gate de inércia. (3) **Decisão de produto**: se URLs `http://` devem ser recusadas é pergunta que ninguém respondeu; a spec não a responde por conta própria.

Registrada como lacuna com entrada proposta `T-002-8`, `depends_on: [T-002-5]`.

**Alternatives considered**: adicionar `pattern` agora — rejeitado pelos três motivos acima; remover a menção a HTTPS por não ser verificável — rejeitado, pois a orientação é útil ainda que não seja enforçada, e já existe em `ProductImageRequest`.

---

## D5 — Como fechar o escopo da primeira remoção da cadeia

**Decision**: predicado verificável em vez de recomendação — **no diff, toda linha `-` deve pertencer ao bloco `description` do Path Item de imagens** (FR-017). Verificação por inspeção do próprio diff, antes do commit.

**Rationale**: T-002-1, T-002-3 e T-002-4 puderam usar o critério mais forte possível ("nenhuma linha `-`"), porque eram aditivas. Esta task não pode — precisa remover o andaime "completed by T-002-3 and T-002-4". Trocar um critério objetivo por "seja cuidadoso" seria regressão de rigor justo na única task da cadeia que remove algo. O predicado preserva objetividade admitindo exatamente a remoção autorizada.

**Alternatives considered**: contar linhas removidas (frágil a reflow do bloco literal `|`); confiar na revisão humana (não reproduzível).

---

## D6 — Qual gate de geração aplicar, dado que nada é gerado

**Decision**: **igualdade de checksum** nos 6 DTOs de `contracts-catalog`, com baseline capturado **nesta execução** em `/tmp/dto-baseline-010.txt`, antes da edição.

**Rationale**: T-002-3 e T-002-4 usaram "exatamente um DTO novo, checksum idêntico nos preexistentes". Aqui não há DTO novo, então o gate colapsa naturalmente para a forma mais forte: **nenhum arquivo muda**. Isso é mais fácil de verificar e mais difícil de satisfazer por acidente — qualquer toque inadvertido em schema aparece imediatamente.

O nome do arquivo carrega o número da feature por decisão herdada de `T-002-4` (FR-015 de lá): `target/` é regenerado a cada build, então um baseline de outra execução não descreve o estado de partida desta, e um nome genérico convidaria à reutilização acidental.

**Alternatives considered**: comparar apenas nomes de arquivo (não detectaria alteração de conteúdo de um DTO existente); pular o gate por "só mudei texto" — rejeitado: é precisamente a hipótese que o gate existe para comprovar, e `description` de schema **sim** entra no Javadoc gerado, então a inércia não é óbvia a priori.

---

## D7 — Reconciliação com `T-005-3`

**Decision**: **registrar, não corrigir.** A spec propõe substituição concreta para `T-005-3`; `TASKS.json` permanece intocado.

**Rationale**: convenção firme da cadeia — nenhuma spec (005, 008, 009) editou o tracker, mesmo ao identificar lacunas; todas propuseram entrada e deixaram a decisão ao usuário. `T-005-3` está em fase `api`, fora do escopo declarado de uma task `contracts`. A contradição já existia desde `T-002-3` (que fixou corpo JSON); esta task apenas a torna **detectável**, ao promover a estratégia a decisão declarada.

**Alternatives considered**: editar `TASKS.json` — rejeitado por convenção da cadeia e porque o pipeline não tem mandato para isso; silenciar — rejeitado: a contradição chegaria à implementação da cadeia T-005 e custaria retrabalho.

---

## Consolidação

Nenhum `NEEDS CLARIFICATION` pendente. Todas as decisões são rastreáveis a (a) escolha explícita do usuário — D1; (b) evidência no repositório — D2, D5, D6, D7; ou (c) delimitação de escopo declarada na spec — D3, D4.
