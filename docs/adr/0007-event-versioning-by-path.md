# ADR 0007 — Event Versioning by Path

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
Domain events are published cross-service and consumed by independently
released services. Consumers must evolve on their own cadence. The original
brief asked for "OpenAPI-style versioned schemas", but ADR 0004 already
chose JSON Schema 2020-12 over OpenAPI/AsyncAPI for event payloads. We need
a versioning policy that fits JSON Schema and supports N/N-1 coexistence
(ADR 0005).

## Decision Drivers
- Clean side-by-side coexistence of `v1` and `v2` in generated Java code.
- Explicit physical location per version (no ambiguity about which file
  describes which wire format).
- Minimal tooling — no extra infrastructure for an MVP-scale platform.

## Considered Options
- **OpenAPI YAML per event** — rejected: contradicts ADR 0004 and brings
  back the tooling we explicitly walked away from.
- **AsyncAPI catalog** — rejected: adds a spec layer and tooling we do not
  need at current volume.
- **Schema registry (e.g. Confluent / Apicurio)** — rejected: infra
  overhead disproportionate to the current event count.
- **`schemaVersion` field only, no path namespace** — rejected: the
  physical location of "the v1 schema" becomes ambiguous over time; diffs
  become harder to read.
- **Chosen**: major version in path (`events/vN/`), `schemaVersion` field
  inside each payload for minor/patch.

## Decision Outcome
**Chosen**: Path-based major versioning plus a SemVer `schemaVersion`
field. Major `N` lives in `contracts-events/events/vN/` and in the Java
package `com.hubinity.contracts.events.vN`. Minor and patch bumps stay in
the same directory and are signalled only by `schemaVersion`. Coexistence
of `vN` and `vN-1` is required for **≥1 release cycle** (per ADR 0005).
Operational rules live in `contracts-events/VERSIONING.md`.

## Consequences
- ✅ Side-by-side imports in Java (`v1.OrderPaid` vs `v2.OrderPaid`).
- ✅ Clear, file-system-level migration window contract.
- ✅ No new tooling — `jsonschema2pojo` already handles per-directory
  `targetPackage` configuration.
- ⚠️ Producers must dual-emit during the migration window.
- ⚠️ Each major bump requires duplicating files; intentional, keeps history
  readable.
