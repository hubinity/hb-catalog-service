# ADR 0005 — SemVer, `-SNAPSHOT` start, and N/N-1 coexistence

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
This repo is consumed by ≥6 services that release independently. Breaking a
shared DTO without warning would cascade through the platform. We need a
versioning policy and a migration window that lets each consumer move on its
own schedule.

## Decision Drivers
- Multiple independent consumer release cadences.
- "Lockstep upgrade" is impractical at six+ services.
- Greenfield repo: contracts are not yet stable, so we should not pretend a
  `1.0` GA.

## Considered Options
- **No SemVer / "always latest"** — pros: simplest; cons: silent breaking
  changes; impossible to reason about consumer impact. Rejected.
- **SemVer with lockstep upgrade** — pros: only one version live at a time;
  cons: forces a synchronized release across all services for any major bump.
  Rejected.
- **SemVer + N/N-1 coexistence** — pros: consumers migrate on their own
  cadence inside a defined window; cons: contract producers must keep the
  old API surface alive during the window. Accepted.

## Decision Outcome
**Chosen**: SemVer. Current version is `0.1.0-SNAPSHOT` (parent POM, line 9),
signalling pre-GA instability. Breaking changes bump **major**; backward-
compatible additions bump **minor**; doc/build-only changes bump **patch**.
When a major is released, the previous major must remain published and
buildable for **≥1 release cycle** so consumers can migrate without an
emergency window.

## Consequences
- ✅ Consumers can plan upgrades; no silent breakage.
- ✅ `-SNAPSHOT` clearly flags pre-stable status to anyone wiring this in.
- ⚠️ During migration windows, contract authors maintain two surfaces
  (e.g. `contracts-order` v1.x and v2.x in parallel). Cost is intentional and
  bounded by the one-cycle rule.
