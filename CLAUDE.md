# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this service is

`hb-catalog-service` is the source-of-truth microservice for the HiBit catalog (products, categories, stock, stock movements, reservations) in the Hubinity ecosystem. It is one of 12 sibling repos; the umbrella repo one directory up (`../CLAUDE.md`, `../PRD-HUBINITY.md`) holds cross-repo context, and feature specs live in `../specs/` (e.g. `../specs/003-stock-movement-reservation/plan.md`).

**Stack**: Java 21 · Spring Boot 4.1.0 · Maven · PostgreSQL · RabbitMQ · Flyway · MapStruct · Keycloak OAuth2 Resource Server.

## Commands

```bash
# Prerequisite (once per machine, or after shared-contracts changes):
( cd ../platform-shared-contracts && mvn -B -DskipTests install )

# Build + unit tests (no Docker needed; Surefire excludes @Tag("integration"))
mvn -B verify

# Single unit test class
mvn -Dtest=StockServiceTest test

# Integration tests (Testcontainers — requires running Docker daemon;
# Failsafe runs only **/*IT.java tagged "integration")
mvn -P integration-tests verify

# Single integration test class
mvn -P integration-tests -Dtest=StockPersistenceIT verify

# Run locally (profile `local` by default; needs the platform-infra stack:
# `cd ../platform-infra && make up` for postgres + rabbitmq + keycloak)
mvn spring-boot:run
```

Get a dev JWT (local stack running):

```bash
TOKEN=$(curl -s -d "client_id=hb-catalog-web" -d "username=admin-hibit" \
  -d "password=admin123" -d "grant_type=password" \
  http://localhost:8081/realms/hibit/protocol/openid-connect/token | jq -r .access_token)
```

Swagger UI (local only): `http://localhost:8080/swagger-ui.html`. RabbitMQ UI: `http://localhost:15672` (`hubinity`/`hubinity_local`).

## Architecture

Layered DDD-lite under `com.hubinity.catalog`:

- `api/` — controllers (`CategoryController`, `ProductController`, `StockController`), DTO records, MapStruct mappers, `error/` (typed exceptions + `ApiExceptionHandler` → RFC 7807 ProblemDetail), `idempotency/` (filter + service)
- `service/` — all business logic, one class per aggregate (`CategoryService`, `ProductService`, `StockService`, `ReservationExpiryService`)
- `domain/` — JPA entities + Spring Data repositories, **no business logic**
- `events/published/` — event payload records; `integration/` — outbox dispatcher + AMQP publishing
- `config/` — security, JPA auditing, Rabbit topology, filter registration

### Transactional Outbox (see README-outbox.md before touching events)

Services never publish to RabbitMQ directly. Inside the business `@Transactional` method they call `EventPublisher.publish*()`, which INSERTs into `outbox_messages` in the same TX. `OutboxDispatcher` (`@Scheduled`, 5s fixedDelay) drains PENDING rows with `SELECT … FOR UPDATE SKIP LOCKED`, publishes to the `catalog.events` topic exchange, and after 5 failed attempts routes to the `catalog.events.dlx`/DLQ. Delivery is **at-least-once** — consumers dedupe by `messageId`. To add an event: record in `events/published/` → entry in `CatalogEvent` enum → method on `EventPublisher` + `DefaultEventPublisher` → call from the service inside the business TX (full recipe in README-outbox.md).

### Stock concurrency (critical invariant)

Every stock counter mutation is a single conditional `UPDATE … WHERE available >= :qty` on `StockItemRepository` / `StockReservationRepository` — the row lock Postgres takes for the statement is the *only* concurrency primitive. Do **not** introduce `SELECT … FOR UPDATE` (or reuse `findByIdForUpdate`) for new concurrency checks. Reservations follow a state machine ACTIVE → COMMITTED/RELEASED/EXPIRED with a periodic expiry sweep.

### Idempotency

`Idempotency-Key` header is mandatory on the four mutating stock POST endpoints (movements, reserve, release, commit). `IdempotencyFilter` runs **before** `DispatcherServlet` using a claim-row pattern against the durable `idempotency_key` table — its error responses are hand-built ProblemDetail JSON that never pass through `ApiExceptionHandler`. It is registered via `IdempotencyFilterConfig` (`FilterRegistrationBean`), not `@Component`; path narrowing happens in `shouldNotFilter` with `AntPathMatcher`.

### Security

Stateless OAuth2 Resource Server validating JWTs from Keycloak realm `hibit`. `KeycloakRealmRoleConverter` merges realm-scoped + client-scoped roles into `ROLE_`-prefixed authorities; mutating endpoints gate on `@PreAuthorize("hasRole('admin')")`. Principal name = `preferred_username`, which `SecurityContextAuditorAware` uses to populate `created_by`/`updated_by`.

### Persistence conventions

- **Flyway only** (`ddl-auto: validate`): schema changes go in `src/main/resources/db/migration/V<n>__*.sql`. Never switch to `create`/`update`.
- **UUID v7 PKs generated DB-side** via the `uuidv7()` function from `V1__init.sql` (ADR 0009).
- **Soft delete** via `deleted_at TIMESTAMPTZ` on `category`/`product` only (ADR 0011); `stock_movement` is an append-only journal (no `updated_at`).
- MapStruct configured with `defaultComponentModel=spring`, `unmappedTargetPolicy=IGNORE` (ADR 0010).
- ADRs live in `docs/adr/`.

### Spring profiles

`local` (default; hardcoded localhost defaults), `test` (used by unit tests — excludes DataSource/JPA/Flyway/Rabbit auto-config, fully offline), `staging`/`prod` (all credentials from env vars — never put secrets in YAML). Env var reference is in README.md.

## Testing structure

Mirrors main packages: `service/*Test.java` (Mockito unit), `api/*Test.java` (`@WebMvcTest` slices + `spring-security-test`; Boot 4 requires the separate `spring-boot-starter-webmvc-test` starter), `domain/*IT.java` + `db/*IT.java` (`@Tag("integration")` Testcontainers). Outbox behavior is covered by `OutboxDispatcherIT` (happy path, rollback, concurrent dispatchers) and `OutboxBrokerFailureIT` (broker down → FAILED → DLQ).

## Commit convention

Conventional-commit style prefixes: `feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `style:`. Work on `feature/<name>` or `fix/<name>` branches, never directly on `main`.

<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read the current plan
at specs/005-stock-read-authz/plan.md
<!-- SPECKIT END -->
