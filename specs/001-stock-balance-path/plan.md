# Implementation Plan: Path canônico do endpoint de leitura de saldo de estoque no contrato compartilhado

**Branch**: `001-stock-balance-path` | **Date**: 2026-07-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-stock-balance-path/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Registrar o path canônico `/api/v1/products/{productId}/stock` na seção `paths` de `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-001-1). A abordagem: edição aditiva mínima do documento OpenAPI 3.1, validada pelo próprio pipeline de geração do módulo (`openapi-generator-maven-plugin`, parse via swagger-parser durante `mvn -B install`). O cabeçalho do arquivo, que estava corrompido no working tree, já foi restaurado ao estado commitado (`openapi: 3.1.0`), eliminando a pré-condição FR-004 como trabalho remanescente.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` (generator `spring`, `generateApis=false`, `generateModels=true`, `interfaceOnly=true` — DTO-only por ADR 0002); `maven-antrun-plugin` (staging do spec em `${java.io.tmpdir}` para contornar bug de URI com caminho não-ASCII — "Área de Trabalho")

**Storage**: N/A (nenhuma mudança de schema de banco; Flyway não envolvido)

**Testing**: `( cd platform-shared-contracts && mvn -B -DskipTests install )` — o parse do spec pelo generator é o gate de validade; verificação adicional por inspeção (`grep` do path) e parser YAML

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, consumido por serviços JVM (hb-catalog-service) — build local apenas, sem publicação remota (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (multi-repo, edição em `platform-shared-contracts`, repo irmão)

**Performance Goals**: N/A (mudança documental; nenhum requisito de runtime)

**Constraints**: Mudança estritamente aditiva (FR-003); path único de saldo (FR-005); parâmetro `productId` (FR-002); documento deve permanecer OpenAPI 3.1 válido (FR-004); entrada de path pode ficar sem operação — Path Item Object vazio é válido em OpenAPI 3.1 (contingência FR-006 avaliada na Phase 0)

**Scale/Scope**: 1 arquivo, 1 entrada de path (+ descrição); nenhuma linha de código Java gerada ou alterada

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço é alterado. A regra "cross-service payloads MUST come from platform-shared-contracts" é exatamente o que esta feature reforça: a mudança é feita no repo de contratos, nunca duplicada à mão no serviço. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de schema de banco. | ✅ N/A |
| III. Tiered Testing Discipline | Nenhum comportamento Java muda; nenhum teste de serviço é exigido. O gate de qualidade equivalente para contrato é o build do módulo (`mvn -B install`), que roda offline e sem Docker — coerente com o espírito do princípio. | ✅ PASS |
| IV. Events via Transactional Outbox | Nenhum evento criado ou alterado. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Endpoint é de leitura (GET futuro, T-001-2); não entra no conjunto de mutações que exigem Idempotency-Key nem toca contadores de estoque. | ✅ N/A |
| VI. Security & Configuration Hygiene | Autorização da operação é escopo de T-001-5 (registrado em Out of Scope). Nenhum secret, nenhuma configuração alterada. | ✅ PASS (deferido explicitamente) |
| Technology Constraints | "Shared contract changes MUST be made in platform-shared-contracts and consumed as versioned Maven artifacts" — o plano faz exatamente isso; nenhuma dependência nova, nenhum ADR necessário. | ✅ PASS |

**Gate inicial**: PASS — sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/001-stock-balance-path/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── stock-balance-path.yaml   # Fragmento-alvo da entrada de path
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate
│   └── contract.md      # /speckit-checklist (revisão de PR)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml          # + entrada `/api/v1/products/{productId}/stock` na seção paths

# hb-catalog-service/ — NENHUM arquivo de código alterado nesta feature
# (implementação do endpoint é T-004-x; consumo do artefato permanece
#  com.hubinity:contracts-catalog:0.1.0-SNAPSHOT já declarado no pom.xml)
```

**Structure Decision**: Edição em repositório irmão `platform-shared-contracts` (multi-repo por design do ecossistema). O artefato de feature (specs/) vive em `hb-catalog-service` porque a task pertence ao backlog deste serviço (TASKS.json), mas o entregável físico é exclusivamente `contracts-catalog/openapi/catalog.yaml`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado (research.md, data-model.md, contracts/, quickstart.md) não introduziu nenhum elemento novo: continua sendo 1 edição aditiva em 1 arquivo YAML validada por build Maven offline. **Gate final: PASS.**
