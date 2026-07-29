# API/Contract Checklist: Referenciar ProblemDetail no 404 de getProductById

**Purpose**: Validar a qualidade (completude, clareza, consistência, mensurabilidade) dos requisitos de `spec.md` e `plan.md` para `T-002-7-2` antes de gerar `tasks.md`.
**Created**: 2026-07-29
**Feature**: [spec.md](../spec.md), [plan.md](../plan.md)

**Foco selecionado**: contrato OpenAPI (schema/desfecho de erro) — o único artefato que esta task altera. **Profundidade**: leve (light-touch), proporcional ao tamanho da mudança (uma linha de bloco `content` em um arquivo YAML já existente). **Audiência**: revisor (autor + par) antes de `/speckit-tasks`.

*Sem perguntas de escopo feitas ao usuário: a descrição da task já é inequívoca (uma operação, um desfecho, um schema já existente a referenciar) e `decomposition_allowed: false` confirma que não há subdivisão adicional esperada.*

## Requirement Completeness

- [x] CHK001 O requisito cobre tanto a adição do `content` quanto a preservação da `description` existente, evitando que uma reescrita acidental do bloco `404` passe despercebida? [Completeness, Spec §FR-001, §FR-002 — confirmado presente]
- [x] CHK002 Existe um requisito que impeça a mudança de vazar para outros desfechos/operações do documento (escopo estritamente pontual)? [Completeness, Spec §FR-003 — confirmado presente, com critério objetivo de diff]

## Requirement Clarity

- [x] CHK003 O elemento-alvo exato (bloco YAML) está mostrado de forma que um implementador não precise inferir a indentação ou a chave a partir de prosa? [Clarity, Spec §Decisão de escopo — confirmado presente, snippet YAML literal incluído]
- [x] CHK004 A contagem de schemas/DTOs esperada após a mudança (7↔7) está explícita, evitando ambiguidade sobre se uma referência nova conta como schema novo? [Clarity, Spec §FR-005 — confirmado presente]

## Requirement Consistency

- [x] CHK005 A forma de referência (`content.application/json.schema.$ref`) é consistente com a sintaxe já usada pelo desfecho `200` da mesma operação, evitando duas convenções distintas para declarar corpo de resposta no mesmo documento? [Consistency, Plan §Technical Context / Research item 1 — confirmado presente]
- [x] CHK006 O plano justifica a ausência de mudança de código Java (`ApiExceptionHandler`) com evidência (comportamento já emitido em runtime), em vez de simplesmente omitir a questão? [Consistency, Plan §Constitution Check / Research item 2 — confirmado presente]

## Acceptance Criteria Quality

- [x] CHK007 `SC-001`–`SC-003` são objetivamente verificáveis (presença de `content`, resultado de build, contagem de tasks restantes na cadeia) sem exigir interpretação humana? [Measurability, Spec §Success Criteria — confirmado presente]

## Scenario Coverage

- [x] CHK008 Existe cenário de aceitação cobrindo explicitamente que o desfecho `200` da mesma operação permanece intocado, e não apenas que o `404` foi corrigido? [Coverage, Spec §User Story 1, cenário 3 — confirmado presente]

## Edge Case Coverage

- [x] CHK009 O caso de `instance` nunca ser preenchido pelo handler real (`handleProductNotFound`) está reconciliado com o schema genérico, evitando a impressão de que isso seria uma lacuna de implementação? [Edge Case, Spec §Edge Cases — confirmado presente]
- [ ] CHK010 Existe um requisito ou cenário cobrindo o caso em que o `$ref` aponta para um nome de schema inexistente ou com erro de digitação (referência quebrada), além de depender apenas do build para detectar isso? [Gap] — não é uma lacuna de escopo (a validação de build já é o mecanismo correto para este tipo de erro sintático), mas nenhuma seção do spec menciona esse caso explicitamente; achado de baixo impacto dado o tamanho trivial da mudança.

## Dependencies & Assumptions

- [x] CHK011 A única dependência da task (`T-002-7-1`, schema já declarado) está identificada com seu status atual, evitando que o plano assuma um schema ainda não disponível? [Dependency, Spec §Task de origem — confirmado presente, status `done`]
- [x] CHK012 A suposição de que `contracts-catalog` precisa ser reinstalado antes de o serviço recompilar está registrada, em vez de assumida implicitamente? [Assumption, Spec §Assumptions — confirmado presente]

## Ambiguities & Conflicts

- [x] CHK013 Nenhum conflito identificado entre `spec.md` e `plan.md` quanto ao elemento exato a ser adicionado — os dois documentos concordam palavra por palavra no bloco YAML-alvo. [Conflict — nenhum encontrado]

## Notes

- **Achado real (não apenas confirmação)**: CHK010 é uma lacuna de baixo impacto (LOW) — vale mencionar en passant em `tasks.md` que o build do módulo é o gate que detectaria uma referência quebrada, mas não justifica um requisito novo dado o tamanho trivial da mudança e o fato de o build já ser um passo obrigatório em toda task da cadeia `T-002-7`.
- Demais itens confirmam que o spec já cobre a dimensão testada.
- Checklist proporcionalmente mais curta que a de features maiores (ex.: `013-product-images-domain/checklists/domain.md`), reflexo direto do tamanho real da mudança — uma linha de bloco YAML em um arquivo já existente.
