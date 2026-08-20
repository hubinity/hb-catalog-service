# ADR 0006 — CI builds only; GitHub Packages publishing deferred

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
The repo is greenfield. We want a green build signal on every push/PR before
we wire up registry auth and start pushing artifacts. Publishing too early
pollutes GitHub Packages with versions of unstable contracts.

## Decision Drivers
- Need fast feedback that the multi-module build is healthy.
- Avoid leaking half-baked contract versions into a shared registry.
- Defer secret management (`GITHUB_TOKEN`, package scopes) until we are
  ready to consume the artifacts from outside this repo.

## Considered Options
- **Publish on first push** — pros: consumers can resolve from day 1; cons:
  pollutes registry with unstable versions; requires auth secrets before the
  build is even known to be green. Rejected.
- **Build + publish on tag only** — pros: cleaner registry; cons: still
  requires the publish pipeline to exist; premature for a repo with no
  external consumers yet. Deferred (likely the eventual target).
- **Build-only CI now, publish in a follow-up** — pros: green signal
  immediately, zero secrets needed; cons: consumers must install locally
  (`mvn install`) until publish lands. Accepted.

## Decision Outcome
**Chosen**: `.github/workflows/ci.yml` runs `mvn -B -ntp verify` on push to
`main`/`develop` and on PRs to `main`. The parent POM keeps
`<distributionManagement>` commented out (parent POM lines 145–162) with a
TODO referencing the follow-up. A `settings.xml` template documenting the
expected `${env.GITHUB_USERNAME}` / `${env.GITHUB_TOKEN}` is shipped so the
switch to publish is a one-PR change.

## Consequences
- ✅ Green/red signal from day 1 with no secret setup.
- ✅ Registry stays clean until contracts stabilise.
- ⚠️ Consumer services must `mvn install` `0.1.0-SNAPSHOT` into their local
  `~/.m2` until a Fase 0 follow-up enables `mvn deploy`.
