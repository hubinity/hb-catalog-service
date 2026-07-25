# Implementation Plan: Path do endpoint de imagens de produto

**Branch**: `feature/stock-balance-path` (ativa em ambos os repos — herdada da cadeia T-001-x) | **Date**: 2026-07-25 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-product-images-path/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

Abrir a cadeia T-002 no contrato `platform-shared-contracts/contracts-catalog/openapi/catalog.yaml` (task T-002-1) declarando o Path Item `/api/v1/products/{productId}/images` com: `summary`, `description` (que fixa a semântica **URL-only** — a API registra apenas URLs, nunca recebe bytes) e `parameters` de nível de Path Item contendo `productId` (UUID). **Nenhuma operação, corpo ou schema é declarado** — são entregáveis de T-002-2/-3/-4. Premissa de entrada: decisão do usuário de 2026-07-25 por *URL-only reference*, motivada por a entrega via CDN ser explicitamente pós-MVP (PRD §12). Mudança estritamente aditiva em 1 arquivo de repo irmão, validada por build do módulo + regressão zero no consumidor.

## Technical Context

**Language/Version**: YAML / OpenAPI 3.1.0 (artefato); toolchain de validação Java 21 + Maven 3.9+

**Primary Dependencies**: `openapi-generator-maven-plugin` configurado com `generateApis=false` / `generateModels=true` (ADR 0002) — **verificado no pom pai, linhas 114-115**. Consequência decisiva: um Path Item **sem operações e sem schemas novos gera exatamente zero código Java**. Antrun de staging da spec (workaround de path não-ASCII, herdado).

**Storage**: N/A — a decisão URL-only significa que este contrato não introduz nenhum armazenamento; as imagens residem fora do sistema e só suas URLs serão referenciadas (por tasks futuras).

**Testing**: build do módulo (`mvn -B -DskipTests install` em `platform-shared-contracts`) + regressão do consumidor (`mvn -B verify` em `hb-catalog-service`) — autoridade herdada da cadeia T-001

**Target Platform**: Artefato Maven `com.hubinity:contracts-catalog:0.1.0-SNAPSHOT`, build local (ADR 0006)

**Project Type**: Módulo de contrato compartilhado (edição no repo irmão)

**Performance Goals**: N/A

**Constraints**: Estritamente aditiva — nenhuma linha preexistente alterada, removida ou reordenada, nem por reformatação (FR-007, critério de diff objetivo). Sem operação (FR-006), sem `tags` (FR-010, campo de operação), sem `security` próprio (FR-005, herda `bearerAuth` da raiz). Nome do parâmetro deve casar com o template `{productId}` (FR-003).

**Scale/Scope**: 1 arquivo, 1 bloco novo de ~14 linhas em `paths`. Zero arquivos Java em ambos os repos.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Princípio | Avaliação | Resultado |
|---|---|---|
| I. Layered Architecture Boundaries | Nenhum código do serviço alterado; o contrato é editado no repo que o possui (`platform-shared-contracts`), nunca duplicado localmente. | ✅ PASS |
| II. Schema Evolution via Flyway Only | Nenhuma mudança de banco. O atributo `images` na entidade/tabela é da cadeia T-003 e exigirá migração própria lá. | ✅ N/A |
| III. Tiered Testing Discipline | **Tratado explicitamente**: o princípio exige testes por *comportamento alterado*. Aqui **nenhum comportamento de runtime muda**, e isso é verificável, não assumido — com `generateApis=false` e nenhum schema novo, o gerador não emite uma única classe a partir deste Path Item. Não há unidade Java para testar; inventar um teste seria teatro. Os gates aplicáveis são o build do módulo (o documento continua parseável) e `mvn -B verify` no consumidor (regressão zero). Quando T-002-2 declarar a operação e T-005-x a implementar, a disciplina de testes incide integralmente lá. | ✅ PASS (justificado) |
| IV. Events via Transactional Outbox | Nenhum evento publicado ou modelado. | ✅ N/A |
| V. Concurrency & Idempotency Invariants | Nenhum contador de estoque, nenhuma mutação. **Nota para a cadeia**: as operações futuras deste path são POST mutantes; a exigência literal de `Idempotency-Key` do Princípio V cobre os *endpoints de estoque* (movements/reserve/release/commit), não produto — portanto não se aplica automaticamente aqui. Decisão consciente a registrar em T-002-2. | ✅ N/A |
| VI. Security & Configuration Hygiene | O path herda `security: [bearerAuth: []]` da raiz — autenticação declarada, sem redeclaração divergente (FR-005). **Tensão reconhecida e deferida**: o Princípio VI exige `hasRole('admin')` em endpoints de mutação, e as operações futuras deste path serão mutações. O contrato hoje modela apenas autenticação (precedente de T-001-5); a obrigação de role permanece viva para T-002-2 e T-005-x, registrada nos Edge Cases e no Out of Scope da spec. Nenhum secret ou URL de ambiente entra no documento. | ✅ PASS (deferimento documentado) |
| Technology Constraints | Nenhuma dependência, framework ou infraestrutura nova — em particular, a decisão URL-only **evita** introduzir R2/S3 e suas credenciais, que exigiriam ADR. | ✅ PASS |

**Gate inicial**: PASS — sem violações. Complexity Tracking vazio.

## Project Structure

### Documentation (this feature)

```text
specs/006-product-images-path/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── product-images-path.yaml   # Fragmento-alvo: o Path Item exato a inserir
├── checklists/
│   ├── requirements.md  # /speckit-specify quality gate (16/16)
│   └── api.md           # /speckit-checklist qualidade de requisito (38 itens; 4 falhas → correções na spec)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
# Repo irmão: platform-shared-contracts/ (único arquivo tocado)
platform-shared-contracts/
└── contracts-catalog/
    └── openapi/
        └── catalog.yaml   # + Path Item /api/v1/products/{productId}/images
                           #   (summary + description URL-only + parameters.productId)
                           #   inserido após /api/v1/products/{productId}/stock

# hb-catalog-service/ — NENHUM arquivo de código alterado
# (mvn -B verify prova regressão zero; nenhuma classe é gerada a partir deste path)
```

**Structure Decision**: Padrão da cadeia T-001-x — edição no repo irmão, artefatos de spec aqui, branch compartilhada entre os dois repos. Esta feature **abre** a cadeia T-002 (a T-001 fechou em 005).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

Sem violações — tabela intencionalmente vazia. Os dois pontos que exigiram argumentação (Princípio III sem testes novos; Princípio VI com role deferida) foram resolvidos **dentro** do gate, com justificativa verificável, e não constituem violação.

## Constitution Re-Check (pós-Phase 1)

Design finalizado sem elementos novos: um bloco YAML aditivo em 1 arquivo, sem operação, sem schema, sem geração de código. A Phase 1 não introduziu nenhuma entidade de runtime — `data-model.md` registra apenas elementos de documento. As duas tensões identificadas no gate inicial permanecem deferidas com destinatário nomeado (T-002-2, T-005-x) em vez de silenciadas. **Gate final: PASS.**
