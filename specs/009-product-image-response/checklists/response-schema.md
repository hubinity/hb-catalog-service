# Response Schema Requirements Quality Checklist: Schema de resposta do registro de imagem

**Purpose**: Validate the *quality* of the contract requirements for T-002-4 — the `201` response body — before `/speckit-plan` builds on them
**Created**: 2026-07-25
**Feature**: [spec.md](../spec.md)
**Depth**: Standard · **Audience**: Reviewer (PR) · **Companion to**: [requirements.md](./requirements.md) (spec-level quality gate)

**Note**: Itens marcados conforme a avaliação executada nesta etapa do pipeline — ver *Evaluation Result*.

## Requirement Completeness

- [x] CHK001 Are all constituent parts of the deliverable enumerated (`content` on the `201`, the new schema)? [Completeness, Spec §Decisão de escopo]
- [x] CHK002 Is the required shape of `ProductImageResponse` fully specified (type, required list, both properties, item constraints)? [Completeness, Spec §FR-004…§FR-008]
- [x] CHK003 Is the media type constrained to `application/json` exclusively? [Completeness, Spec §FR-001]
- [x] CHK004 Is the placement of the new schema among the existing ones specified? [Completeness, Spec §Assumptions]
- [x] CHK005 Is it stated that the existing `201` `description` must survive untouched? [Completeness, Spec §FR-003]
- [x] CHK006 Are requirements stated for what must NOT be declared, not only for what is added? [Completeness, Spec §FR-009, §FR-010, §FR-011, §FR-012]
- [x] CHK007 Is the inclusion of `productId` — redundant with the path parameter — justified rather than left as unexplained noise? [Gap, Spec §Edge Cases]

## Requirement Clarity & Measurability

- [x] CHK008 Is the "full resulting collection, not just the new entry" semantic stated in the schema's own `description`, not only in spec prose? [Clarity, Spec §FR-004]
- [x] CHK009 Is FR-013's additive criterion ("no line appears as `-`") stated without inheriting T-002-2's different rule? [Measurability, Spec §FR-013]
- [x] CHK010 Is the origin of `maxLength: 2048` on the items traceable, and is it flagged as an addition beyond the approved sketch? [Clarity, Spec §Assumptions]
- [x] CHK011 Can FR-015's "exactly one additional DTO" be objectively verified, including the unchanged-checksum claim? [Measurability, Spec §FR-015]
- [x] CHK012 Is the prohibition on `minItems` argued rather than asserted? [Clarity, Spec §FR-009]

## Backlog Gap Handling (schema `Product` sem `images`)

- [x] CHK013 Is the gap supported by **evidence** (a stated scan of the tracker) rather than by impression? [Traceability, Spec §Lacuna de backlog]
- [x] CHK014 Is the **impact** of leaving it open stated concretely (contract diverges from service after T-003-4)? [Clarity, Spec §Lacuna de backlog]
- [x] CHK015 Is the handoff **actionable** — a complete tracker entry with id, description, phase and `depends_on`, plus a suggested queue position? [Completeness, Spec §Out of Scope]
- [x] CHK016 Is the decision **not** to fix it here justified by scope rather than reading as avoidance? [Clarity, Spec §Lacuna de backlog]
- [x] CHK017 Is it stated that this task avoids *depending* on the gap, so the gap is not aggravated? [Consistency, Spec §Edge Cases]
- [x] CHK018 Is it explicit that `TASKS.json` is **not** edited by the spec? [Traceability, Spec §Out of Scope]

## Second Gap: Error Bodies vs. Constitution

- [x] CHK019 Is the absence of modelled error bodies reconciled with Principle I's RFC 7807 mandate, rather than passed over? [Gap, Spec §Edge Cases]
- [x] CHK020 Is that second gap given the same treatment as the first — evidence, impact, proposed entry? [Consistency, Spec §Out of Scope]
- [x] CHK021 Is the difference in `decomposition_allowed` between the two proposed entries justified? [Clarity, Spec §Out of Scope]

## Response Shape Rationale

- [x] CHK022 Is returning the collection rather than the created item justified strongly enough that a reviewer will not "correct" it? [Clarity, Spec §Edge Cases]
- [x] CHK023 Is that justification tied to the same root cause as the already-accepted absence of `Location`? [Consistency, Spec §Edge Cases]
- [x] CHK024 Is the positional convention (first element = primary) made **observable in this contract**, rather than requiring the reader to recall T-002-3? [Clarity, Spec §FR-004, §FR-007]
- [x] CHK025 Is the single-element case addressed? [Edge Case, Spec §Edge Cases]
- [x] CHK026 Is unbounded collection growth in the response either bounded or explicitly deferred? [Gap, Spec §Edge Cases]

## Code-Generation Continuity

- [x] CHK027 Is it stated that this is the **second** generating task, quantified (5 → 6)? [Measurability, Spec §Contexto técnico]
- [x] CHK028 Are T-002-3's two gates (prior inventory capture, measured test baseline) required **again** rather than assumed already satisfied? [Completeness, Spec §FR-015, §FR-016]
- [x] CHK029 Is the distinction between *generated artifact* and *new behaviour* preserved from the previous task? [Consistency, Spec §Edge Cases]

## Requirement Consistency

- [x] CHK030 Is the `$ref`-not-inline choice consistent with every other response body in the document? [Consistency, Spec §FR-002]
- [x] CHK031 Is the schema name consistent with the `Response` suffix used by the service? [Consistency, Spec §Assumptions]
- [x] CHK032 Are the item constraints symmetric with `ProductImageRequest.url` from T-002-3? [Consistency, Spec §FR-008]
- [x] CHK033 Is the tolerant-reader posture (no `additionalProperties: false`) consistent with T-002-3? [Consistency, Spec §FR-010]
- [x] CHK034 Is the decision to leave the Path Item `description` untouched consistent with the rule established in T-002-3? [Consistency, Spec §FR-012]

## Scope Boundary Integrity

- [x] CHK035 Is the boundary against T-002-5 (storage strategy **and** the final `description` cleanup) stated? [Coverage, Spec §Out of Scope]
- [x] CHK036 Is the boundary against the T-003 chain stated? [Coverage, Spec §Out of Scope]
- [x] CHK037 Is the boundary against the T-005 chain stated? [Coverage, Spec §Out of Scope]
- [x] CHK038 Are the error outcomes explicitly kept out of scope rather than silently skipped? [Coverage, Spec §FR-011]

## Dependencies & Assumptions

- [x] CHK039 Is the dependency on T-002-3's delivered request body and schema documented? [Dependency, Spec §Contexto técnico]
- [x] CHK040 Are decisions made *by this spec* distinguished from those *inherited* from the user? [Clarity, Spec §Assumptions]

## Evaluation Result (2026-07-25)

Avaliado contra `spec.md` no momento da geração. **37 de 40 passaram na primeira leitura; 3 falharam e motivaram edições na spec.** Os 40 passam após as correções abaixo.

| Item | Achado | Resolução |
|---|---|---|
| CHK019 / CHK020 / CHK021 | **Mais substantivo — segunda lacuna de backlog.** O Princípio I da constituição exige RFC 7807 `ProblemDetail` em **toda** resposta de erro do serviço, mas o contrato declara `400`/`403`/`404` só com `description`, e **nenhuma task modela `ProblemDetail`**. A spec tratava isso como simples "fora de escopo, ninguém revisita" — sem notar que é divergência contrato × serviço da mesma natureza da lacuna do `Product`. | Adicionado Edge Case nomeando a divergência e o princípio, mais entrada proposta `T-002-7` no *Out of Scope*, com o mesmo rigor dado à `T-002-6`. A diferença de `decomposition_allowed` entre as duas foi justificada (esta alcança todas as operações). |
| CHK007 | `productId` na resposta é redundante com o parâmetro de path, e a spec não dizia por que declará-lo — um revisor poderia removê-lo como ruído. | Edge Case novo: torna a resposta autocontida (log, cache, fila) e há **precedente direto** — `StockItem` declara `productId` embora servido por `/products/{productId}/stock`. |
| CHK026 | Crescimento da coleção na resposta não era mencionado: sem teto, sem paginação, sem deferimento. | Edge Case novo: cardinalidade esperada é baixa, e um teto pertence ao **atributo**, não a esta resposta — mesma alocação de T-002-3, que remeteu o limite à cadeia T-003. |

Nenhum item foi dispensado. Nenhuma ambiguidade pendente bloqueia o `/speckit-plan`.

**Padrão desta execução**: as três falhas foram de **consequência não examinada** — a spec decidia corretamente, mas não perseguia o efeito da decisão até o fim (o que acontece com erros? por que este campo existe? e se a coleção crescer?). Difere das anteriores: 006 falhou em fronteiras de escopo, 007 em subespecificação do entregável, 008 em permissividade não declarada.

## Notes

- Itens marcados `[x]` refletem a avaliação já executada; o resultado final volta a ser revisitado quando o quickstart rodar de ponta a ponta.
- Esta lista valida **qualidade de requisito**. A correção do contrato é provada pelos gates do `tasks.md`: build do módulo (FR-014), inventário de DTOs (FR-015) e regressão do consumidor (FR-016).
