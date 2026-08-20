# 🛒 Sistema de Catálogo de Produtos — HiBit - (Backend) - Ecossistema Hubinity - In Progress
> Parte integrante do ecossistema distribuído Hubinity.

---

## 💻 Visão Geral
- **O que faz:** Microsserviço **source-of-truth do catálogo HiBit** — gerencia produtos (com histórico de preço), categorias (árvore), estoque (disponível/reservado/reorder point), movimentações de estoque (journal append-only) e reservas com TTL. Expõe REST API alinhada ao contrato `contracts-catalog` e publica eventos de mudança de produto/preço/estoque na exchange RabbitMQ `catalog.events` via **Transactional Outbox** (ver [`README-outbox.md`](./README-outbox.md)).
- **Problema que resolve:** Concentra em um único serviço a verdade sobre "o que existe à venda" no ecossistema HiBit, evitando estoque divergente entre cashier/support/order. Os eventos publicados permitem que `sc-order-service` e `hb-cashier-service` se mantenham eventualmente consistentes sem chamadas síncronas no caminho crítico.
- **Posicionamento no Ecossistema:** Backend Spring Boot rodando no realm Keycloak `hibit`. **Phase 1 em construção** — endpoints de categorias, produtos e estoque (movimentações + reservas) no ar, com idempotência e outbox implementados. Pendências: remoção do package `_diagnostics`, hardening de produção e consumers downstream.

## 🏗️ Papel na Arquitetura
- **Tipo de Componente:** Microsserviço REST + publisher de eventos AMQP (via outbox).
- **Responsabilidades Principais:**
  - Persistir e expor entidades do domínio catálogo (`Category`, `Product`, `PriceHistory`, `StockItem`, `StockMovement`, `StockReservation`).
  - Validar JWTs emitidos pelo realm `hibit` (OAuth2 Resource Server) e aplicar autorização via roles realm-scoped.
  - Publicar eventos de domínio em `catalog.events` quando o estado mudar (`ProductCreated`, `ProductUpdated`, `ProductDeactivated`, `PriceChanged`, `StockChanged`).
  - Garantir idempotência das operações mutantes de estoque via header `Idempotency-Key` (tabela durável `idempotency_key`).
  - Expor métricas Prometheus, tracing OTLP e health probes via Actuator.
- **Limites e Fronteiras (Boundaries):**
  - **Não** processa pedidos (responsabilidade de `sc-order-service`) nem cobranças (responsabilidade de `hb-cashier-service`).
  - **Não** emite JWTs — apenas valida; emissão é do Keycloak.
  - **Não** modela cross-realm — para servir o `sc-order-service` (realm `star-coffee`) há um scope `catalog:read` planejado para gateway na Fase 5.

## 🔗 Dependências e Comunicação
### Serviços Internos da Hubinity
- **`platform-iam`** (realm `hibit`) — fonte dos JWTs validados pelo OAuth2 Resource Server.
- **`platform-shared-contracts`** — JARs `contracts-catalog:0.1.0-SNAPSHOT` (DTOs) e `contracts-events:0.1.0-SNAPSHOT` (schemas de evento).
- **`platform-infra`** — stack local (postgres + rabbitmq + keycloak) usada em dev; também builda este serviço quando o profile `catalog` é ativado.

### Infraestrutura e Serviços Externos
- **PostgreSQL** — Supabase em cloud (PG 18), container `postgres:16-alpine` em dev local.
- **RabbitMQ** — CloudAMQP em cloud, container `rabbitmq:3.13-management-alpine` em dev local. Este serviço é **publisher** na exchange `catalog.events` (topic durable, com DLX/DLQ).
- **Keycloak** — Railway Hobby em cloud, container `quay.io/keycloak/keycloak:26.0` em dev local.

## 🛠️ Tecnologias e Ferramentas
| Camada | Tecnologia | Versão |
| :--- | :--- | :--- |
| Linguagem | Java | 21 (LTS, Temurin) |
| Framework | Spring Boot (parent) | 4.1.0 |
| Web / Validation | spring-boot-starter-web + starter-validation | (gerenciado pelo parent) |
| Persistence | spring-boot-starter-data-jpa + Hibernate | (gerenciado pelo parent) |
| Migrations | spring-boot-starter-flyway + flyway-database-postgresql | (gerenciado pelo parent) |
| JDBC | postgresql (runtime scope) | (gerenciado pelo parent) |
| Messaging | spring-boot-starter-amqp | (gerenciado pelo parent) |
| Security | starter-security + starter-oauth2-resource-server | (gerenciado pelo parent) |
| Cache | starter-cache + Caffeine | (gerenciado pelo parent) |
| Resilience | resilience4j-spring-boot4 + resilience4j-reactor | 2.4.0 |
| Mapping | MapStruct (processor model `spring`) | 1.6.3 |
| API Docs | springdoc-openapi-starter-webmvc-ui | 3.0.3 |
| Observabilidade | actuator + micrometer-tracing-bridge-otel + otlp exporter + micrometer-registry-prometheus | (gerenciado pelo parent) |
| Test | starter-test + starter-webmvc-test + spring-security-test | 4.1.0 / 7.1.0 |
| Integration test | Testcontainers (junit-jupiter + postgresql + rabbitmq) | 1.20.4 |
| Coverage | jacoco-maven-plugin | 0.8.12 |
| Build | Maven | 3.9.x |
| Container | `eclipse-temurin:21-jre-alpine` runtime / `maven:3.9-eclipse-temurin-21-alpine` builder | — |
| Contratos internos | `com.hubinity:contracts-catalog` + `contracts-events` | 0.1.0-SNAPSHOT |

## 📐 Padrões de Projeto e Arquitetura do Código
- **Estilo Arquitetural:** **Layered DDD-lite** — `domain` (entidades + repositórios JPA, sem lógica de negócio), `service` (lógica de negócio, uma classe por aggregate), `api` (controllers + DTO + mappers + error handling + idempotência), `events`/`integration` (payloads de evento + outbox dispatcher + publicação AMQP), `config` (security, auditing, Rabbit topology).
- **Padrões Relevantes:**
  - **Contract-first** — DTOs vêm do `platform-shared-contracts`; controllers são manualmente alinhados aos endpoints OpenAPI.
  - **Transactional Outbox** — eventos persistidos em `outbox_messages` na mesma TX do negócio, despachados por `@Scheduled` job com `FOR UPDATE SKIP LOCKED` e DLQ após 5 tentativas. Entrega **at-least-once**: consumers deduplicam por `messageId`. Ver [`README-outbox.md`](./README-outbox.md).
  - **Idempotência via claim-row** — `IdempotencyFilter` roda antes do `DispatcherServlet` nos quatro endpoints mutantes de estoque; registros duráveis na tabela `idempotency_key` (retenção 24h).
  - **Concorrência de estoque via `UPDATE` condicional** — `UPDATE … WHERE available >= :qty`; o row lock do próprio statement é a única primitiva de concorrência (sem `SELECT … FOR UPDATE`).
  - **RFC 7807 ProblemDetail** — todas as respostas de erro via `ApiExceptionHandler` + exceções tipadas em `api/error/`.
  - **Soft delete via `deleted_at TIMESTAMPTZ`** (ver ADR 0011) — aplicado em `category` e `product`; journals (`stock_movement`, `price_history`) não.
  - **UUID v7 PKs gerados DB-side** (ver ADR 0009) — função `uuidv7()` provisionada pelo `V1__init.sql`.
  - **MapStruct para DTO mapping** (ver ADR 0010) — `defaultComponentModel=spring`, `unmappedTargetPolicy=IGNORE`.
  - **`SecurityContextAuditorAware`** popula `created_by`/`updated_by` automaticamente a partir do `preferred_username` do JWT.
  - **Role mapping JWT → Spring authorities** via `KeycloakRealmRoleConverter` (ver ADR 0002) — funde roles realm-scoped e client-scoped sob prefixo `ROLE_`.

## 📂 Estrutura do Projeto
```text
hb-catalog-service/
├── README.md / README-outbox.md / CLAUDE.md
├── pom.xml                                       # parent: spring-boot 4.1.0
├── Dockerfile                                    # multi-stage: maven builder + temurin-21-jre-alpine runtime
├── CONTRIBUTING.md / LICENSE
├── docs/
│   ├── adr/                                      # ADRs 0002, 0003, 0009, 0010, 0011
│   └── ANALISE_ARQUITETURAL_2026-07.md
└── src/
    ├── main/
    │   ├── java/com/hubinity/catalog/
    │   │   ├── HbCatalogServiceApplication.java
    │   │   ├── api/
    │   │   │   ├── CategoryController / ProductController / StockController
    │   │   │   ├── _diagnostics/                 # ⚠️ TEMPORÁRIO — pendente de remoção
    │   │   │   ├── dto/                          # request/response records (inclui page responses e results)
    │   │   │   ├── error/                        # ApiExceptionHandler + exceções tipadas (RFC 7807)
    │   │   │   ├── idempotency/                  # IdempotencyFilter + IdempotencyService
    │   │   │   └── mapper/                       # MapStruct (Category, Product, StockItem/Movement/Reservation)
    │   │   ├── config/                           # SecurityConfig, KeycloakRealmRoleConverter, JpaAuditingConfig,
    │   │   │                                     # SecurityContextAuditorAware, RabbitConfig, IdempotencyFilterConfig
    │   │   ├── domain/                           # Category, Product, PriceHistory, StockItem, StockMovement,
    │   │   │                                     # StockReservation, OutboxMessage, IdempotencyRecord + repositórios + enums
    │   │   ├── events/published/                 # ProductCreated/Updated/Deactivated, PriceChanged, StockChanged
    │   │   ├── integration/                      # EventPublisher, DefaultEventPublisher, OutboxDispatcher,
    │   │   │                                     # OutboxHousekeepingJob, CatalogEvent (routing keys)
    │   │   └── service/                          # CategoryService, ProductService, StockService, ReservationExpiryService
    │   └── resources/
    │       ├── application.yml                   # config base (+ knobs do outbox em app.outbox.*)
    │       ├── application-local.yml             # defaults hardcoded localhost
    │       ├── application-staging.yml           # tudo via env
    │       ├── application-prod.yml              # tudo via env, INFO root, Swagger OFF
    │       └── db/migration/                     # V1__init, V2__price_history, V3__stock_idempotency,
    │                                             # V4__create_outbox_messages
    └── test/
        └── java/com/hubinity/catalog/            # unit (service/, api/, config/) + ITs Testcontainers (domain/, db/)
```

## ⚙️ Configuração e Variáveis de Ambiente

### Perfis Spring
| Profile   | Quando se ativa             | Notas                                                                  |
| --------- | --------------------------- | ---------------------------------------------------------------------- |
| `local`   | default (`mvn spring-boot:run`) | Defaults hardcoded localhost para DB/MQ/Keycloak.                  |
| `test`    | `mvn test`                  | Exclui DataSource/JPA/Flyway/Rabbit auto-config — totalmente offline.  |
| `staging` | Railway / GitHub Actions    | Todas credenciais via env.                                             |
| `prod`    | deploy produção             | `INFO` root, `WARN` Spring, Swagger UI desligado.                      |

Default: `local` (override via `SPRING_PROFILES_ACTIVE`).

### Variáveis (staging / prod)
```bash
# Spring
SPRING_PROFILES_ACTIVE=staging          # ou prod
SERVER_PORT=8080                        # opcional, default 8080

# PostgreSQL
HB_CATALOG_DB_URL=jdbc:postgresql://<host>:5432/<db>
HB_CATALOG_DB_USERNAME=<user>
HB_CATALOG_DB_PASSWORD=<pwd>

# RabbitMQ (CloudAMQP)
CLOUDAMQP_URL=amqps://<user>:<pwd>@<host>/<vhost>

# Keycloak
KEYCLOAK_ISSUER_URI=https://iam.hubinity.io/realms/hibit
KEYCLOAK_JWK_URI=https://iam.hubinity.io/realms/hibit/protocol/openid-connect/certs
HB_CATALOG_KEYCLOAK_CLIENT_ID=hb-catalog-service     # opcional, default hb-catalog-service

# CORS (default permite portas 4200-4203 das 4 SPAs locais)
APP_CORS_ALLOWED_ORIGINS=https://catalog.hubinity.io,https://support.hubinity.io,...

# Outbox (opcionais — defaults em application.yml)
APP_OUTBOX_DISPATCH_DELAY_MS=5000
APP_OUTBOX_DISPATCH_BATCH_SIZE=50
APP_OUTBOX_DISPATCH_MAX_ATTEMPTS=5

# Observabilidade
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317    # opcional
OTEL_SAMPLING=0.1                                    # opcional, default 0.1 (10%)

# Swagger
SWAGGER_ENABLED=false                                # default true em local, false otherwise
```

## 🚀 Como Instalar e Executar
### Pré-requisitos
- JDK 21 (Temurin recomendado)
- Maven 3.9.x
- Docker + Docker Compose (para a stack local Postgres/RabbitMQ/Keycloak via `platform-infra`)

### Passos para Instalação
```bash
# 1. Buildar os contratos compartilhados (uma vez por máquina, ou após mudanças no shared-contracts)
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

# 2. Buildar e testar este serviço
mvn -B verify

```

### Execução Local
```bash
# 1. Subir a stack base (postgres + rabbitmq + keycloak com realms já populados)
( cd ../platform-infra && make up )

# 2. Rodar via Maven (profile `local` por default)
mvn spring-boot:run

# Ou rodar o jar empacotado
mvn -DskipTests package
java -jar target/hb-catalog-service-0.1.0-SNAPSHOT.jar
```

O serviço sobe em http://localhost:8080. Health: `GET /actuator/health`. Swagger (apenas profile `local`): http://localhost:8080/swagger-ui.html.

### Execução via Docker
```bash
# Build da imagem (multi-stage: builder maven + runtime temurin-21-jre-alpine)
docker build -t hb-catalog-service:dev .

# Run standalone (precisa rede para alcançar a stack do platform-infra)
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  --network host \
  hb-catalog-service:dev

# OU subir já integrado à stack do platform-infra via profile `catalog`
( cd ../platform-infra && make up-catalog )
```

## 🛣️ Endpoints Principais

Toda rota `/api/**` exige JWT válido do realm `hibit`; operações de escrita exigem adicionalmente a role `admin` (`@PreAuthorize`). Erros seguem RFC 7807.

### Categorias
| Endpoint | Método | Auth | Descrição |
| --- | --- | --- | --- |
| `/api/v1/categories` | POST | ROLE_admin | Cria categoria. |
| `/api/v1/categories` | GET | JWT | Lista flat; `?tree=true` retorna a árvore aninhada. |
| `/api/v1/categories/{id}` | GET | JWT | Busca por id. |
| `/api/v1/categories/{id}` | PUT | ROLE_admin | Atualiza (409 slug duplicado; 422 parent inválido/ciclo). |
| `/api/v1/categories/{id}` | DELETE | ROLE_admin | Soft-delete (409 se tem filhos). |

### Produtos
| Endpoint | Método | Auth | Descrição |
| --- | --- | --- | --- |
| `/api/v1/products` | POST | ROLE_admin | Cria produto (409 SKU duplicado; 422 categoria inválida). |
| `/api/v1/products` | GET | JWT | Busca paginada — filtros `categoryId`, `q` (nome/SKU); `sort` por name\|price\|sku\|createdAt. |
| `/api/v1/products/{id}` | GET | JWT | Busca por id. |
| `/api/v1/products/{id}` | PUT | ROLE_admin | Atualiza; mudança de preço grava entrada no price history. |
| `/api/v1/products/{id}/price-history` | GET | JWT | Histórico completo de preços (mais recente primeiro). |
| `/api/v1/products/{id}` | DELETE | ROLE_admin | Soft-delete. |

### Estoque (movimentações + reservas)
Os quatro endpoints POST exigem o header **`Idempotency-Key`** (obrigatório — 400 sem ele; retry com a mesma key devolve a resposta original).

| Endpoint | Método | Auth | Descrição |
| --- | --- | --- | --- |
| `/api/v1/products/{id}/stock/movements` | POST | ROLE_admin | Movimentação IN/OUT (409 estoque insuficiente). |
| `/api/v1/products/{id}/stock/movements` | GET | JWT | Histórico de movimentações, paginado. |
| `/api/v1/stock/reservations` | POST | ROLE_admin | Cria reserva com TTL (409 estoque insuficiente). |
| `/api/v1/stock/reservations/{id}/release` | POST | ROLE_admin | Libera reserva ativa. |
| `/api/v1/stock/reservations/{id}/commit` | POST | ROLE_admin | Confirma reserva (baixa definitiva). |

### Infra / diagnóstico
| Endpoint | Auth | Descrição |
| --- | --- | --- |
| `GET /actuator/health` (+ liveness/readiness) | público | Health probes. |
| `GET /actuator/info` | público | Build info. |
| `GET /actuator/prometheus`, `/actuator/metrics/**` | ROLE_admin | Métricas. |
| `GET /swagger-ui.html`, `/v3/api-docs` | público em `local` | Swagger UI / OpenAPI JSON. |
| `GET /api/v1/_diagnostics/*` | varia ⚠️ TEMPORÁRIO | Sanity-checks do filter chain — pendente de remoção. |

## 🔄 Fluxos Principais

### Domain Model
| Entidade           | Tabela              | Soft delete | Audit                     | Notas                                                                 |
| ------------------ | ------------------- | ----------- | ------------------------- | --------------------------------------------------------------------- |
| `Category`         | `category`          | ✓           | ✓                         | Self-FK via `parentId` (sem relacionamento JPA).                      |
| `Product`          | `product`           | ✓           | ✓                         | `sku` partial UNIQUE excluindo soft-deleted.                          |
| `PriceHistory`     | `price_history`     | —           | append-only               | Uma linha por mudança de preço.                                       |
| `StockItem`        | `stock_item`        | —           | ✓                         | PK = `productId` (FK para product).                                   |
| `StockMovement`    | `stock_movement`    | —           | append-only (sem `updated_at`) | Journal de IN/OUT/RESERVE/RELEASE/COMMIT.                        |
| `StockReservation` | `stock_reservation` | —           | ✓                         | TTL via `expiresAt` + state machine ACTIVE → COMMITTED/RELEASED/EXPIRED. |
| `OutboxMessage`    | `outbox_messages`   | —           | —                         | PENDING → PUBLISHED/FAILED; housekeeping diário. Ver README-outbox.md. |
| `IdempotencyRecord`| `idempotency_key`   | —           | —                         | Claim-row por `Idempotency-Key`; retenção 24h.                        |

UUID v7 PKs gerados DB-side (ver ADR 0009). Soft-delete por `deleted_at TIMESTAMPTZ` (ver ADR 0011). Mapping via MapStruct (ver ADR 0010).

### Fluxo de escrita com evento (implementado)
1. Admin chama `POST /api/v1/products` com JWT realm role `admin`.
2. `KeycloakRealmRoleConverter` mapeia roles → Spring authorities; `@PreAuthorize("hasRole('admin')")` libera.
3. `ProductService` persiste em Postgres; `SecurityContextAuditorAware` popula `created_by` a partir do `preferred_username`.
4. Na **mesma transação**, `EventPublisher.publishProductCreated()` grava o evento em `outbox_messages` (status `PENDING`).
5. `OutboxDispatcher` (`@Scheduled`, 5s) publica o batch em `catalog.events` (topic durable) com `FOR UPDATE SKIP LOCKED`; após 5 falhas a mensagem vai para o DLQ `catalog.events.dlq`.
6. Consumers (`hb-cashier-service`, `sc-order-service` futuros) deduplicam por `messageId` (entrega at-least-once) e atualizam caches/projeções.

### Fluxo de reserva de estoque
1. `POST /api/v1/stock/reservations` (com `Idempotency-Key`) decrementa `available` e incrementa `reserved` via `UPDATE` condicional — 409 se insuficiente.
2. A reserva nasce `ACTIVE` com `expiresAt`; `release` devolve ao disponível, `commit` baixa definitivamente (ambos registram movimento no journal).
3. `ReservationExpiryService` (sweep periódico) expira reservas vencidas e devolve o estoque, usando o mesmo padrão de `UPDATE` condicional.

## 🔐 Segurança
Serviço stateless OAuth2 Resource Server. Toda request em `/api/**` exige `Authorization: Bearer <jwt>` emitido pelo realm Keycloak `hibit`. Spring Security valida assinatura, `iss` e `exp`; em seguida `KeycloakRealmRoleConverter` funde realm-scoped + client-scoped roles em authorities prefixadas com `ROLE_`, de modo que `@PreAuthorize("hasRole('admin')")` e os checks `hasRole(...)` da filter chain funcionem uniformemente. O `preferred_username` do JWT vira o nome do principal (ver ADR 0002).

### Obter um token DEV (stack local rodando)
```bash
TOKEN=$(curl -s \
  -d "client_id=hb-catalog-web" \
  -d "username=admin-hibit" \
  -d "password=admin123" \
  -d "grant_type=password" \
  http://localhost:8081/realms/hibit/protocol/openid-connect/token | jq -r .access_token)
```

### Chamar endpoint protegido
```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/products
```

## 📊 Observabilidade e Testes
- **Logs:** Logback default com pattern enriquecido por `traceId`/`spanId`: `%5p [traceId=%X{traceId:-} spanId=%X{spanId:-}]` (em `application.yml`).
- **Tracing:** Micrometer Tracing bridge + OTLP exporter. Sampling default `0.1` (10%), configurável via `OTEL_SAMPLING`. Endpoint default `http://localhost:4317` (OTLP gRPC), override via `OTEL_EXPORTER_OTLP_ENDPOINT`.
- **Métricas:** Micrometer → Prometheus via Actuator (`/actuator/prometheus`, restrito a `ROLE_admin`).
- **Health probes:** `/actuator/health/liveness` e `/actuator/health/readiness` habilitadas (Kubernetes-friendly).
- **Como Rodar os Testes:**
  - Unit / WebMvc slice / config: `mvn test` (Surefire exclui automaticamente testes `@Tag("integration")`). Cobre services (Mockito), controllers (`@WebMvcTest` + `spring-security-test`) e config.
  - Integration (Testcontainers — exige Docker): `mvn -P integration-tests verify` (Failsafe roda apenas `**/*IT.java` com tag `integration`). Inclui persistência, migrations Flyway, filtro de idempotência e outbox (`OutboxDispatcherIT`, `OutboxBrokerFailureIT`).
  - Classe específica: `mvn -Dtest=StockServiceTest test` ou `mvn -P integration-tests -Dtest=StockPersistenceIT verify`.
  - Coverage: JaCoCo gera relatório em `target/site/jacoco/` na phase `verify`. PRD seta meta de 80%; sem `<check>` enforce hoje.

---

## 📚 ADRs (Decisões de Arquitetura)
Consulte `docs/adr/` para o histórico:
- **0002** — JWT role mapping (realm + resource roles, política do prefixo `ROLE_`).
- **0003** — CORS allowlist (knob `APP_CORS_ALLOWED_ORIGINS`).
- **0009** — UUID v7 gerado DB-side (função `uuidv7()`).
- **0010** — MapStruct (vs manual mappers).
- **0011** — Soft delete via `deleted_at TIMESTAMPTZ`.

Ver também `docs/ANALISE_ARQUITETURAL_2026-07.md` (relatório de análise) e as specs das features em `../specs/` (001 categorias, 002 produtos, 003 estoque).

## 🗺️ Roadmap (Phase 1)
Entregue até aqui: CRUD de categorias e produtos (com price history), movimentações e reservas de estoque com idempotência, Transactional Outbox publicando em `catalog.events`. Pendente: remoção do package `_diagnostics`, testes end-to-end adicionais, hardening de produção e consumers downstream (`hb-catalog-web`, cache invalidation do totem). Ver o board do feature para o backlog ordenado.

## 📄 Licença
MIT — ver [`LICENSE`](./LICENSE).
