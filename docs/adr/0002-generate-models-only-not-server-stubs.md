# ADR 0002 — Generate models only, not server stubs

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
`openapi-generator-maven-plugin` can emit DTOs, API interfaces, server stubs
(Spring/JAX-RS/etc.), tests, documentation, and supporting files. This repo's
job is to publish the **contract**, not to dictate the runtime stack of the
services consuming it. Services here target Spring Boot 4 / Jakarta EE 10 and
choose their own Security/Resilience/Observability stack.

## Decision Drivers
- Avoid coupling the contract to a specific server framework version.
- Keep generated jars small and predictable (no random helper classes leaking
  into consumers' classpath).
- Prevent the generator from fighting our Spring Security configuration or
  forcing a specific Resilience4j integration.

## Considered Options
- **Full Spring server stubs** (`spring` generator) — pros: less code to write
  in services; cons: locks in Spring version, generates filters/configs that
  collide with our security setup, ships unwanted dependencies. Rejected.
- **APIs + models** — pros: shared interfaces; cons: same coupling risk,
  forces a reactive-vs-blocking decision at the contract layer. Rejected.
- **Models only** — pros: pure data shape, framework-agnostic; cons: each
  service writes its own controllers. Accepted.

## Decision Outcome
**Chosen**: Models-only generation. Parent POM `<pluginManagement>` pins:
`generateApis=false`, `generateModels=true`, `generateSupportingFiles=false`,
`generateApiTests=false`, `generateApiDocumentation=false`,
`generateModelTests=false`, `generateModelDocumentation=false`. Each service
implements its own controllers against the shared DTOs.

## Consequences
- ✅ Contract artifacts contain only DTOs — no framework lock-in.
- ✅ Services are free to evolve their HTTP layer without breaking the
  contract jar.
- ⚠️ Each service writes its own controller signatures from the same YAML;
  enforced by code review rather than a compiler.
