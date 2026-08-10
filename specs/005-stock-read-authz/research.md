# Research: Requisito de autorização da operação de leitura de saldo (T-001-5)

**Date**: 2026-07-24 · **Plan**: [plan.md](./plan.md)

Decisão sem precedente interno (primeiro `securityScheme` do contrato). As escolhas estruturais foram tomadas pelo usuário na confirmação do pipeline, ancoradas no `SecurityConfig` real. Nenhum NEEDS CLARIFICATION permanece.

## R1. Tipo de security scheme (decisão do usuário)

- **Decision**: `bearerAuth` — `type: http`, `scheme: bearer`, `bearerFormat: JWT`, com `description` identificando o emissor (Keycloak realm `hibit`).
- **Rationale**: Decisão explícita do usuário. O serviço é OAuth2 **Resource Server** (valida JWTs), não Authorization Server — o consumidor de leitura só precisa "apresentar um JWT bearer". HTTP bearer é o esquema OpenAPI mínimo e correto para Resource Server; `bearerFormat: JWT` é documentacional.
- **Alternatives considered**: `openIdConnect` (rejeitado pelo usuário — acopla o contrato ao `openIdConnectUrl` de ambiente, verboso); `oauth2` com flows/scopes (rejeitado — roles vêm de `realm_access.roles`, não de scopes OAuth; declararia endpoints/scopes que o serviço não usa).

## R2. Alcance do requisito (decisão do usuário)

- **Decision**: `security: [ - bearerAuth: [] ]` no **nível raiz** do documento; a operação de saldo **não** declara `security` próprio (herda o global).
- **Rationale**: Decisão explícita do usuário. `SecurityConfig` exige `authenticated()` para todo `/api/**`; todas as operações hoje no contrato (produtos, categorias, saldo) estão sob `/api/**` — logo o requisito é uniforme e o nível raiz o expressa sem repetição. Reads não têm `@PreAuthorize` → apenas autenticação, sem role.
- **Alternatives considered**: `security` por operação (rejeitado pelo usuário — deixaria as demais operações sem requisito declarado, inconsistente com a realidade de que todas exigem auth); exigir role admin (rejeitado — falsificaria o requisito; reads não são admin-only).

## R3. Efeito colateral sobre getProductById

- **Decision**: Aceito e documentado (Edge Case). O `security` raiz passa a valer para `getProductById` também.
- **Rationale**: Coerente com a realidade (`/api/**` autenticado); é mudança de requisito **aditiva** que não edita o bloco daquela operação. Nenhuma operação do contrato é `permitAll` no serviço (os `permitAll` reais — health, `_diagnostics/public` — não constam no contrato), então não há operação que precise de override `security: []`.
- **Alternatives considered**: Aplicar `security` só na operação de saldo para evitar o efeito colateral (rejeitado — subdeclararia o requisito das outras operações, contrariando o serviço).

## R4. Endpoints públicos e mutações não modelados

- **Decision**: Não modelar exceções `permitAll` nem reforço de admin das mutações nesta task.
- **Rationale**: Os endpoints `permitAll` (health/diagnostics) não estão no contrato — nada a excepcionar. As mutações de estoque não fazem parte da cadeia T-001 e não estão modeladas no contrato; o reforço `hasRole('admin')` delas é decisão de task futura. O `security` global `bearerAuth` estabelece corretamente o piso "autenticado" para tudo que existe no contrato.
- **Alternatives considered**: Antecipar `security` com scope/role admin nas (ainda inexistentes) operações de mutação (rejeitado — fuga de escopo; operações nem existem no contrato).

## R5. Impacto na geração DTO-only

- **Decision**: Nenhum. `securitySchemes` e `security` não geram código no modo `generateModels=true`/`generateApis=false`; apenas passam pelo parse do swagger-parser.
- **Rationale**: DTOs vêm de `components/schemas`; blocos de segurança são metadados de API ignorados pela geração de modelos. Build permanece verde (autoridade de validação herdada).
- **Alternatives considered**: n/a.

## R6. Herança de autoridade de validação e workflow

- **Decision**: Sem mudanças — build do módulo como gate; branch `feature/stock-balance-path`; commits no polish. Este é o **commit de fechamento da cadeia T-001**.
- **Rationale**: Padrão estável.
- **Alternatives considered**: n/a (herdado).
