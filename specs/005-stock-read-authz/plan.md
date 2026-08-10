# Implementation Plan: Requisito de autorização da operação de leitura de saldo de estoque

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia T-001-x) | **Date**: 2026-07-24 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-stock-read-authz/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Introduzir segurança no contrato `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-001-5, **última da cadeia T-001**): declarar `components/securitySchemes/bearerAuth` (HTTP bearer JWT, Keycloak realm `hibit`), aplicar `security: [bearerAuth: []]` no nível raiz (todas as operações), e finalizar a `description` do Path Item. Decisões estruturais do usuário (2026-07-24): HTTP bearer JWT (não OAuth2/OIDC) e `security` global (não por operação) — fiéis ao `SecurityConfig` (`/api/**` authenticated; reads sem admin). Mudança aditiva validada pelo build do módulo + regressão do consumidor.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` (DTO-only, ADR 0002 — `securitySchemes`/`security` não geram modelo; passam pelo parse); staging antrun (herdado)

**Storage**: N/A

**Testing**: build do módulo (`mvn -B -DskipTests install`) + regressão do consumidor (`mvn -B verify`) — herdados da cadeia

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: Mudança aditiva — `securitySchemes` (em `components`) + `security` (raiz) + description do Path Item (FR-005); operação de saldo sem `security` próprio (FR-003 — herda o global); esquema/requisito fixados no spec (FR-001/FR-002); documento válido pela autoridade herdada

**Scale/Scope**: 1 arquivo; ~6 linhas de `securitySchemes`, 2 linhas de `security` raiz, description do Path Item

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; contrato no repo correto. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. | ✅ N/A |
| III. Tiered Testing Discipline | Nenhum comportamento Java muda; gates: build do módulo + verify do consumidor. | ✅ PASS |
| IV. Events via Transactional Outbox | Nenhum evento. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Leitura; nada tocado. | ✅ N/A |
| VI. Security & Configuration Hygiene | **Alinhamento positivo**: o contrato passa a declarar o que o Princípio VI já exige do serviço (OAuth2 Resource Server, JWT do realm hibit). O requisito documentado espelha `SecurityConfig` — não introduz política nova, apenas a torna explícita no contrato. Secrets não entram (bearerAuth é descritivo; sem URLs de ambiente). | ✅ PASS |
| Technology Constraints | Mudança de contrato no repo correto; nenhuma dependência nova. | ✅ PASS |

**Gate inicial**: PASS — sem violações; Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/005-stock-read-authz/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── stock-read-authz.yaml    # Fragmentos-alvo: securitySchemes + security raiz + description
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── contract.md      # /speckit-checklist revisão de PR (21/21)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + components/securitySchemes/bearerAuth
                           # + security (raiz): - bearerAuth: []
                           # ~ description do Path Item (estado final — cadeia T-001 completa)

# hb-catalog-service/ — NENHUM arquivo de código alterado
# (SecurityConfig.java é fonte de verdade, intocado; mvn -B verify prova regressão zero)
```

**Structure Decision**: Padrão da cadeia T-001-x — edição no repo irmão, artefatos aqui, branch compartilhada. Esta feature fecha a cadeia: após ela a operação de leitura de saldo está integralmente especificada.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia.

## Constitution Re-Check (pós-Phase 1)

Design finalizado sem elementos novos: edição aditiva em 1 arquivo YAML, validada por build offline + regressão do consumidor. O requisito de segurança documentado reforça o Princípio VI em vez de tensioná-lo. **Gate final: PASS.**
