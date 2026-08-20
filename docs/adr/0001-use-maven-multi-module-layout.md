# ADR 0001 — Use Maven multi-module layout

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
Hubinity has at least six backend services (catalog, order, support, cashier,
totem, iam-aware gateways) and each one needs only a slice of the cross-service
contracts. We must publish the contracts in a way that lets each consumer pull
exactly what it depends on without dragging unrelated domains.

## Decision Drivers
- Minimize transitive footprint per service (clean dep graph).
- Keep all contract YAMLs/JSON Schemas in a single repo so cross-domain edits
  are atomic and reviewable in one PR.
- Allow per-domain versioning later without splitting the repo.

## Considered Options
- **Single fat artifact** — pros: one coordinate to depend on; cons: every
  service compiles every DTO; an unrelated change in `support.yaml` triggers a
  rebuild for `sc-totem-web`. Rejected.
- **Repo-per-contract** — pros: total isolation; cons: six+ repos to keep in
  lockstep, cross-reference drift, no shared parent POM. Rejected.
- **Maven multi-module under one parent POM** — pros: shared plugin
  management (versions of `openapi-generator`, `jsonschema2pojo`, Jackson BOM
  live once in the parent), per-module artifacts (`contracts-catalog`,
  `contracts-order`, `contracts-support`, `contracts-cashier`,
  `contracts-events`); cons: more `pom.xml` files. Accepted.

## Decision Outcome
**Chosen**: Maven multi-module. The parent POM (`com.hubinity:platform-shared-contracts:0.1.0-SNAPSHOT`,
`packaging=pom`) declares the five sub-modules and centralizes plugin/dependency
versions. Each service depends only on the `contracts-<domain>` artifact(s) it
needs.

## Consequences
- ✅ Each service has a minimal contract footprint; rebuild graph is sharp.
- ✅ Plugin and BOM versions are pinned in one place (parent
  `<pluginManagement>` / `<dependencyManagement>`).
- ⚠️ Six `pom.xml` files to maintain (1 parent + 5 children); offset by the
  fact that children inherit nearly everything.
