# Phase 0 Research: Referenciar ProblemDetail no 404 de getStockItemByProductId

Nenhum `NEEDS CLARIFICATION` no Technical Context do `plan.md`. As decisões de fundo (schema a referenciar, sintaxe de referência) já foram tomadas em `T-002-7-1`/`T-002-7-2`; esta task as reaplica ao segundo desfecho de erro.

## 1. Como referenciar o schema no desfecho de erro

- **Decision**: acrescentar `content.application/json.schema.$ref: '#/components/schemas/ProblemDetail'` ao desfecho `404` já existente, preservando sua `description`.
- **Rationale**: mesma sintaxe já aplicada em `getProductById` (`T-002-7-2`) — consistência entre os dois primeiros usos reais do schema.
- **Alternatives considered**: nenhuma — reaplicação direta de um padrão já validado.

## 2. Por que a ausência de implementação da rota não bloqueia esta task

- **Decision**: prosseguir com a referência mesmo sem `StockController`/`StockService` implementarem `getStockItemByProductId` ainda.
- **Rationale**: o contrato já descreve a operação por completo desde a cadeia `T-001` (parâmetro, resposta `200`, autorização) — a implementação (`T-004`) é deliberadamente posterior, mesmo padrão usado para imagens de produto (`T-002` antes de `T-003`/`T-005`). Referenciar um schema de erro no contrato não pressupõe que o erro já seja produzido em runtime; é uma declaração de shape para quem for consumir ou implementar depois.
- **Alternatives considered**: adiar `T-002-7-3` até `T-004` implementar a rota — descartado: contradiria o padrão já estabelecido no projeto e criaria uma dependência artificial entre cadeias que o `TASKS.json` não declara (`T-002-7-3` não tem `T-004` em `depends_on`).

## 3. Escopo de uma única operação/desfecho por task

- **Decision**: tocar apenas o `404` de `getStockItemByProductId` nesta task.
- **Rationale**: mesma decomposição atômica já usada em `T-002-7-2` — uma referência por task, seguindo a cadeia de `depends_on` já registrada em `TASKS.json`.
- **Alternatives considered**: N/A — mesma justificativa da task irmã.
