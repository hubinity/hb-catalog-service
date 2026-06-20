# hb-catalog-service

Hubinity's source-of-truth microservice for **HiBit** products, categories and
stock. Exposes a REST API (contract: `contracts-catalog`) and publishes
domain events to the `catalog.events` RabbitMQ exchange (contract:
`contracts-events`).

**Status:** Phase 1 — Catálogo (bootstrap). No business code yet; the empty
skeleton in this commit lets `mvn package` and `docker build` succeed.
Entities, controllers and publishers land in features 1.3 – 1.8.

## Stack

| Layer            | Technology                                      |
|------------------|-------------------------------------------------|
| Language         | Java 21 (LTS, Temurin)                          |
| Framework        | Spring Boot 4.1.0                               |
| Persistence      | Spring Data JPA + Hibernate, Flyway migrations  |
| Database         | PostgreSQL 18 (cloud Supabase) / 16 (local)     |
| Messaging        | RabbitMQ 3.13 (exchange `catalog.events`)       |
| AuthN/AuthZ      | Keycloak (realm `hibit`) — Spring Security OAuth2 Resource Server |
| Cache            | Caffeine (in-process), via Spring Cache         |
| Resilience       | Resilience4j (Spring Boot 4 starter)            |
| Mapping          | MapStruct 1.6.x                                 |
| Observability    | Actuator + Micrometer + OpenTelemetry (OTLP)    |
| API docs         | springdoc-openapi 3.x (Swagger UI in dev)       |
| Build            | Maven 3.9                                       |
| Container        | `eclipse-temurin:21-jre-alpine` runtime         |

See PRD section 4.1 and `docs/adr/` for the full rationale.

## Local development

### Prerequisites

- JDK 21 (Temurin recommended)
- Maven 3.9.x
- Docker + Docker Compose (for the local Postgres / RabbitMQ / Keycloak stack)

### Quickstart

```bash
# 1. Build the shared contracts once (publishes JARs to your local ~/.m2)
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

# 2. Bring up the local stack (Postgres + RabbitMQ + Keycloak)
( cd ../platform-infra && docker compose up -d postgres rabbitmq keycloak )

# 3. Build + test this service
mvn -B verify

# 4. Run locally
mvn spring-boot:run
# or via Docker
docker build -t hb-catalog-service:dev .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=local \
  --network host \
  hb-catalog-service:dev
```

The platform-wide stack lives in
[`../platform-infra/docker-compose.yml`](../platform-infra/docker-compose.yml).
Bringing the catalog service up as a compose service requires the opt-in
`catalog` profile: `docker compose --profile catalog up`.

## Profiles

| Profile   | When                          | Notes                                                    |
|-----------|-------------------------------|----------------------------------------------------------|
| `local`   | `mvn spring-boot:run`         | Hard-coded `localhost` defaults for DB/MQ/Keycloak.      |
| `test`    | `mvn test`                    | Excludes DataSource/JPA/Flyway/Rabbit auto-config — fully offline. |
| `staging` | Railway / GitHub Actions      | All sensitive values via env vars.                       |
| `prod`    | Production deploy             | `INFO` root, `WARN` Spring, Swagger UI off.              |

Default profile: `local` (override with `SPRING_PROFILES_ACTIVE`).

### Schema

Flyway provisions the catalog schema on startup. The initial migration
([`V1__init.sql`](./src/main/resources/db/migration/V1__init.sql)) creates a
`uuidv7()` PK generator, a `set_updated_at()` audit trigger and five tables:

| Table               | Purpose                                                              |
|---------------------|----------------------------------------------------------------------|
| `category`          | Product categories (logical tree via `parent_id`).                   |
| `product`           | Sellable items, priced and FK'd to `category`.                       |
| `stock_item`        | 1:1 with product — current `available` / `reserved` / `reorder_point` counts. |
| `stock_movement`    | Append-only journal of stock changes (IN/OUT/RESERVE/RELEASE/COMMIT). |
| `stock_reservation` | Short-lived holds against available stock (ACTIVE → COMMITTED/RELEASED/EXPIRED). |

Soft-delete follows the [`deleted_at TIMESTAMPTZ`](./docs/adr/0011-soft-delete-deleted-at.md)
convention (ADR 0011): `category` and `product` carry the column; the journal
and stock tables do not.

The Flyway migration is integration-tested against a Testcontainers Postgres 16
instance in `FlywayMigrationIT`. Default `mvn test` skips it (tag-based
exclusion); run it with `mvn -P integration-tests verify` when Docker is
available.

### Domain model

| Entity | Table | Soft delete | Audit | Notes |
|---|---|---|---|---|
| `Category` | `category` | ✓ | ✓ | Self-FK via `parentId` (no JPA relationship) |
| `Product` | `product` | ✓ | ✓ | `sku` partial UNIQUE excludes soft-deleted |
| `StockItem` | `stock_item` | — | ✓ | PK = `productId` (FK to product) |
| `StockMovement` | `stock_movement` | — | append-only (no `updated_at`) | Journal of stock changes |
| `StockReservation` | `stock_reservation` | — | ✓ | TTL via `expiresAt` + status state machine |

UUID v7 PKs are generated DB-side (see [ADR 0009](docs/adr/0009-uuid-v7-db-default.md)).
Soft delete uses `deleted_at TIMESTAMPTZ` (see [ADR 0011](docs/adr/0011-soft-delete-deleted-at.md)).
Mapping uses MapStruct (see [ADR 0010](docs/adr/0010-mapstruct-vs-manual-mappers.md)).

## Environment variables (staging / prod)

| Variable                       | Required | Description                                       |
|--------------------------------|----------|---------------------------------------------------|
| `SPRING_PROFILES_ACTIVE`       | yes      | `staging` or `prod`                               |
| `HB_CATALOG_DB_URL`            | yes      | JDBC URL, e.g. `jdbc:postgresql://host:5432/db`   |
| `HB_CATALOG_DB_USERNAME`       | yes      | DB user                                           |
| `HB_CATALOG_DB_PASSWORD`       | yes      | DB password                                       |
| `CLOUDAMQP_URL`                | yes      | RabbitMQ AMQP URI (CloudAMQP-style)               |
| `KEYCLOAK_ISSUER_URI`          | yes      | e.g. `https://iam.hubinity.io/realms/hibit`       |
| `KEYCLOAK_JWK_URI`             | yes      | JWKS endpoint                                     |
| `OTEL_EXPORTER_OTLP_ENDPOINT`  | no       | Defaults to `http://localhost:4317`               |
| `OTEL_SAMPLING`                | no       | Defaults to `0.1` (10% trace sampling)            |
| `SERVER_PORT`                  | no       | Defaults to `8080`                                |
| `SWAGGER_ENABLED`              | no       | Defaults to `true` in `local`, `false` otherwise  |

## Endpoints

| Endpoint                       | Profile          | Description                       |
|--------------------------------|------------------|-----------------------------------|
| `GET /actuator/health`         | all              | Aggregate health (liveness + readiness probes via `/health/liveness`, `/health/readiness`) |
| `GET /actuator/info`           | all              | Build info                        |
| `GET /actuator/prometheus`     | all              | Metrics scrape endpoint           |
| `GET /actuator/metrics`        | all              | Metric registry                   |
| `GET /swagger-ui.html`         | `local` only     | API documentation UI              |

## Documentation

- ADRs: [`docs/adr/`](./docs/adr/)
- PRD: see `PRD-HUBINITY.md` at the workspace root, section 4.1 (HB Catalog).
- Roadmap: features 1.2 – 1.15 are pending — see the feature board for the
  ordered backlog (security config, entities, controllers, RabbitMQ
  publisher, integration tests, etc.).

## Security

The catalog service is a stateless OAuth2 Resource Server. Every request to
`/api/**` must carry an `Authorization: Bearer <jwt>` issued by the Keycloak
realm `hibit`. Spring Security validates the token (signature, `iss`, `exp`),
then `KeycloakRealmRoleConverter` merges the realm- and client-scoped roles
from the JWT into `ROLE_`-prefixed Spring authorities so
`@PreAuthorize("hasRole('admin')")` and the filter-chain `hasRole(...)` checks
work uniformly. The JWT's `preferred_username` claim is used as the principal
name (see ADR 0002).

### Fetch a local dev token

```bash
TOKEN=$(curl -s \
  -d "client_id=hb-catalog-web" \
  -d "username=admin-hibit" \
  -d "password=admin123" \
  -d "grant_type=password" \
  http://localhost:8081/realms/hibit/protocol/openid-connect/token | jq -r .access_token)
```

### Call a protected endpoint

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/_diagnostics/me
```

### Diagnostics endpoints

| Endpoint                              | Auth required                  | Purpose                                             |
|---------------------------------------|--------------------------------|-----------------------------------------------------|
| `GET /api/v1/_diagnostics/public`     | none                           | Sanity-check the filter chain reaches the controller. |
| `GET /api/v1/_diagnostics/me`         | any valid JWT                  | Echoes principal name + resolved authorities.       |
| `GET /api/v1/_diagnostics/admin-only` | JWT with realm role `admin`    | Verifies `@PreAuthorize` + role mapping end-to-end. |

The `_diagnostics/*` package is a throwaway — it will be removed when the real
catalog endpoints land in features 1.5+. The leading underscore in the folder
name makes the removal a single `rm -rf` of the package.

### Related ADRs

- [`docs/adr/0002-jwt-role-mapping.md`](./docs/adr/0002-jwt-role-mapping.md)
  — why realm + resource roles are both mapped, and how the `ROLE_` prefix
  policy is applied.
- [`docs/adr/0003-cors-allowlist.md`](./docs/adr/0003-cors-allowlist.md)
  — CORS allowlist policy and the `APP_CORS_ALLOWED_ORIGINS` env knob.

## License

MIT — see [`LICENSE`](./LICENSE).
