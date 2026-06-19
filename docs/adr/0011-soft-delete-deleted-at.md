# ADR 0011 — Soft delete via `deleted_at TIMESTAMPTZ`

- **Status**: Accepted
- **Date**: 2026-06-19
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
HiBit's catalog (and every downstream Hubinity service) needs a way to remove
records without breaking historical references. A past order that quotes a
product that has since been "removed" must still render correctly; auditors
need to see what the catalog looked like at the time of the sale. A hard
`DELETE` against `product` would cascade into reporting and event-history
gaps. We need a uniform policy that is cheap to apply across every service.

## Decision Drivers
- **Historical integrity** — references from `sc-order-service`, audit logs
  and event payloads must keep resolving.
- **Operational simplicity** — one column, one convention, no archive tables.
- **Slug / SKU reuse** — natural keys (slug, SKU) must be reclaimable once a
  record is logically gone; only *alive* rows enforce uniqueness.
- **Uniform pattern** — every service learns the rule once and applies it
  everywhere; no per-service archive schemas to maintain.

## Considered Options
- **Hard `DELETE`** — pros: simplest model; cons: breaks historical FK refs,
  forces every reader to handle "missing parent" defensively. Rejected.
- **`is_deleted BOOLEAN`** — pros: trivial flag; cons: loses the *when*
  (timestamp), forcing a parallel `deleted_at` column anyway. Rejected.
- **Separate `*_archive` tables** — pros: physical isolation, smaller live
  indexes; cons: doubles DDL, breaks live queries that need historical rows,
  operational cost (migrations × 2). Rejected.
- **Event-sourcing per entity** — pros: full history; cons: massive
  complexity for an MVP catalog. Rejected.
- **`deleted_at TIMESTAMPTZ NULL`** — accepted (see below).

## Decision Outcome
**Chosen**: every "primary" business entity gets a nullable
`deleted_at TIMESTAMPTZ` column.
- `deleted_at IS NULL` ⇒ alive (the only state most queries care about).
- `deleted_at IS NOT NULL` ⇒ soft-deleted; the value records *when* the row
  was retired.

Append-only journals (`stock_movement`) do **not** get this column — they
are mutated by INSERT only and never "removed".

UNIQUE constraints on natural keys are expressed as *partial* unique
indexes filtered by `WHERE deleted_at IS NULL`, so the same slug or SKU can
be reused once the prior row is soft-deleted.

## Consequences
- ✅ Reads default to `WHERE deleted_at IS NULL`. JPA mappings in task 1.4
  will use `@SQLRestriction("deleted_at IS NULL")` to make this implicit.
- ✅ Natural-key uniqueness via partial indexes (`ux_product_sku_alive`,
  `ux_category_slug_alive`) lets operators reclaim a SKU/slug after removal.
- ✅ Historical FKs keep resolving — `ON DELETE RESTRICT` is paired with the
  soft-delete convention so a row with dependants must be soft-deleted, not
  hard-deleted.
- ⚠️ Storage grows linearly with churn. A periodic purge job (cut-off
  policy by retention window) is deferred to Phase 5; document it in the
  ops runbook when the volume actually hurts.
- ⚠️ Index size increases. Partial indexes mitigate this for natural keys;
  for foreign-key indexes we accept the cost.
- ⚠️ Joins to soft-deleted parents must be explicit — readers crossing
  service boundaries (e.g. order → product) need to opt into showing
  historical names.

## Applied to (Phase 1 catalog schema)
- **Yes**: `category`, `product`.
- **No**: `stock_item` (1:1 lifetime with product), `stock_movement`
  (journal), `stock_reservation` (state machine: `ACTIVE` → `COMMITTED` |
  `RELEASED` | `EXPIRED`).
