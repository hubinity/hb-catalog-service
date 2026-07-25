# Implementation Plan: Operação POST de registro de imagem de produto

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-product-images-post/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Declarar a operação `post` (`operationId: addProductImage`) no Path Item `/api/v1/products/{productId}/images` de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-2), com identidade (`summary`, `description`, `tags: [products]`) e três desfechos **description-only, sem `content`**: `201` (sucesso, explicitamente **sem** `Location`), `403` (falta da role `admin`) e `404` (produto inexistente). Junto, substituir a `description` do Path Item, que se tornou factualmente falsa ao afirmar que as operações viriam das tasks restantes.

**Esta é a primeira operação de mutação declarada no contrato inteiro** — até aqui o documento só tinha dois GETs. As escolhas viram precedente para todas as mutações futuras do catálogo.

A task **encerra as duas pendências constitucionais** deferidas por T-002-1: Princípio VI (role `admin` → `403` + prosa) e Princípio V (idempotência → não exigida, por evidência).

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` com `generateApis=false`, `generateModels=true`, `generateSupportingFiles=false` (ADR 0002) — **reverificado no pom pai, linhas 114-116, e confirmado empiricamente**: o diretório `.../catalog/api/` **não existe** em `target/generated-sources`, e os DTOs gerados são exatamente 4 (`Category`, `Product`, `StockItem`, `StockMovement`), um por schema. Declarar uma **operação** portanto não gera código algum.

**Storage**: N/A — a estratégia URL-only mantém as imagens fora do sistema; o contrato só registra URLs.

**Testing**: build do módulo (`mvn -B -DskipTests install` em `platform-shared-contracts`) + regressão do consumidor (`mvn -B verify` em `hb-catalog-service`) — autoridade herdada da cadeia

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: **Mudança NÃO estritamente aditiva** — diferente de T-002-1. Duas partes: (1) substituição da `description` do Path Item pelo texto fixado na spec; (2) inserção do bloco `post` após `parameters`. Critério de diff correspondente (FR-014): todas as linhas removidas devem estar contidas no bloco `description:` do Path Item de imagens; nenhuma pode pertencer a outro elemento. Sem `requestBody` (FR-010), sem `content` nos desfechos (FR-004), sem `security` próprio (FR-011), sem `Idempotency-Key` (FR-012), sem `400`/`401` (FR-009).

**Scale/Scope**: 1 arquivo; ~20 linhas novas (bloco `post`) + ~8 linhas substituídas (`description`). Zero arquivos Java em ambos os repos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; o contrato é editado no repo que o possui. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. A coluna de imagens é da cadeia T-003. | ✅ N/A |
| III. Tiered Testing Discipline | **Verificado, não assumido**: com `generateApis=false` e nenhum schema novo (o `content` do `201` é T-002-4), declarar a operação **não gera nem altera uma única classe Java** — comprovado pela ausência do diretório `catalog/api/` em `target/generated-sources` e pela correspondência 1:1 entre os 4 DTOs e os 4 schemas. Nenhum comportamento de runtime muda ⇒ não há tier de teste aplicável, e inventar um seria teatro. Gates: build do módulo + `mvn -B verify` no consumidor. A disciplina incide integralmente na cadeia T-005, que implementa o handler. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento publicado ou modelado. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | **PENDÊNCIA ENCERRADA.** T-002-1 deixou em aberto se esta operação exigiria `Idempotency-Key`. Decidido por evidência: **não exige**. O Princípio V impõe a chave aos POSTs mutantes **de estoque** (movements/reserve/release/commit) — é uma mutação de **produto**, fora do alcance literal. O `IdempotencyFilter` cobre exatamente aqueles 4 paths, e nenhuma mutação de `ProductController` exige a chave; exigi-la aqui seria inconsistente e demandaria alterar o array `PROTECTED`, mudança que nenhuma task agendou. Registrado como decisão, não como silêncio. | ✅ PASS (pendência encerrada) |
| VI. Security & Configuration Hygiene | **PENDÊNCIA ENCERRADA.** T-002-1 deixou em aberto como declarar a exigência de role. Decidido: **`403` declarado + afirmação em prosa na `description` da operação**. O contrato não consegue expressar isso de forma machine-readable — `bearerAuth` é `http`/`bearer`, sem scopes, e as roles do Keycloak chegam por `realm_access.roles`, não por scopes OAuth; nenhuma construção do OpenAPI 3.1 cobre o caso. A declaração **espelha** o Princípio VI em vez de tensioná-lo: o serviço aplicará `@PreAuthorize("hasRole('admin')")` (T-005-2), como já faz em toda mutação de `ProductController`. Autenticação segue herdada da raiz, sem `security` próprio. Nenhum secret entra no documento. | ✅ PASS (pendência encerrada) |
| Technology Constraints | Nenhuma dependência, framework ou infraestrutura nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

**Nota de precedente**: sendo a primeira mutação do contrato, as decisões registradas nas linhas dos Princípios V e VI passam a ser o padrão de referência para as mutações futuras do catálogo. Registrado aqui para que a repetição seja consciente.

## Project Structure

### Documentation (this feature)

```text
specs/007-product-images-post/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── product-images-post.yaml   # Fragmentos-alvo: description substituta + bloco post
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── operation.md     # /speckit-checklist qualidade de requisito (40 itens; 6 falhas → correções na spec)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # ~ description do Path Item /api/v1/products/{productId}/images
                           #   (substituída — texto fixado na spec)
                           # + bloco post (addProductImage), após parameters:
                           #   identidade + desfechos 201/403/404 sem content

# hb-catalog-service/ — NENHUM arquivo de código alterado
# (mvn -B verify prova regressão zero; nenhuma classe é gerada a partir de operações)
```

**Structure Decision**: Padrão da cadeia — edição no repo irmão, artefatos de spec aqui, branch compartilhada. Esta feature é a **segunda** da cadeia T-002 e a que introduz a primeira mutação do contrato.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia. Os três pontos que exigiram argumentação (Princípio III sem testes novos; Princípios V e VI com pendências encerradas) foram resolvidos **dentro** do gate, com evidência verificável, e não constituem violação.

## Constitution Re-Check (pós-Phase 1)

Design finalizado sem elementos novos: dois fragmentos YAML em 1 arquivo, sem schema, sem geração de código. A Phase 1 não introduziu entidade de runtime alguma — `data-model.md` registra apenas elementos de documento. Diferente de T-002-1, esta feature **não deixa pendência constitucional aberta**: as duas herdadas foram encerradas com decisão registrada. **Gate final: PASS.**
