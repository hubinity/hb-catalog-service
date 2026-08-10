# Feature Specification: Requisito de autorização da operação de leitura de saldo de estoque

**Feature Branch**: `005-stock-read-authz`

**Created**: 2026-07-24

**Status**: Draft

**Input**: User description: "detalhe a task T-001-5 do TASKS.json: 'Definir requisito de autorização da operação de leitura de saldo de estoque em contracts-catalog/openapi/catalog.yaml com base em NÃO ESPECIFICADO NO PRD'. Use o código real de platform-shared-contracts como contexto técnico absoluto."

**Task de origem**: `T-001-5` (TASKS.json, fase `contracts`) — depende de `T-001-4` (**concluída**). **Última task da cadeia T-001** (fecha o contrato da operação de leitura de saldo).

## Contexto técnico verificado (código real)

- **Contrato** (`catalog.yaml`): possui `info`, `servers`, `tags` (products, stock), 2 paths e schemas. **Não há `components/securitySchemes` nem `security`** — o documento inteiro é silente sobre autenticação/autorização. A operação `getStockItemByProductId` está completa exceto por este requisito.
- **Serviço** (`hb-catalog-service/config/SecurityConfig.java`): OAuth2 Resource Server (Keycloak realm `hibit`), JWT bearer, **stateless**. Regra efetiva: `/actuator/health*` e `/api/v1/_diagnostics/public` são `permitAll`; `/actuator/prometheus` e `/actuator/metrics/**` exigem `hasRole('admin')`; **`/api/**` exige `authenticated()`**; qualquer outra requisição é `denyAll`.
- **Padrão dos reads**: `getProductById` e os GET de produto/categoria **não têm `@PreAuthorize`** → caem em `authenticated()`. A leitura de saldo (GET, futura T-004-x) seguirá o mesmo padrão: **basta um JWT válido; não exige admin**. Apenas as mutações (stock movements/reservations, CRUD) usam `@PreAuthorize("hasRole('admin')")`.

## Decisões do usuário (registradas na confirmação do pipeline)

1. **Security scheme**: `type: http, scheme: bearer, bearerFormat: JWT` (nome `bearerAuth`) — o **primeiro `securityScheme` do documento**. Descreve "envie um JWT do Keycloak"; não modela o fluxo OAuth2 (irrelevante para o consumidor de leitura).
2. **Requisito e alcance**: `security: [ { bearerAuth: [] } ]` no **nível raiz** do documento (aplica a todas as operações) — a leitura de saldo herda "basta um JWT válido", espelhando `requestMatchers("/api/**").authenticated()` e o padrão dos reads.

## Decisão de escopo desta task

Entregáveis: (a) declarar `components/securitySchemes/bearerAuth` (HTTP bearer JWT); (b) declarar `security` global no nível raiz referenciando `bearerAuth`; (c) atualizar a `description` do Path Item para o estado final (todos os elementos da operação declarados — cadeia T-001 completa). Fora de escopo: modelagem de `permitAll`/exceções por operação (health/diagnostics não estão no contrato), roles de mutação (endpoints admin são outra decisão), implementação (T-004-x).

**Elementos alvo**:

```yaml
# components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: Keycloak-issued JWT (realm hibit) presented as a Bearer token.

# root level (após tags/servers, fora de paths):
security:
  - bearerAuth: []
```

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Consumidor sabe que precisa autenticar para ler o saldo (Priority: P1)

Uma equipe consumidora abre o contrato e vê, de forma explícita, que a operação de leitura de saldo (e as demais) exige um JWT bearer — o mesmo token do Keycloak que o serviço valida. Não precisa descobrir por tentativa-e-erro (401) nem ler o `SecurityConfig` do serviço.

**Why this priority**: É o entregável central e o fecho da cadeia T-001; sem ele o contrato afirma implicitamente que a operação é pública, o que é falso.

**Independent Test**: O documento tem `securitySchemes/bearerAuth` (http/bearer/JWT) e `security: [bearerAuth: []]` no nível raiz; build do módulo verde.

**Acceptance Scenarios**:

1. **Given** o contrato, **When** um consumidor inspeciona a segurança, **Then** encontra `bearerAuth` (HTTP bearer JWT) em `components/securitySchemes` e um requisito `security` global referenciando-o.
2. **Given** o requisito global, **When** o consumidor verifica a operação `getStockItemByProductId`, **Then** ela herda `bearerAuth` (sem `security` próprio que o sobrescreva).
3. **Given** a edição concluída, **When** o build do módulo roda, **Then** conclui sem erros.

---

### User Story 2 - Contrato reflete o modelo real de autenticação sem exagerar o requisito (Priority: P2)

Quem revisa o contrato confirma que o requisito declarado corresponde à realidade — autenticação por JWT bearer, **não** exigência de role admin na leitura — de modo que consumidores de leitura (ex.: sc-order-service na Fase 3) não sejam induzidos a achar que precisam de privilégios de administrador.

**Why this priority**: Um requisito exagerado (admin) no contrato desencorajaria integrações legítimas de leitura; a fidelidade ao serviço é o valor.

**Independent Test**: A operação de saldo não declara nenhum requisito de role/scope além de `bearerAuth`; nenhum `security` de operação contradiz o global.

**Acceptance Scenarios**:

1. **Given** a operação de leitura de saldo, **When** inspecionado seu requisito de segurança, **Then** exige apenas autenticação por `bearerAuth` (sem scopes/roles).
2. **Given** o esquema `bearerAuth`, **When** lido, **Then** sua `description` identifica o emissor (Keycloak realm `hibit`) sem acoplar o contrato a uma URL de ambiente.

---

### Edge Cases

- **Endpoints públicos do serviço não modelados**: `health`/`_diagnostics/public` são `permitAll` no serviço, mas **não constam no contrato** — o `security` global não os afeta (não existem aqui). Nenhuma exceção por operação é necessária nesta cadeia.
- **Security global vs. operação `getProductById`**: o requisito raiz passa a valer também para `getProductById` — coerente com a realidade (`/api/**` autenticado). É mudança aditiva de requisito, sem editar o bloco daquela operação.
- **Distinção read vs. mutação**: o contrato ainda não modela as mutações de estoque (fora da cadeia T-001); o requisito global `bearerAuth` cobre "autenticado", e o eventual reforço de admin nas mutações é decisão de uma task futura — não desta.
- **`bearerFormat` informativo**: `bearerFormat: JWT` é documentacional (não valida); serve para o consumidor saber o tipo de token.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O documento MUST declarar `components/securitySchemes/bearerAuth` com `type: http`, `scheme: bearer`, `bearerFormat: JWT` e uma `description` que identifique o emissor (Keycloak realm `hibit`) sem URL de ambiente.
- **FR-002**: O documento MUST declarar um requisito `security` global no nível raiz: `- bearerAuth: []`, aplicando-se a todas as operações (incluindo `getStockItemByProductId`).
- **FR-003**: A operação de leitura de saldo MUST NOT declarar `security` próprio, scopes ou roles — herda o requisito global (autenticação apenas), fiel ao padrão dos reads do serviço.
- **FR-004**: A `description` do Path Item MUST ser atualizada para o estado final: operação, parâmetro, corpo de resposta e autorização declarados — cadeia T-001 completa (sem menção a tasks pendentes).
- **FR-005**: A mudança MUST ser aditiva/restrita: adiciona `securitySchemes` (em `components`) e `security` (raiz), e reescreve a `description` do Path Item; paths, operações, parâmetros e schemas existentes permanecem intactos; documento válido pela autoridade herdada (build do módulo).
- **FR-006**: A entrega MUST provar regressão zero no consumidor (`mvn -B verify` verde em `hb-catalog-service` com o artefato reinstalado).

### Key Entities

- **Security scheme `bearerAuth` (novo)**: descreve o mecanismo de autenticação (HTTP bearer JWT do Keycloak); primeiro do documento.
- **Requisito `security` global (novo)**: aplica `bearerAuth` a todas as operações no nível raiz.
- **Path Item do saldo (existente — description finalizada)**: cadeia T-001 encerrada.
- **`SecurityConfig.java` (serviço — fonte de verdade, intocado)**: define que `/api/**` exige autenticação; reads não exigem admin.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: O contrato declara explicitamente o requisito de autenticação (bearerAuth) aplicável à leitura de saldo — verificável por inspeção; nenhum consumidor precisa inferir por 401 ou ler o serviço.
- **SC-002**: O documento permanece válido e processável: build do módulo verde após a edição.
- **SC-003**: O requisito declarado corresponde ao serviço (autenticação, não admin) — verificável comparando o `security` da operação com o padrão `authenticated()` de `/api/**` e a ausência de `@PreAuthorize` nos reads.
- **SC-004**: A cadeia T-001 (T-001-1..5) fica **completa** — a operação de leitura de saldo está integralmente especificada no contrato (endereço, verbo, parâmetro, corpo, autorização).

## Assumptions

- As 2 decisões estruturais foram tomadas pelo usuário na confirmação do pipeline (2026-07-24): esquema HTTP bearer JWT e `security` global — ambas fiéis ao `SecurityConfig` verificado.
- Modelar `security` global (não por operação) é aceitável porque todas as operações hoje no contrato exigem autenticação no serviço; endpoints `permitAll` reais (health/diagnostics) não estão no contrato.
- Não modelar OAuth2/OIDC flows é intencional: roles vêm de `realm_access.roles` (não de scopes OAuth); o consumidor de leitura só precisa saber "envie um JWT bearer".
- Autoridade de validação e workflow herdados da cadeia (build do módulo; branch `feature/stock-balance-path`; commits no polish).
- O reforço de `hasRole('admin')` nas mutações de estoque não é modelado aqui — as mutações não fazem parte da cadeia T-001 e entrariam em task própria.

## Out of Scope

- Requisitos de segurança das operações de mutação (movements/reservations) e do CRUD admin.
- Modelagem de exceções `permitAll` por operação (endpoints públicos não constam no contrato).
- Modelagem de OAuth2/OpenID Connect flows e scopes.
- Implementação do endpoint e sua anotação de segurança no serviço (T-004-x).
