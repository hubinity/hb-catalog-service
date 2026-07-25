# Specification Quality Checklist: Corpo de requisição JSON do registro de imagem de produto

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-07-25
**Feature**: [spec.md](../spec.md)

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

### Validation outcome

15 requisitos funcionais, 8 critérios de sucesso, 16/16 itens aprovados. Três pontos exigiram
julgamento em vez de marcação reflexa:

- **"No implementation details"** — a spec nomeia construções OpenAPI (`requestBody`, `$ref`,
  `format: uri`) e cita `generateModels=true`. Não é violação: o entregável **é** um documento de
  contrato, e a citação do gerador serve como **evidência de consequência** (esta task passa a
  gerar código), não como trabalho a fazer. Mesma convenção das specs 001–007.
- **"Requirements are testable and unambiguous"** — FR-014 exige que a geração produza
  "exatamente um DTO adicional". É verificável por contagem antes/depois, não por inspeção
  subjetiva; foi redigido assim justamente porque é a primeira vez na cadeia que há código gerado.
- **"Written for non-technical stakeholders"** — a explicação de mixed content (FR-007) é técnica.
  Mantida: sem ela, um revisor tenderia a "corrigir" a ausência de `pattern` como descuido, quando
  é decisão registrada.

### Zero [NEEDS CLARIFICATION] markers — why

As duas incógnitas sem default defensável (forma do corpo; rigor da validação) foram decididas
pelo usuário antes da especificação. Quatro outras foram resolvidas por evidência e registradas
como decisões, não como silêncio:

- **Singular, não lote** — o texto commitado em `854c02f` já diz "the URL of an image" e "an image
  reference"; lote exigiria reescrever contrato entregue.
- **Schema nomeado, não inline** — todo corpo do documento usa `$ref`; `api/dto/` já usa sufixo
  `Request`.
- **Sem `409`** — realocado à cadeia T-003 (ver abaixo).
- **Sem reescrita da `description` do Path Item** — a frase segue verdadeira como proveniência.

### Obrigação herdada — encerrada aqui

A spec 007 (T-002-2) deferiu explicitamente o desfecho **`400`** a esta task, por ele depender da
existência do formato de requisição. FR-008 o declara. A obrigação **não é repassada adiante**.

### Pergunta realocada, não deferida de novo

O `409`/URL duplicada tinha sido deferido a esta task pela 007, sob a justificativa de depender da
forma do corpo. Com a forma resolvida (uma URL), ficou claro que a pergunta é sobre o **estado da
coleção**, não sobre o **formato do payload** — então foi **realocada** à cadeia T-003, onde a
coluna e uma eventual restrição de unicidade existem. Isso é diferente de deferir a mesma pergunta
duas vezes: mudou o motivo e mudou o destinatário.

### Ruptura em relação a T-002-1 e T-002-2

Esta é a **primeira task da cadeia T-002 que gera código**: um schema em `components/schemas` vira
DTO (`generateModels=true`), levando os modelos gerados de 4 para 5. O argumento "nada é gerado",
que sustentou o Constitution Check do Princípio III nas duas tasks anteriores, **deixa de valer** e
precisa ser reformulado no plano — sinalizado aqui para que o `/speckit-plan` não o repita por
inércia.

Em compensação, a mudança volta a ser **estritamente aditiva** (zero remoções), ao contrário da
007 — o critério de diff da 006 volta a valer.
