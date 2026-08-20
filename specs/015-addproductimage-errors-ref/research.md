# Phase 0 Research: Referenciar ProblemDetail nos desfechos 400/403/404 de addProductImage

Nenhum `NEEDS CLARIFICATION` no Technical Context do `plan.md`. As decisões de fundo (schema a referenciar, sintaxe de referência) já foram tomadas em `T-002-7-1`/`T-002-7-2`/`T-002-7-3`; esta feature as reaplica aos três desfechos de erro de `addProductImage`.

## 1. Como referenciar o schema em cada desfecho de erro

- **Decision**: acrescentar `content.application/json.schema.$ref: '#/components/schemas/ProblemDetail'` a cada um dos três desfechos (`400`, `403`, `404`) já existentes, preservando suas `description` individuais.
- **Rationale**: mesma sintaxe já aplicada em `getProductById` (`T-002-7-2`) e `getStockItemByProductId` (`T-002-7-3`) — consistência entre os cinco usos reais do schema no documento.
- **Alternatives considered**: nenhuma — reaplicação direta de um padrão já validado três vezes.

## 2. Por que a ausência de implementação da operação não bloqueia esta feature

- **Decision**: prosseguir com as três referências mesmo sem `ProductController`/`ProductService` implementarem `addProductImage` ainda.
- **Rationale**: o contrato já descreve a operação por completo (requestBody, resposta `201`, os três desfechos de erro) — a implementação (`T-002`/`T-003`) é deliberadamente posterior, mesmo padrão usado para leitura de saldo de estoque (`T-002-7-3`/`T-004`). Referenciar um schema de erro no contrato não pressupõe que o erro já seja produzido em runtime; é uma declaração de shape para quem for consumir ou implementar depois.
- **Alternatives considered**: adiar `T-002-7-4..6` até `T-002`/`T-003` implementarem a operação — descartado: contradiria o padrão já estabelecido no projeto e criaria uma dependência artificial entre cadeias que o `TASKS.json` não declara (nenhuma das três tasks tem `T-002`/`T-003` em `depends_on`).

## 3. Por que agrupar três desfechos em uma única feature, em vez de uma feature por desfecho

- **Decision**: cobrir `400`, `403` e `404` de `addProductImage` em uma única feature/spec, diferente do padrão de `013`/`014` (uma operação por feature).
- **Rationale**: as três tasks (`T-002-7-4/5/6`) compartilham a mesma operação, o mesmo schema-alvo (`ProblemDetail`) e o mesmo padrão de mudança pontual (`content` acrescentado, `description` preservada). Tratá-las como três features separadas replicaria a mesma decisão de design três vezes sem agregar valor de revisão adicional — ao contrário de `013`/`014`, que cobriam operações distintas (`getProductById` vs. `getStockItemByProductId`) e, portanto, mereciam specs independentes.
- **Alternatives considered**: uma feature por desfecho (mesmo padrão de `013`/`014`) — rejeitada por criar três specs quase idênticas para a mesma operação, aumentando overhead de revisão sem reduzir risco.

## 4. Escopo: os três desfechos de erro de uma única operação, nada além disso

- **Decision**: tocar apenas `400`/`403`/`404` de `addProductImage` nesta feature.
- **Rationale**: mesma decomposição atômica já usada em `T-002-7-2`/`T-002-7-3` — referências pontuais seguindo a cadeia de `depends_on` já registrada em `TASKS.json`, apenas agrupadas por operação em vez de uma task isolada.
- **Alternatives considered**: N/A — mesma justificativa das tasks irmãs.
