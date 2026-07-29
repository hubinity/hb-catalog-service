# API/Contract Checklist: Referenciar ProblemDetail no 404 de getStockItemByProductId

**Purpose**: Validar a qualidade dos requisitos de `spec.md` e `plan.md` para `T-002-7-3` antes de gerar `tasks.md`.
**Created**: 2026-07-29
**Feature**: [spec.md](../spec.md), [plan.md](../plan.md)

**Foco selecionado**: contrato OpenAPI + a interação com a cadeia de implementação pendente (`T-004`), já que essa é a diferença distintiva desta task em relação à sua irmã (`T-002-7-2`). **Profundidade**: leve, proporcional ao tamanho da mudança. **Audiência**: revisor antes de `/speckit-tasks`.

## Requirement Completeness

- [x] CHK001 O requisito cobre tanto a adição do `content` quanto a preservação da `description` existente? [Completeness, Spec §FR-001, §FR-002 — confirmado presente]
- [x] CHK002 A ausência de implementação da rota (`StockController`/`StockService`, cadeia `T-004`) está documentada como contexto, não como bloqueio silencioso ou lacuna não reconhecida? [Completeness, Spec §Contexto técnico verificado, §Edge Cases — confirmado presente, tratado explicitamente]

## Requirement Clarity

- [x] CHK003 O elemento-alvo exato está mostrado sem exigir inferência de indentação/chave? [Clarity, Spec §Decisão de escopo — confirmado presente]
- [x] CHK004 A justificativa de "regressão zero" está diferenciada da task irmã (aqui é estrutural, por ausência de código, não por preservação de comportamento existente), evitando que um leitor confunda os dois casos? [Clarity, Spec §FR-006, Plan §Technical Context — confirmado presente]

## Requirement Consistency

- [x] CHK005 A decisão de não bloquear esta task na cadeia `T-004` é consistente com o `depends_on` real de `T-002-7-3` em `TASKS.json` (`[T-002-7-2]`, não `T-004-*`)? [Consistency, Spec §Task de origem, Research item 2 — confirmado presente]
- [x] CHK006 O padrão "contrato à frente da implementação" é justificado por precedente real do projeto (cadeia de imagens de produto), não apresentado como uma exceção ad-hoc? [Consistency, Spec §Contexto técnico verificado — confirmado presente]

## Acceptance Criteria Quality

- [x] CHK007 Os critérios de sucesso (`SC-001`–`SC-003`) são objetivamente verificáveis? [Measurability, Spec §Success Criteria — confirmado presente]

## Scenario Coverage

- [x] CHK008 Existe cenário cobrindo que o desfecho `200` da mesma operação permanece intocado? [Coverage, Spec §User Story 1, cenário 3 — confirmado presente]

## Edge Case Coverage

- [x] CHK009 A dualidade da `description` original ("unknown product **or** no stock record" — duas causas distintas para o mesmo 404) está reconciliada com o fato de `ProblemDetail` ser genérico o suficiente para cobrir ambas sem exigir dois schemas? [Edge Case, Spec §Edge Cases — confirmado presente, implicitamente coberto pela genericidade do schema (sem enum em `type`/`title`)]
- [x] CHK010 O caso "quem implementar `T-004` no futuro precisa produzir um `ProblemDetail` compatível" está atribuído explicitamente a essa task futura, evitando que esta spec pareça responsável por uma garantia de runtime que não pode cumprir hoje? [Edge Case, Spec §Edge Cases — confirmado presente]

## Dependencies & Assumptions

- [x] CHK011 A dependência real (`T-002-7-2`, done) está identificada corretamente, sem confundir com a cadeia `T-004` (pendente, mas não uma dependência formal)? [Dependency, Spec §Task de origem — confirmado presente]
- [x] CHK012 A suposição de reinstalação de `contracts-catalog` está registrada? [Assumption, Spec §Assumptions — confirmado presente]

## Ambiguities & Conflicts

- [x] CHK013 Nenhum conflito identificado entre `spec.md` e `plan.md` quanto ao elemento-alvo ou à ausência de impacto em `T-004`. [Conflict — nenhum encontrado]

## Notes

- **Nenhum achado novo desta vez** — diferente da checklist da task irmã (`013/checklists/api.md`, que encontrou CHK010 sobre `$ref` quebrado) e da análise que encontrou H1 (caminho `../platform-shared-contracts`), esta spec já nasceu aplicando as duas lições: caminhos já usam `../platform-shared-contracts` desde `plan.md`/`quickstart.md`, e a ausência de implementação da rota já é tratada como contexto explícito, não como lacuna.
- Checklist proporcionalmente pequena, mesmo padrão da task irmã, refletindo o tamanho real da mudança.
