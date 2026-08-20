# ADR 0003 — Jakarta EE namespace, Jackson serialization, no bean validation

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
Generated DTOs ship with two cross-cutting concerns baked in: a namespace
(`javax.*` vs `jakarta.*`) and an annotation set (Jackson, JSR-303/Bean
Validation, Gson, etc.). Picking the wrong defaults forces every consumer
service to shim or strip annotations at runtime.

## Decision Drivers
- Target runtime is Spring Boot 4 on Jakarta EE 10 (`jakarta.*` only;
  `javax.*` is gone).
- Business validation rules differ per service (e.g. catalog allows partial
  drafts, order rejects them); they belong in the service, not the contract.
- The contract should describe **shape**, services own **rules**.

## Considered Options
- **`javax.*` + JSR-303 on DTOs** — pros: works on older Spring Boot 2.x;
  cons: incompatible with our Spring Boot 4 target. Rejected.
- **`jakarta.*` + JSR-303 on DTOs** — pros: validation "free" at the edge;
  cons: leaks service-specific business rules into the shared artifact;
  inconsistent rules across consumers cause friction. Rejected.
- **`jakarta.*` + Jackson only** — pros: pure data shape; consumers add
  Hibernate Validator if they want; cons: validation must be wired per
  service. Accepted.

## Decision Outcome
**Chosen**: `jakarta.*` + Jackson. Parent POM enforces `useJakartaEe=true`,
`serializationLibrary=jackson`, `useBeanValidation=false` for OpenAPI, and
`includeJsr303Annotations=false`, `annotationStyle=jackson2` for
`jsonschema2pojo`. The only `jakarta.*` dependency shipped is
`jakarta.annotation-api` (for `@Generated`).

## Consequences
- ✅ DTOs drop into Spring Boot 4 services with zero adapter code.
- ✅ Contract stays small and rule-free.
- ⚠️ Each service must register its own validator (Hibernate Validator) and
  declare its own constraints when stricter input enforcement is needed.
