# Phase 0 Research: Referenciar ProblemDetail no 404 de getProductById

Nenhum `NEEDS CLARIFICATION` no Technical Context do `plan.md` — a única decisão de fundo (qual schema referenciar, com qual shape) já foi tomada e ratificada em `T-002-7-1` (spec `012-problemdetail-schema`). Esta task não introduz nenhuma decisão nova, apenas consome a anterior.

## 1. Como referenciar o schema no desfecho de erro

- **Decision**: acrescentar `content.application/json.schema.$ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` já existente, preservando sua `description`.
- **Rationale**: é o padrão OpenAPI 3.1 canônico para declarar o corpo de uma resposta; nenhum outro desfecho do documento usa uma forma diferente para declarar `content` (todos os desfechos com corpo, como o `200` de `getProductById`, usam exatamente essa estrutura).
- **Alternatives considered**: nenhuma — não há decisão de design aqui, apenas aplicar a mesma sintaxe já usada em todo o restante do documento.

## 2. Por que não alterar `ApiExceptionHandler.java`

- **Decision**: nenhuma mudança de código no `hb-catalog-service`.
- **Rationale**: `handleProductNotFound` já produz exatamente o shape `ProblemDetail` (verificado em código: `ProblemDetail.forStatusAndDetail`, `setTitle`, `setType`) — o contrato estava atrasado em relação ao código, não o contrário. Esta task fecha essa lacuna documentando o que já é verdade em runtime.
- **Alternatives considered**: N/A — não há necessidade de mudar comportamento, apenas de documentá-lo.

## 3. Escopo de uma única operação/desfecho por task

- **Decision**: tocar apenas o `404` de `getProductById` nesta task, deixando os outros quatro desfechos de erro (`T-002-7-3..6`) intactos.
- **Rationale**: `T-002-7` foi deliberadamente decomposta em seis subtarefas atômicas (`T-002-7-1..6`), cada uma com `decomposition_allowed: false` e uma cadeia de `depends_on` — a intenção registrada em `TASKS.json` é uma referência por task, não um lote.
- **Alternatives considered**: referenciar todos os cinco desfechos de erro pendentes em uma única mudança — descartado: contradiria a decomposição já registrada no tracker e o escopo desta execução (`T-002-7-2` especificamente).
