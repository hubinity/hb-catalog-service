# ADR 0009 — UUID v7 PKs generated DB-side via `uuidv7()`

- **Status**: Accepted
- **Date**: 2026-06-19
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
PRD §5 selects UUID v7 as the primary-key strategy for every Hubinity microservice
(time-ordered, RFC 9562). Postgres 18 ships a native `uuidv7()` function; the
local docker-compose stack and the current Supabase tier are Postgres 16, which
does not. We need one strategy that works against both today and tomorrow.

## Decision Drivers
- **Index locality** — v7 IDs are monotonically increasing within a millisecond,
  keeping B-tree leaf inserts hot and avoiding the random-write penalty of v4.
- **No app-side coordination** — multiple Spring instances can INSERT in parallel
  without a shared clock or sequence.
- **Forward-portable to PG 18 native** — when Supabase upgrades, swapping the
  user-defined function for the built-in must be a one-line change.
- **Single-vendor stance** — PRD §3 already commits to Postgres; we may use
  Postgres-specific features without abstraction layers.

## Considered Options
- **App-side via a Java UUID v7 library** — rejected: clock skew between replicas
  breaks the monotonic guarantee; also forces every entry point (controller,
  RabbitMQ consumer, scheduled job) to wire the generator.
- **Postgres 18 native `uuidv7()`** — deferred: not yet available on our PG 16
  baseline. Will switch on PG 18 upgrade.
- **Chosen — plpgsql `uuidv7()` function declared in `V1__init.sql`**, used as
  the `DEFAULT` for every primary-key column.

## Decision Outcome
Every UUID PK column is declared `DEFAULT uuidv7()` in the migration. On the
Hibernate side the field is mapped as:

```java
@Id
@Column(name = "id", insertable = false, updatable = false, nullable = false)
@org.hibernate.annotations.Generated(event = org.hibernate.generator.EventType.INSERT)
private UUID id;
```

`insertable = false` tells Hibernate to omit `id` from the INSERT statement, the
DB `DEFAULT uuidv7()` fires, and `@Generated(INSERT)` makes Hibernate refresh
the assigned value via the `RETURNING` clause.

## Consequences
- ✅ Deterministic ordering — index scans on time-correlated queries stay tight.
- ✅ No app-side UUID coordination — every replica inserts independently.
- ⚠️ Postgres-specific. Acceptable per the single-vendor PRD stance.
- ⚠️ Migration to PG 18 native `uuidv7()` is trivial: drop the user-defined
  function (or alias it to the native one) — the column DEFAULTs are unchanged.

## Applied to
Four entities use the DB-side default: `Category`, `Product`, `StockMovement`,
`StockReservation`. **NOT** `StockItem` — its PK is the FK `product_id`, which
is supplied at INSERT time; that entity therefore does NOT carry the
`@Generated(INSERT)` annotation.
