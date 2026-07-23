# Research: Schema de resposta da operação de leitura de saldo (T-001-4)

**Date**: 2026-07-23 · **Plan**: [plan.md](./plan.md)

As duas incertezas estruturais da task foram resolvidas pelo usuário na confirmação do pipeline; o restante deriva do código real. Nenhum NEEDS CLARIFICATION permanece.

## R1. Nome do schema (decisão do usuário)

- **Decision**: Reusar `components/schemas/StockItem` como corpo da `'200'` — nenhum schema `StockItemResponse` é criado no contrato.
- **Rationale**: Decisão explícita do usuário (2026-07-23). Evita dois DTOs gerados quase idênticos; o rótulo "StockItemResponse" do tracker nomeia a entrega. Fecha o achado L2 da feature 002.
- **Alternatives considered**: Criar `StockItemResponse` separado (rejeitado pelo usuário — duplicação); renomear `StockItem`→`StockItemResponse` (rejeitado — churn no nome do DTO gerado sem ganho).

## R2. Reconciliação de campos (decisão do usuário)

- **Decision**: Reescrever o schema para os 5 campos do record real `api/dto/StockItemResponse.java`: `productId` (uuid), `available`/`reserved`/`reorderPoint` (int32, `minimum: 0`), `updatedAt` (date-time). `required: [productId, available, reserved, reorderPoint]`.
- **Rationale**: Decisão explícita do usuário — o serviço é fonte de verdade. Os campos antigos (`quantityOnHand` int64, `reorderLevel`, `lastMovementAt`) eram especulativos (bootstrap da Fase 0, anterior à implementação real) e nunca foram consumidos. `updatedAt` fora do required: pode ser nulo antes da primeira mudança de contadores; os 4 demais são inicializados no INSERT (entity defaults = 0). Encerra a divergência registrada desde a feature 001.
- **Alternatives considered**: Manter campos antigos com mapeamento no serviço (rejeitado pelo usuário — perderia `reserved`, tradução artificial); híbrido aditivo (rejeitado pelo usuário — schema inchado com campos mortos).

## R3. Segurança da mudança breaking

- **Decision**: Reescrita direta, sem versionamento/depreciação; prova empírica obrigatória via `mvn -B verify` no consumidor (FR-006).
- **Rationale**: ADR 0006 — artefato SNAPSHOT build-only, sem publicação remota nem consumidores externos. Verificação no código real: nenhuma classe de `hb-catalog-service` importa `com.hubinity.contracts.catalog.dto.StockItem` (dependência declarada no pom, classes geradas não referenciadas — registrado no TASKS.json §contract_compliance). Modo de falha coberto: se o verify falhar por referência ao DTO antigo, a correção do ponto de uso entra nesta entrega.
- **Alternatives considered**: Depreciar campos antigos gradualmente (rejeitado — cerimônia sem consumidores para proteger); bump de versão (rejeitado — ADR 0005/0006, fase SNAPSHOT).

## R4. Estrutura do content da '200'

- **Decision**: `content: application/json: schema: $ref: '#/components/schemas/StockItem'` — estrutura idêntica à `'200'` de `getProductById`; `description` da resposta permanece a fixada pela feature 002.
- **Rationale**: Convenção única existente no documento; DTO-only generation consome o `$ref` normalmente (modelos vêm de components).
- **Alternatives considered**: Schema inline na resposta (rejeitado — quebraria a convenção e impediria reuso do DTO gerado).

## R5. Description do Path Item

- **Decision**: Texto fixado em FR-004 — operação, parâmetro e corpo declarados; pendência restante: autorização (T-001-5).
- **Rationale**: Progressão de estado estabelecida pela cadeia (features 002 R5, 003 R3).
- **Alternatives considered**: n/a (padrão consolidado).

## R6. Corpo de erro do '404'

- **Decision**: Não modelar ProblemDetail no contrato nesta cadeia.
- **Rationale**: RFC 7807 é padrão transversal do serviço (`ApiExceptionHandler`); modelá-lo no contrato é decisão de plataforma (afetaria todas as operações, mereceria ADR próprio), não desta task.
- **Alternatives considered**: Adicionar schema ProblemDetail + content no '404' (rejeitado — fuga de escopo; inconsistente se feito só numa operação).

## R7. Herança de autoridade de validação e workflow

- **Decision**: Sem mudanças — build do módulo como gate; branch `feature/stock-balance-path`; commits no polish (padrão T007/T010 da feature 003).
- **Rationale**: Padrão estável da cadeia.
- **Alternatives considered**: n/a (herdado).
