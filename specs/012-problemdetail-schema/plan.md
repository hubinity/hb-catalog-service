# Implementation Plan: Schema ProblemDetail (RFC 7807) no contrato do catálogo

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia) | **Date**: 2026-07-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-problemdetail-schema/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Declarar o schema `ProblemDetail` (RFC 7807) em `components/schemas` de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task `T-002-7-1`). Uma única adição: cinco propriedades (`type`, `title`, `status`, `detail`, `instance`), nenhuma obrigatória, sem `additionalProperties: false`. **Nenhum desfecho de erro é tocado** — isso é escopo de `T-002-7-2` até `T-002-7-6`, já registradas em `TASKS.json`, dependentes desta em cadeia.

**Primeira das seis subtarefas** em que `T-002-7` foi decomposta (`decomposition_allowed: true`). A forma é transcrita diretamente de `org.springframework.http.ProblemDetail`, já usado em runtime por `ApiExceptionHandler` — não há decisão de produto pendente, apenas modelagem de um contrato já implícito no código.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin`, `generatorName: spring`, sem restrição de `modelsToGenerate` — todo schema em `components/schemas` gera uma classe, referenciado ou não. **Contagem reverificada empiricamente antes de afirmar**: hoje **6 schemas ↔ 6 DTOs** (`Category`, `Product`, `StockItem`, `StockMovement`, `ProductImageRequest`, `ProductImageResponse`). Esta task leva a **7 ↔ 7**.

**Storage**: N/A. Nenhuma persistência — é um schema de documento, sem contrapartida de banco.

**Testing**: build do módulo + comparação de inventário de DTOs + regressão do consumidor — o mesmo trio de gates já usado nas tasks anteriores da cadeia T-002, **reproduzido e não assumido** (baseline capturado nesta execução).

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão `platform-shared-contracts`)

**Performance Goals**: N/A

**Constraints**: **Estritamente aditiva** — zero remoções (FR-012). Nenhuma propriedade `required` (FR-009, reflete que a RFC 7807 declara os cinco membros como OPTIONAL). Sem `additionalProperties: false` (FR-010, permite a propriedade de extensão `errors` que o serviço já emite em erros de validação). O schema **não é referenciado** por nenhuma operação nesta task (FR-011 preserva os cinco desfechos de erro existentes como description-only).

**Scale/Scope**: 1 arquivo; ~20 linhas novas, um único bloco. Zero arquivos Java escritos à mão; **1 arquivo Java gerado** (`ProblemDetail.java`).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato editado no repo que o possui. Esta task **avança** o cumprimento do princípio: hoje o contrato não modela a forma RFC 7807 que o princípio exige do serviço, embora o serviço já a produza via `ApiExceptionHandler`. Declarar o schema é documentar, não alterar, comportamento já conforme. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. | ✅ N/A |
| III. Tiered Testing Discipline | Há geração de código (`ProblemDetail` → DTO, 6 → 7), mas geração ≠ comportamento: o artefato é portador de dados sem lógica, ainda não referenciado por nenhuma operação (isso é `T-002-7-2..6`). Sem unidade de comportamento a testar em `hb-catalog-service`. Gates: build do módulo, comparação de inventário de DTOs e `mvn -B verify` — os dois últimos com captura própria nesta execução. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nada de contadores ou chaves de idempotência. | ✅ N/A |
| VI. Security & Configuration Hygiene | Nenhuma mudança de autenticação/autorização. O schema não expõe segredo — `detail`/`title` são mensagens de erro humanas já produzidas pelo serviço; `type`/`instance` são URIs constantes (`urn:hubinity:catalog:...`) ou `about:blank`. | ✅ PASS |
| Technology Constraints | Nenhuma dependência ou infraestrutura nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/012-problemdetail-schema/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── problemdetail-schema.yaml   # O fragmento-alvo
├── checklists/
│   ├── requirements.md      # /speckit-specify quality gate (16/16)
│   └── contract.md          # /speckit-checklist (19 itens; 1 gap → corrigido na spec)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado à mão)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + schema ProblemDetail em components/schemas
                           #   (anexado após ProductImageResponse)
                           # Nenhum desfecho de erro referenciado ainda

# Artefato GERADO (não escrito à mão, não versionado):
# .../dto/ProblemDetail.java   → sétimo código produzido pela cadeia T-002

# hb-catalog-service/ — NENHUM arquivo alterado
```

**Structure Decision**: Padrão da cadeia. Primeira das seis subtarefas de `T-002-7`; as cinco seguintes (`T-002-7-2..6`) referenciam este schema, uma por desfecho de erro.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado: um único fragmento aditivo em 1 arquivo. A Phase 1 confirmou que a única entidade nova é de documento (um schema) e sua contrapartida é um DTO gerado sem lógica, ainda não referenciado. Nenhuma pendência constitucional aberta. **Gate final: PASS.**
