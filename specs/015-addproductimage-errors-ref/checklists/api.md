# API/Contract Checklist: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

**Purpose**: Validar a qualidade dos requisitos de `spec.md` para `T-002-7-4/5/6` antes de gerar `plan.md`.
**Created**: 2026-07-29
**Feature**: [spec.md](../spec.md)

**Foco selecionado**: contrato OpenAPI dos três desfechos de erro de uma única operação (`addProductImage`) e a interação com as cadeias de implementação pendentes (`T-002`/`T-003`) — a diferença distintiva desta feature frente às suas irmãs (`013`/`014`) é cobrir três desfechos simultaneamente na mesma operação, em vez de um único desfecho isolado. **Profundidade**: leve, proporcional ao tamanho da mudança (três blocos `content` pontuais). **Audiência**: revisor antes de `/speckit-plan`.

## Requirement Completeness

- [x] CHK001 O requisito cobre a adição de `content` para os três desfechos (400, 403, 404) de forma simétrica, sem tratar nenhum como caso especial? [Completeness, Spec §FR-001–FR-003 — confirmado presente]
- [x] CHK002 A preservação das três `description` existentes está coberta individualmente, uma por desfecho, e não apenas de forma agregada? [Completeness, Spec §FR-004 — confirmado presente]
- [x] CHK003 A ausência de implementação da operação (`ProductController`/`ProductService`, cadeias `T-002`/`T-003`) está documentada como contexto, não como bloqueio silencioso ou lacuna não reconhecida? [Completeness, Spec §Contexto técnico verificado, §Edge Cases — confirmado presente, tratado explicitamente]

## Requirement Clarity

- [x] CHK004 O elemento-alvo exato dos três desfechos está mostrado em um único bloco YAML, sem exigir inferência de indentação/chave para nenhum dos três? [Clarity, Spec §Decisão de escopo — confirmado presente]
- [x] CHK005 A justificativa de "regressão zero" está corretamente descrita como estrutural (ausência de código, não preservação de comportamento existente), no mesmo sentido já estabelecido por `014`? [Clarity, Spec §FR-008 — confirmado presente]

## Requirement Consistency

- [x] CHK006 A decisão de não bloquear esta feature nas cadeias `T-002`/`T-003` é consistente com o `depends_on` real das três tasks em `TASKS.json` (`T-002-7-4→T-002-7-3`, `T-002-7-5→T-002-7-4`, `T-002-7-6→T-002-7-5` — todas internas à cadeia `T-002-7`, nenhuma para `T-002`/`T-003`)? [Consistency, Spec §Tasks de origem — confirmado presente]
- [x] CHK007 O padrão "contrato à frente da implementação" é justificado por precedente real do projeto (mesmo padrão de `013`/`014`), não apresentado como exceção ad-hoc desta feature? [Consistency, Spec §Contexto técnico verificado — confirmado presente]
- [x] CHK008 A decisão de agrupar três tasks (`T-002-7-4/5/6`) em uma única feature, em vez de uma feature por task (padrão de `013`/`014`), está justificada explicitamente? [Consistency, Spec §Assumptions — confirmado presente]

## Acceptance Criteria Quality

- [x] CHK009 Os critérios de sucesso (`SC-001`–`SC-003`) são objetivamente verificáveis para os três desfechos, não apenas para um subconjunto? [Measurability, Spec §Success Criteria — confirmado presente]

## Scenario Coverage

- [x] CHK010 Existe cenário cobrindo que o desfecho `201` da mesma operação permanece intocado? [Coverage, Spec §User Story 3, cenário 3 — confirmado presente]
- [x] CHK011 Cada um dos três desfechos tem sua própria user story com critérios de aceite dedicados, em vez de uma única story genérica cobrindo os três? [Coverage, Spec §User Story 1–3 — confirmado presente]

## Edge Case Coverage

- [x] CHK012 O caso "quem implementar as cadeias `T-002`/`T-003` no futuro precisa produzir um `ProblemDetail` compatível para cada um dos três desfechos" está atribuído explicitamente a essas cadeias futuras, evitando que esta spec pareça responsável por uma garantia de runtime que não pode cumprir hoje? [Edge Case, Spec §Edge Cases — confirmado presente]
- [x] CHK013 A dependência de ordem entre as três tasks (encadeadas via `depends_on`) está reconciliada com o fato de as três mudanças serem entregues juntas nesta feature, sem implicar um estado intermediário observável? [Edge Case, Spec §Edge Cases — confirmado presente]

## Dependencies & Assumptions

- [x] CHK014 A dependência real (`T-002-7-3`, done) está identificada corretamente, sem confundir com as cadeias `T-002`/`T-003` (pendentes, mas não dependências formais)? [Dependency, Spec §Tasks de origem — confirmado presente]
- [x] CHK015 A suposição de reinstalação de `contracts-catalog` está registrada? [Assumption, Spec §Assumptions — confirmado presente]

## Ambiguities & Conflicts

- [x] CHK016 Nenhum conflito identificado entre as três user stories quanto ao elemento-alvo ou ao schema referenciado (as três referenciam o mesmo `ProblemDetail`, sem variação). [Conflict — nenhum encontrado]

## Notes

- **Nenhum achado novo** — a spec já aplica as lições das features irmãs (`013`, que encontrou um `$ref` quebrado, e `014`, que tratou explicitamente a ausência de implementação): caminho do contrato correto, ausência de implementação tratada como contexto, e agrupamento de três desfechos justificado por serem da mesma operação.
- Checklist proporcionalmente maior que a de `014` (16 itens vs. 13) para refletir a cobertura triplicada (três desfechos em vez de um), mantendo a mesma profundidade leve por item.
- `plan.md` ainda não existe nesta etapa do pipeline (checklist roda antes de `/speckit-plan`); nenhum item desta checklist depende de conteúdo de `plan.md`.
