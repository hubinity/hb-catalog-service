# Specification Quality Checklist: Propriedade images no schema Product

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-07-26
**Feature**: [spec.md](../spec.md)
**Task de origem**: `T-002-6` — nascida da lacuna identificada pela spec 009

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

**Validação executada em 2026-07-26 — todos os itens passam.** As duas ressalvas estruturais da fase `contracts` são as mesmas registradas nas specs 005, 008, 009 e 010, pelo mesmo critério: o entregável **é** um documento de especificação, então trechos YAML são o objeto da spec e não vazamento de implementação; e SC-005/SC-006/SC-007 referenciam build e artefatos gerados porque, numa task de contrato, esses são os desfechos observáveis — SC-001 a SC-004 e SC-008 cobrem o valor de negócio de forma agnóstica.

**Pontos de força verificados:**

- **Zero marcadores [NEEDS CLARIFICATION]** — diferente de `T-002-5`, esta task não tinha decisão em aberto: a forma da propriedade é ditada por `ProductImageResponse.images` (molde existente) e a escolha opcional/obrigatório é resolvida pela lista `required` que o próprio schema já pratica. Nenhuma pergunta bloqueante era necessária, e nenhuma foi inventada.
- **Duas afirmações de risco foram verificadas, não presumidas.** (a) `Product` é referenciado **uma única vez** (linha 52, `200` de `getProductById`) e **nunca** é corpo de requisição — é isso que dispensa `readOnly: true` (FR-010) e elimina a ambiguidade "posso escrever `images`?". (b) **Nenhum** arquivo de `hb-catalog-service/src/` importa `com.hubinity.contracts`. Ambas foram checadas por varredura antes de virarem requisito.
- **FR-016 e FR-017 são deliberadamente distintos.** Checksum diferente prova que *algo* mudou; FR-017 exige confirmar **o quê** (o campo `images` em `Product.java`). Sem esse par, um erro que alterasse o DTO por outro motivo passaria pelo gate.
- **FR-018 exige declarar a própria fraqueza do gate.** A regressão do consumidor passa porque nada consome os DTOs gerados — não porque a mudança é compatível com o uso. A spec obriga a registrar isso, para que a passagem não seja lida como garantia mais forte do que é. É o oposto de inflar o resultado.
- **FR-014 já nasce com o predicado corrigido** (`grep '^-' | grep -v '^---'`), incorporando o defeito C1 que o `/speckit-analyze` da spec 010 encontrou — a forma `grep '^-[^-]'` não detecta remoção de linha em branco.
- **Duplicação da convenção posicional é assumida como deliberada**, com o risco de divergência futura declarado nos *Edge Cases*, em vez de introduzida silenciosamente.
- **Uma lacuna nova é registrada com evidência**: seis tasks (`T-006-1` a `T-006-6`) especificam ETag sobre `GET /api/v1/products` e `GET /api/v1/categories`, endpoints que o contrato **não declara** — o documento tem apenas três paths. Entrada `T-002-9` proposta; `TASKS.json` intocado.
