# hb-catalog-service (Backend) - Ecossistema Hubinity - In Progress
> Parte integrante do ecossistema distribuído Hubinity.

---

## 💻 Visão Geral
- **O que faz:** Microsserviço **source-of-truth do catálogo HiBit** — gerencia produtos, categorias, estoque (disponível/reservado/reorder point), movimentações de estoque (journal append-only) e reservas com TTL. Expõe REST API alinhada ao contrato `contracts-catalog` e publicará eventos de mudança de produto/preço/estoque na exchange RabbitMQ `catalog.events` (publisher planejado para Phase 1.8).
- **Problema que resolve:** Concentra em um único serviço a verdade sobre "o que existe à venda" no ecossistema HiBit, evitando estoque divergente entre cashier/support/order. Atualmente, contracts-events permitem que `sc-order-service` e `hb-cashier-service` se mantenham eventualmente consistentes sem chamadas síncronas no caminho crítico.
- **Posicionamento no Ecossistema:** Backend Spring Boot rodando no realm Keycloak `hibit`. **Phase 1 em construção** (Phase 1.4 atual — segurança e diagnostics no ar; entidades e controllers principais aterrissam nas features 1.5 – 1.8). O esqueleto atual já passa em `mvn verify` (31/31 testes) e `docker build`.

## 🏗️ Papel na Arquitetura
- **Tipo de Componente:** Microsserviço REST + (futuro) publisher de eventos AMQP.
- **Responsabilidades Principais:**
  - Persistir e expor entidades do domínio catálogo (`Category`, `Product`, `StockItem`, `StockMovement`, `StockReservation`).
  - Validar JWTs emitidos pelo realm `hibit` (OAuth2 Resource Server) e aplicar autorização via roles realm-scoped.
  - (Planejado) publicar eventos de domínio em `catalog.events` quando o estado mudar.
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
- **RabbitMQ** — CloudAMQP em cloud, container `rabbitmq:3.13-management-alpine` em dev local. Este serviço será **publisher** na exchange `catalog.events`.
- **Keycloak** — Railway Hobby em cloud, container `quay.io/keycloak/keycloak:26.0` em dev local.

## 🛠️ Tecnologias e Ferramentas
| Camada | Tecnologia | Versão |
| :--- | :--- | :--- |
| Linguagem | Java | 21 (LTS, Temurin) |
| Framework | Spring Boot (parent) | 4.1.0 |
| Web / Validation | spring-boot-starter-web + starter-validation | (gerenciado pelo parent) |
| Persistence | spring-boot-starter-data-jpa + Hibernate | (gerenciado pelo parent) |
| Migrations | Flyway core + flyway-database-postgresql | (gerenciado pelo parent) |
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
- **Estilo Arquitetural:** **Layered DDD-lite** — `domain` (entidades + repositórios JPA), `api` (controllers + DTO + MapStruct mappers), `config` (security, auditing), `integration` (futuro: AMQP publishers).
- **Padrões Relevantes:**
  - **Contract-first** — DTOs vêm do `platform-shared-contracts`; controllers são manualmente alinhados aos endpoints OpenAPI.
  - **Soft delete via `deleted_at TIMESTAMPTZ`** (ver ADR 0011) — aplicado em `category` e `product`; journals (`stock_movement`) não.
  - **UUID v7 PKs gerados DB-side** (ver ADR 0009) — função `uuidv7()` provisionada pelo `V1__init.sql`.
  - **MapStruct para DTO mapping** (ver ADR 0010) — `defaultComponentModel=spring`, `unmappedTargetPolicy=IGNORE`.
  - **`SecurityContextAuditorAware`** popula `created_by`/`updated_by` automaticamente a partir do `preferred_username` do JWT.
  - **Role mapping JWT → Spring authorities** via `KeycloakRealmRoleConverter` (ver ADR 0002) — funde roles realm-scoped e client-scoped sob prefixo `ROLE_`.

## 📂 Estrutura do Projeto
```text
hb-catalog-service/
├── README.md
├── pom.xml                                       # parent: spring-boot 4.1.0
├── Dockerfile                                    # multi-stage: maven builder + temurin-21-jre-alpine runtime
├── BRANCHES.md / CONTRIBUTING.md / LICENSE
├── docs/
│   └── adr/                                      # 5 ADRs (0002, 0003, 0009, 0010, 0011)
└── src/
    ├── main/
    │   ├── java/com/hubinity/catalog/
    │   │   ├── HbCatalogServiceApplication.java
    │   │   ├── api/
    │   │   │   ├── _diagnostics/                 # ⚠️ TEMPORÁRIO — removido em Phase 1.5+
    │   │   │   ├── dto/                          # CategoryRequest/Response, ProductRequest/Response, StockItem/Movement/Reservation
    │   │   │   └── mapper/                       # MapStruct (CategoryMapper, ProductMapper, StockItem/Movement/ReservationMapper)
    │   │   ├── config/                           # SecurityConfig, KeycloakRealmRoleConverter, JpaAuditingConfig, SecurityContextAuditorAware
    │   │   ├── domain/                           # Category, Product, StockItem, StockMovement, StockReservation + Repositórios JPA + enums
    │   │   └── integration/                      # (reservado para AMQP publishers — Phase 1.8)
    │   └── resources/
    │       ├── application.yml                   # config base
    │       ├── application-local.yml             # defaults hardcoded localhost
    │       ├── application-staging.yml           # tudo via env
    │       ├── application-prod.yml              # tudo via env, INFO root, Swagger OFF
    │       └── db/migration/V1__init.sql         # uuidv7() + set_updated_at() + 5 tabelas
    └── test/
        └── java/com/hubinity/catalog/            # 10 classes de teste (config, db, domain, api/_diagnostics)
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

### Atualmente expostos (Phase 1.4)
| Endpoint                                | Profile         | Auth                                | Descrição                                         |
| --------------------------------------- | --------------- | ----------------------------------- | ------------------------------------------------- |
| `GET /actuator/health`                  | all             | público                             | Aggregate health + liveness/readiness probes.     |
| `GET /actuator/info`                    | all             | público                             | Build info.                                       |
| `GET /actuator/prometheus`              | all             | ROLE_admin                          | Métricas Prometheus.                              |
| `GET /actuator/metrics/**`              | all             | ROLE_admin                          | Registry Micrometer.                              |
| `GET /swagger-ui.html` / `/v3/api-docs` | `local` apenas  | público (em local)                  | Swagger UI / OpenAPI JSON.                        |
| `GET /api/v1/_diagnostics/public`       | all             | público ⚠️ TEMPORÁRIO              | Sanity-check do filter chain.                     |
| `GET /api/v1/_diagnostics/me`           | all             | JWT válido qualquer ⚠️ TEMPORÁRIO  | Echoes principal + authorities.                   |
| `GET /api/v1/_diagnostics/admin-only`   | all             | ROLE_admin ⚠️ TEMPORÁRIO            | Verifica `@PreAuthorize` + role mapping.          |

> ⚠️ O package `_diagnostics/*` é descartável — será removido em features 1.5+ quando os endpoints reais aterrissarem. O underscore prefix permite remoção via `rm -rf` de um único package.

### Próximos (planejados para Phase 1.5 – 1.7)
- `GET|POST|PUT|DELETE /api/v1/categories`
- `GET|POST|PUT|DELETE /api/v1/products`
- `GET|POST /api/v1/stock/*` (consultas + movimentações + reservas)

## 🔄 Fluxos Principais

### Domain Model
| Entidade           | Tabela              | Soft delete | Audit                     | Notas                                                                 |
| ------------------ | ------------------- | ----------- | ------------------------- | --------------------------------------------------------------------- |
| `Category`         | `category`          | ✓           | ✓                         | Self-FK via `parentId` (sem relacionamento JPA).                      |
| `Product`          | `product`           | ✓           | ✓                         | `sku` partial UNIQUE excluindo soft-deleted.                          |
| `StockItem`        | `stock_item`        | —           | ✓                         | PK = `productId` (FK para product).                                   |
| `StockMovement`    | `stock_movement`    | —           | append-only (sem `updated_at`) | Journal de IN/OUT/RESERVE/RELEASE/COMMIT.                        |
| `StockReservation` | `stock_reservation` | —           | ✓                         | TTL via `expiresAt` + state machine ACTIVE → COMMITTED/RELEASED/EXPIRED. |

UUID v7 PKs gerados DB-side (ver ADR 0009). Soft-delete por `deleted_at TIMESTAMPTZ` (ver ADR 0011). Mapping via MapStruct (ver ADR 0010).

### Fluxo async planejado (Phase 1.8 — ainda não em produção)
1. Admin chama `POST /api/v1/products` com JWT realm role `admin`.
2. `KeycloakRealmRoleConverter` mapeia roles → Spring authorities; `@PreAuthorize("hasRole('admin')")` libera.
3. Service persiste em Postgres; `SecurityContextAuditorAware` popula `created_by` a partir do `preferred_username`.
4. Publisher AMQP emite `ProductCreated` em `catalog.events` (exchange topic durable).
5. Consumers (`hb-cashier-service`, `sc-order-service` futuros) atualizam caches/projeções via fila + DLX.

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
  http://localhost:8080/api/v1/_diagnostics/me
```

## 📊 Observabilidade e Testes
- **Logs:** Logback default com pattern enriquecido por `traceId`/`spanId`: `%5p [traceId=%X{traceId:-} spanId=%X{spanId:-}]` (em `application.yml`).
- **Tracing:** Micrometer Tracing bridge + OTLP exporter. Sampling default `0.1` (10%), configurável via `OTEL_SAMPLING`. Endpoint default `http://localhost:4317` (OTLP gRPC), override via `OTEL_EXPORTER_OTLP_ENDPOINT`.
- **Métricas:** Micrometer → Prometheus via Actuator (`/actuator/prometheus`, restrito a `ROLE_admin`).
- **Health probes:** `/actuator/health/liveness` e `/actuator/health/readiness` habilitadas (Kubernetes-friendly).
- **Como Rodar os Testes:**
  - Unit / WebMvc slice / config: `mvn test` → **31/31 PASS** (Surefire exclui automaticamente testes `@Tag("integration")`).
  - Integration (Testcontainers — exige Docker): `mvn -P integration-tests verify` (Failsafe roda apenas `**/*IT.java` com tag `integration`).
  - Coverage: JaCoCo gera relatório em `target/site/jacoco/` na phase `verify`. PRD seta meta de 80%; sem `<check>` enforce hoje (bootstrap module).

---

## 📚 ADRs (Decisões de Arquitetura)
Consulte `docs/adr/` para o histórico:
- **0002** — JWT role mapping (realm + resource roles, política do prefixo `ROLE_`).
- **0003** — CORS allowlist (knob `APP_CORS_ALLOWED_ORIGINS`).
- **0009** — UUID v7 gerado DB-side (função `uuidv7()`).
- **0010** — MapStruct (vs manual mappers).
- **0011** — Soft delete via `deleted_at TIMESTAMPTZ`.

## 🗺️ Roadmap (Phase 1)
Features 1.5 – 1.15 pendentes: entidades JPA mapeadas para os endpoints reais, controllers de catálogo/estoque, RabbitMQ publisher, testes de integração end-to-end, hardening de produção. Ver o board do feature para o backlog ordenado.

## 📄 Licença
MIT — ver [`LICENSE`](./LICENSE).
