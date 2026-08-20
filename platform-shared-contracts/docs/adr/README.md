# Architecture Decision Records

This directory captures the non-obvious choices baked into
`platform-shared-contracts`. Each ADR explains **why** a decision was made,
the alternatives considered, and the trade-offs accepted.

Current ADRs:

- [0001 — Use Maven multi-module layout](0001-use-maven-multi-module-layout.md)
- [0002 — Generate models only, not server stubs](0002-generate-models-only-not-server-stubs.md)
- [0003 — Jakarta EE namespace, Jackson serialization, no bean validation](0003-jakarta-ee-and-jackson-only.md)
- [0004 — JSON Schema + jsonschema2pojo for cross-service events](0004-jsonschema2pojo-for-events.md)
- [0005 — SemVer, `-SNAPSHOT` start, and N/N-1 coexistence](0005-semver-snapshot-and-coexistence.md)
- [0006 — CI builds only; GitHub Packages publishing deferred](0006-build-only-ci-no-publish-yet.md)

## Adding a new ADR

1. Copy the structure of an existing ADR.
2. Number it next-in-sequence (`000N-kebab-case-slug.md`).
3. Status starts as `Proposed`; flip to `Accepted` once merged.
4. Keep it 30–80 lines; link the relevant `pom.xml` line or file path.
5. Add the link to this README.

Format follows [MADR 3.0](https://adr.github.io/madr/) (short form).
