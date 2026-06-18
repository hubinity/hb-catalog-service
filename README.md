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

## License

MIT — see [`LICENSE`](./LICENSE).
