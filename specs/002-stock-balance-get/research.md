# Research: Operação GET no path canônico de saldo de estoque (T-001-2)

**Date**: 2026-07-22 · **Plan**: [plan.md](./plan.md)

Incertezas do Technical Context resolvidas contra o código real e as decisões herdadas da feature 001. Nenhum NEEDS CLARIFICATION permanece.

## R1. Identidade da operação

- **Decision**: `get` com `operationId: getStockItemByProductId`, `summary: Fetch the current stock balance for a product`, `tags: [stock]`.
- **Rationale**: Derivação direta da única operação existente (`getProductById`): formato `get<Recurso>By<Chave>` (recurso = schema `StockItem`, chave = `productId`), summary imperativo, tag declarada. Unicidade de `operationId` é trivial (só existem 2 operações no documento).
- **Alternatives considered**: `getStockBalance` (rejeitado — não segue o padrão `By<Chave>` nem nomeia o schema); reutilizar tag `products` (rejeitado — estoque é domínio próprio; T-004-x terá controller próprio `StockController`).

## R2. Parâmetro de path não declarado (risco central — contingência FR-006)

- **Decision**: Tentar primeiro a entrega **somente com a operação** (honrando o escopo T-001-2). Gatilho objetivo da contingência: falha do `mvn -B install` com mensagem de validação do swagger-parser/openapi-generator citando parâmetro de path ausente (classe de erro "missing path parameter 'productId'" / "declared path parameter ... needs to be defined"). Se disparar, incluir no **mesmo commit** o bloco `parameters` mínimo (name/in/required/schema uuid), registrando que T-001-3 permanece dona da especificação fina do parâmetro (description e refinamentos).
- **Rationale**: O `openapi-generator-maven-plugin` valida o spec por padrão (`skipValidateSpec=false`) e a regra de parâmetros de template não declarados é **provável** de disparar com operação presente (com Path Item sem operação, T-001-1, não disparou — validado empiricamente pelo build verde da feature 001). A probabilidade de acionamento da contingência é alta; o spec já a documenta em FR-006 e Out of Scope.
- **Alternatives considered**: Declarar o parâmetro preventivamente (rejeitado — invade T-001-3 sem necessidade comprovada; se o build passar sem ele, o escopo fica limpo); desligar a validação do plugin (`skipValidateSpec=true`) (rejeitado — enfraquece a autoridade de validação estabelecida).

## R3. Resposta sem `content` / responses mínimas

- **Decision**: `'200'` e `'404'` apenas com `description` (textos fixados em FR-004); nenhuma chave `content`.
- **Rationale**: No OpenAPI 3.1, o Response Object exige apenas `description`; `content` é opcional. A geração DTO-only (`generateModels=true`, `generateApis=false`) não depende de `content` em operações — modelos vêm de `components/schemas`, intocados aqui. O corpo da `'200'` (ref a `StockItem`/`StockItemResponse`) é exatamente o entregável de T-001-4.
- **Alternatives considered**: Referenciar `StockItem` já no content (rejeitado — antecipa T-001-4, inclusive a decisão de nome do schema de resposta).

## R4. Semântica do `'404'` único

- **Decision**: Um único desfecho `'404'` cobre produto inexistente **e** produto sem registro de saldo; texto: `Stock balance not found for the given product (unknown product or no stock record)`.
- **Rationale**: Decisão explícita do usuário na revisão do checklist (CHK005). Padrão REST para sub-recurso singular; a distinção fina, se necessária, será modelada no corpo de erro (ProblemDetail) em T-001-4. Vinculante para a implementação T-004-x.
- **Alternatives considered**: Distinguir os casos por descriptions/códigos distintos (rejeitado pelo usuário — modelagem extra agora e antecipação de T-001-4).

## R5. Ajuste da description do Path Item (FR-007)

- **Decision**: Reescrever a `description` do Path Item para declarar que a operação GET existe e que o parâmetro `productId`, o corpo da resposta e a autorização são declarados por T-001-3..T-001-5. `summary` do Path Item permanece.
- **Rationale**: O texto herdado de T-001-1 afirmaria falsamente que a operação GET ainda está pendente; contrato é lido por consumidores externos e não pode mentir sobre o próprio estado.
- **Alternatives considered**: Remover a description (rejeitado — perderia o aviso de que parâmetro/corpo/auth ainda vêm); manter intacta (rejeitado — ficaria factualmente incorreta).

## R6. Herança de autoridade de validação e workflow

- **Decision**: Sem mudanças — `( cd platform-shared-contracts && mvn -B -DskipTests install )` como gate (feature 001, R3); branch `feature/stock-balance-path` já ativa nos dois repos (T003 da feature 001); commits na fase final de polish.
- **Rationale**: Cadeia T-001-x compartilha branch e pipeline; reabrir essas decisões a cada task seria retrabalho sem ganho.
- **Alternatives considered**: Branch por task (rejeitado — a cadeia é uma única entrega lógica de contrato; commits incrementais por task já dão rastreabilidade).

## Adendo a R2 — contingência FR-006 acionada

- **Resultado observado em 2026-07-22**: o primeiro `mvn -B -DskipTests install`, executado após a inclusão da operação `get`, falhou no `contracts-catalog` com `Declared path parameter productId needs to be defined as a path parameter in path or operation level`.
- **Ação aplicada**: incluído na operação somente o bloco mínimo previsto em FR-006/R2 (`name: productId`, `in: path`, `required: true`, schema `string`/`uuid`). A T-001-3 permanece responsável pela descrição e pelos refinamentos do parâmetro.
