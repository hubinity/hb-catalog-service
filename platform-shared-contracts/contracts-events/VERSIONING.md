# Event Schema Versioning

## Purpose
Rules for evolving event schemas without breaking consumers. Events are
published cross-service; each consumer must be able to upgrade on its own
cadence. This document defines how producers signal changes and how
consumers tolerate them.

## Path layout
Schemas live under `contracts-events/events/vN/<Name>.schema.json`, where
`N` is the **major** version. Each major version is a separate physical
directory. Generated POJOs mirror the path in `com.hubinity.contracts.events.vN`.

## `schemaVersion` field
Every event payload carries a top-level `schemaVersion` string formatted as
SemVer (`N.x.y`). The major component MUST match the directory (`vN`). The
producer sets it on every emission. The consumer validates it and rejects
payloads whose major does not match a known version.

## Backward-compatible changes
Stay in the same `vN/` directory. Bump `schemaVersion` to `N.x.y`.
Permitted:
- Add **optional** fields.
- Widen enums (add new allowed values).
- Tighten a regex **only if** all current producers already comply.
- Relax `required` to optional (with caution; consumers may already key off
  presence).

## Breaking changes
Create a new `events/v(N+1)/` directory. Initial `schemaVersion` is
`N+1.0.0`. Breaking includes:
- Remove or rename a field.
- Narrow a type (e.g. `string` → `string` with stricter `format`).
- Add a new `required` field.
- Change semantic meaning of an existing field.

## Coexistence
When `vN+1` ships, the `vN/` schemas remain in place for **≥1 release
cycle** (per ADR 0005). During the migration window:
- Producers dual-emit `vN` and `vN+1` events.
- Consumers subscribe to both queues / topics until they are upgraded.
- Once all consumers report on `vN+1`, the `vN/` schemas may be removed in
  the next release.

## Java packages
Generated classes use `com.hubinity.contracts.events.vN.*`. Because the
version is part of the package, consumers can `import` both versions
side-by-side:

```java
import com.hubinity.contracts.events.v1.OrderPaid;
import com.hubinity.contracts.events.v2.OrderPaid;
```

This is the mechanism that makes the coexistence window practical.

## Reference
- `docs/adr/0004-jsonschema2pojo-for-events.md` — why JSON Schema, not
  OpenAPI/AsyncAPI, for event payloads.
- `docs/adr/0007-event-versioning-by-path.md` — the path-based versioning
  policy this document operationalises.
