# ADR 0002 — Keycloak realm + resource roles as Spring authorities

- **Status**: Accepted
- **Date**: 2026-06-19
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
HiBit users authenticate against the shared Keycloak realm `hibit`. Their roles
arrive in the access token under two distinct JWT claims: `realm_access.roles`
(realm-wide assignments such as `admin`, `tecnico`, `atendente`) and
`resource_access.<client-id>.roles` (per-service client mappings). The catalog
service must turn both into Spring Security `GrantedAuthority` instances so
that `@PreAuthorize("hasRole('admin')")` works uniformly regardless of where
the role was granted.

## Decision Drivers
- One authorization model that does not care whether a role is realm-scoped or
  client-scoped — both are first-class.
- Configurable client id so the same converter ships unchanged to every
  Hubinity microservice.
- Deterministic, idempotent mapping: a token issued twice yields the same
  authority set; duplicates between realm and resource scopes collapse.
- Compatibility with Spring's `hasRole(...)` expression idiom, which requires
  the `ROLE_` prefix.

## Considered Options
- **Realm-only mapping** — pros: simplest reader; cons: every per-service role
  (e.g. `catalog-writer`) would have to be promoted to realm scope, polluting
  the global namespace. Rejected — too coarse.
- **Client-only mapping** — pros: tight per-service isolation; cons: the
  `admin` role lives at realm scope across HiBit, so we would have to mirror
  it into every service's client mapper. Rejected — duplication and drift.
- **JWT scopes (`scope` claim)** — pros: standard OAuth2 idiom; cons: OAuth2
  scopes describe *what the token is allowed to ask for*, not *who the user
  is* — semantics drift and existing Hibit role names do not map cleanly.
  Rejected.
- **Read realm + resource roles, merge, prefix `ROLE_`, dedupe** — accepted.

## Decision Outcome
**Chosen**: `KeycloakRealmRoleConverter` reads both `realm_access.roles` and
`resource_access.<client>.roles`, merges them into an ordered set, prefixes
each with `ROLE_`, and returns them as `SimpleGrantedAuthority`. The client
id is injected from the `app.security.keycloak.client-id` property (defaulting
to `hb-catalog-service`). The JWT `principal-claim-name` is set to
`preferred_username` so controllers see a human-readable username instead of
the opaque `sub` UUID.

## Consequences
- ✅ A single role-mapping policy applies across realm and client scopes; no
  duplication required when a role is meaningful in both.
- ✅ `app.security.keycloak.client-id` is the only knob each service tweaks;
  the converter implementation is shared.
- ⚠️ Role names must not collide across realm and resource scope by accident;
  dedupe makes the collision harmless but operators should still namespace
  per-client roles (e.g. `catalog-writer`, not `writer`).
- ⚠️ A Testcontainers Keycloak integration test (real token round-trip) is
  deferred to the Phase-1 hardening pass — current coverage is the pure
  converter unit test plus the `DiagnosticsController` security slice test.
