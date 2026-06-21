# ADR 0003 — CORS allowlist via env var

- **Status**: Accepted
- **Date**: 2026-06-19
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
The catalog service is consumed by multiple Hibit frontends (`hb-catalog-web`,
`hb-cashier-web`, `hb-support-web`, `sc-totem-web`), each running on its own
origin. Browser preflight (`OPTIONS`) must succeed for credentialed requests
with `Authorization: Bearer <jwt>`. Spring Security blocks any origin that is
not explicitly allowlisted, so the catalog has to declare the right list per
environment without code changes.

## Decision Drivers
- Local development friction-free for the four current Angular dev servers.
- Production allowlist driven by ops, not source code.
- Compatibility with credentialed CORS (`Access-Control-Allow-Credentials:
  true`) — which forbids wildcard origins.
- Keep actuator and management routes untouched by CORS — they are server-to-
  server traffic.

## Considered Options
- **Wildcard `*`** — pros: zero config; cons: incompatible with credentialed
  preflight (Spring/CORS spec forbids `Allow-Origin: *` together with
  `Allow-Credentials: true`). Rejected.
- **Per-route override (controller-level `@CrossOrigin`)** — pros: explicit at
  the endpoint; cons: every new endpoint becomes a place to forget the
  annotation. Rejected — too easy to drift.
- **Gateway-only CORS** — pros: single source of truth; cons: API gateway
  lands in Phase 5, and until then each service is exposed directly to
  browsers. Rejected for now (revisit when the gateway lands).
- **Service-level allowlist via env var** — accepted.

## Decision Outcome
**Chosen**: `application.yml` declares
`app.cors.allowed-origins` with four `localhost` defaults
(`:4200`–`:4203`) for the in-repo Angular apps. The
`CorsConfigurationSource` bean in `SecurityConfig` reads this comma-separated
list, trims whitespace, and registers the configuration only for `/api/**`.
Staging and prod override the same property via the
`APP_CORS_ALLOWED_ORIGINS` environment variable. Credentials are allowed;
all standard verbs and headers are accepted.

## Consequences
- ✅ Cloud ops can change the allowlist without redeploying — just set the
  env var.
- ✅ Actuator endpoints stay off the CORS surface (no risk of leaking
  cross-origin scrape traffic semantics).
- ⚠️ Every new frontend requires a new entry in
  `APP_CORS_ALLOWED_ORIGINS`; this should be added to the cloud-setup
  checklist as a follow-up note (tracked separately).
- ⚠️ When the API gateway ships in Phase 5, the gateway becomes the
  authoritative CORS boundary and the per-service config degrades to a
  defense-in-depth fallback; this ADR will be revisited (likely superseded
  in part) at that point.
