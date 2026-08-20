# ADR 0004 — JSON Schema + jsonschema2pojo for cross-service events

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
Cross-service events (`OrderPaid`, `StockChanged`, `ProductCreated`,
`PriceChanged`, `OrderCancelled`, `ProductUpdated`, `ProductDeactivated`,
`ServiceRevenueGenerated` — 8 schemas under `contracts-events/events/`) are
asynchronous payloads with no HTTP semantics. OpenAPI is built around
paths/operations/responses; using it for events forces fake paths and noisy
boilerplate.

## Decision Drivers
- Events are messaging payloads, not HTTP resources.
- The repo should use the format native to each communication style.
- No schema-registry infra (Confluent, Apicurio) is in place yet.

## Considered Options
- **OpenAPI for events** — pros: one generator; cons: fake paths,
  request/response wrapping is irrelevant noise, semantics mismatch. Rejected.
- **Avro** — pros: compact, versioned; cons: needs a schema registry we don't
  yet operate; binary payloads harder to debug at this stage. Rejected.
- **Protobuf** — pros: strong typing, performant; cons: harder to inspect,
  binary by default, contradicts our REST+JSON sync stack. Rejected.
- **JSON Schema 2020-12 + `jsonschema2pojo-maven-plugin`** — pros: native
  format for JSON payloads, draft-2020-12 is current, plugin produces
  Jackson-annotated POJOs identical in shape to the OpenAPI output; cons:
  two generators in the build. Accepted.

## Decision Outcome
**Chosen**: JSON Schema 2020-12 for events; `jsonschema2pojo-maven-plugin`
runs in `contracts-events/` with `annotationStyle=jackson2`,
`useJakartaValidation=true`, `includeJsr303Annotations=false`,
`generateBuilders=true`, `dateTimeType=java.time.OffsetDateTime`. Output is
merged via `build-helper-maven-plugin` so generated sources land under the
same `target/generated-sources/openapi/src/main/java` path used by the four
OpenAPI sub-modules.

## Consequences
- ✅ Each communication style uses its native schema format.
- ✅ Event POJOs match OpenAPI DTOs in shape (Jackson + `jakarta.*` +
  `OffsetDateTime` + `BigDecimal`).
- ⚠️ Two code generators in the build; justified by the clearer semantic
  separation between sync APIs and async events.
