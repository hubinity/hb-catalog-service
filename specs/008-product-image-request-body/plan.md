# Implementation Plan: Corpo de requisição JSON do registro de imagem de produto

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-product-image-request-body/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Declarar o corpo de requisição da operação `addProductImage` em `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-3). Três adições: **(1)** `requestBody` obrigatório, `application/json` exclusivo, referenciando o schema por `$ref`; **(2)** desfecho **`400`** — obrigação **herdada de T-002-2**; **(3)** schema `ProductImageRequest` em `components/schemas`, com a única propriedade `url` (`format: uri`, 1–2048).

**Esta é a primeira task da cadeia T-002 que gera código.** As duas anteriores passaram no Princípio III alegando que nada era gerado; aqui um schema novo produz um DTO, e o argumento precisa ser outro.

Encerra a obrigação do `400` e **realoca** — não redefere — a questão do `409`/duplicata para a cadeia T-003.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` com `generateApis=false`, `generateModels=true` (ADR 0002). **Correspondência verificada empiricamente antes de afirmar**: hoje há **4 schemas ↔ 4 DTOs** (`Category`, `Product`, `StockItem`, `StockMovement`) e nenhum diretório `catalog/api/`. Adicionar um schema leva a **5 ↔ 5**. É esta correspondência 1:1 que torna o efeito da mudança previsível e contável.

**Storage**: N/A no contrato. A decisão de corpo (só `url`) **determina** o armazenamento a jusante: `Product.images[]` como lista de strings ⇒ coluna `text[]` em T-003-2.

**Testing**: build do módulo (`mvn -B -DskipTests install`) + **comparação de inventário de DTOs** (novo gate desta task) + regressão do consumidor (`mvn -B verify`)

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: **Mudança estritamente aditiva** — ao contrário de T-002-2, e voltando ao critério de T-002-1: no diff, **nenhuma** linha aparece como `-`. Mídia `application/json` exclusiva (FR-001). Sem `pattern` na URL (FR-007). Sem `additionalProperties: false` (FR-016). Sem `409` (FR-010). Sem lote (FR-011). Sem `content` no `400` (FR-009). `description` do Path Item **intocada** (decisão 6).

**Scale/Scope**: 1 arquivo; ~25 linhas novas em três pontos. Zero arquivos Java escritos à mão; **1 arquivo Java gerado**.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo que o possui. O DTO gerado vive no artefato de contratos, consumido como dependência — nunca duplicado localmente. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. A coluna `text[]` que esta decisão implica é entregável de T-003-2, com migração própria. | ✅ N/A |
| III. Tiered Testing Discipline | **Argumento reformulado — o das tasks anteriores caducou.** T-002-1 e T-002-2 passaram alegando "nada é gerado"; **aqui há geração**: `ProductImageRequest` vira DTO (4 → 5 modelos). Mas geração ≠ comportamento. O artefato é um **portador de dados sem lógica**, que o serviço **sequer referencia** — a referência é da cadeia T-005. Não existe unidade de comportamento a testar em `hb-catalog-service`, então continua não havendo tier aplicável; o que muda é o **peso do gate**: a regressão do consumidor deixa de ser formalidade e passa a ser a prova de que o DTO novo compila e não colide com nada. Somam-se dois gates: comparação de inventário de DTOs (FR-014) e `mvn -B verify`. | ✅ PASS (justificado, com argumento novo) |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nada de contadores. A decisão de não exigir `Idempotency-Key` foi encerrada em T-002-2 e não é reaberta — declarar um corpo não altera essa análise. | ✅ N/A |
| VI. Security & Configuration Hygiene | Autenticação e role `admin` já declaradas em T-002-2; esta task não as toca. **Nota de superfície de ataque**: o contrato aceita URL arbitrária sem `pattern` nem allow-list de host (decisão do usuário). Isso é aceitável **porque** sob URL-only o serviço nunca busca a URL — ela é apenas armazenada e devolvida. O risco residual (URL apontando para conteúdo indevido, ou `http://` gerando mixed content) é de exibição, mitigável no cliente, e está declarado na `description` do schema. Nenhum secret entra no documento. | ✅ PASS |
| Technology Constraints | Nenhuma dependência, framework ou infraestrutura nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

**Nota de precedente**: primeiro `requestBody` do contrato. Mídia única, schema nomeado com sufixo `Request`, `required: true`, limites explícitos e leitor tolerante passam a ser o padrão de referência para as entradas futuras.

## Project Structure

### Documentation (this feature)

```text
specs/008-product-image-request-body/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── product-image-request.yaml   # Os 3 fragmentos-alvo
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── request-body.md  # /speckit-checklist qualidade de requisito (40 itens; 4 falhas → correções na spec)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado à mão)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + requestBody na operação addProductImage
                           # + desfecho '400' (obrigação herdada de T-002-2)
                           # + schema ProductImageRequest em components/schemas
                           #   (anexado após StockMovement)

# Artefato GERADO (não escrito à mão, não versionado):
# contracts-catalog/target/generated-sources/openapi/.../dto/ProductImageRequest.java
#   → primeiro código produzido pela cadeia T-002

# hb-catalog-service/ — NENHUM arquivo alterado
# (o DTO novo existe no artefato mas ainda não é referenciado; isso é T-005)
```

**Structure Decision**: Padrão da cadeia — edição no repo irmão, artefatos de spec aqui, branch compartilhada. Terceira feature da cadeia T-002.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia. O único ponto que exigiu argumentação nova (Princípio III, agora com código gerado) foi resolvido **dentro** do gate, com a distinção entre *artefato gerado* e *comportamento novo*, e não constitui violação.

## Constitution Re-Check (pós-Phase 1)

Design finalizado: três fragmentos aditivos em 1 arquivo. A Phase 1 confirmou que a única entidade nova é de documento (um schema) e que sua contrapartida em runtime é um DTO gerado sem lógica. Nenhuma pendência constitucional é aberta e a obrigação herdada do `400` é encerrada. **Gate final: PASS.**
