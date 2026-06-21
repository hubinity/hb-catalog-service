# ADR 0010 — MapStruct for entity ↔ DTO conversion

- **Status**: Accepted
- **Date**: 2026-06-19
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
The catalog has five entities, each with at least two conversion paths
(request → entity, entity → response, and for the mutable ones a partial
update path). The same pattern repeats across four backends in the Hubinity
platform. Hand-rolling these converters multiplies boilerplate and silently
hides type-drift bugs (a field renamed in the DTO but not in the converter
fails to copy, no compile error).

## Decision Drivers
- **Boilerplate elimination** — `O(entities × dto-variants)` mappers must not
  bloat the codebase.
- **Compile-time safety** — a rename in the DTO or entity must break the build.
- **Spring integration** — mappers should be injectable beans (no static
  utility classes plumbed through every service).
- **Zero runtime reflection** — keep the hot path allocation-free.

## Considered Options
- **Manual mappers** — rejected: boilerplate × N services; no compile-time
  enforcement against drift.
- **ModelMapper** — rejected: reflection-heavy, no compile-time safety, slower
  than generated code on hot paths.
- **Spring `BeanUtils.copyProperties`** — rejected: silently skips type
  mismatches, no support for partial-update semantics.
- **Chosen — MapStruct 1.6.x via annotation processor**, with global defaults
  declared in `pom.xml` (`-Amapstruct.defaultComponentModel=spring`,
  `-Amapstruct.unmappedTargetPolicy=IGNORE`).

## Decision Outcome
Every mapper has:
- `toEntity(request)` — populates a fresh entity, ignoring `id`, audit columns,
  and `deletedAt`.
- `toResponse(entity)` — straightforward field-for-field copy.
- For updatable entities, `void updateEntity(@MappingTarget entity, request)`
  annotated with `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` so
  partial PATCH payloads only overwrite the fields the client actually sent.

Component model is `spring`; mappers are injected via constructor like any
other bean. Implementation classes are generated under
`target/generated-sources/annotations/`.

## Consequences
- ✅ Compile-time generation — DTO/entity drift surfaces in `mvn compile`,
  not in production.
- ✅ Injectable beans — no service-layer plumbing or static singletons.
- ✅ Generated `*Impl.java` are plain Java — debuggable, no reflection.
- ⚠️ Stack traces step through the generated `*Impl.java` under
  `target/generated-sources/annotations/` rather than the interface — IDE
  navigation works but the file lives off the main `src/main/java` tree.
- ℹ️ This project does not use Lombok, so the well-known
  Lombok/MapStruct annotation-processor ordering concern does not apply.
