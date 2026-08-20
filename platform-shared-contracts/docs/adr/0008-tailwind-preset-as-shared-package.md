# ADR 0008 — Tailwind preset as shared npm package

- **Status**: Accepted
- **Date**: 2026-06-18
- **Deciders**: Hubinity Platform team

## Context and Problem Statement
Hubinity ships four Angular 22 frontends — three HiBit backoffices
(`hb-catalog-web`, `hb-support-web`, `hb-cashier-web`) and one Star Coffee
totem (`sc-totem-web`). All four use Tailwind CSS 4 and must converge on a
single visual language across **two brands** (HiBit corporate blue/green
and Star Coffee brown/cream) without copy-pasting tokens between repos.

## Decision Drivers
- Cross-frontend consistency of colors, typography, spacing, and breakpoints.
- Single source of truth for design tokens — token rename happens in one
  place, not four.
- Low-friction adoption: each frontend should pull tokens with a single
  dependency and one import line.
- Touch-target tokens (WCAG ≥ 44px, PRD §14) must be enforceable without
  per-repo configuration drift.

## Considered Options
- **Per-frontend Tailwind config** — rejected: inevitable drift across four
  repos, no central review point for brand changes.
- **Generated tokens from a design tool (Figma Tokens Studio, Style
  Dictionary)** — overkill for the MVP scope; reconsider once the design
  system has a dedicated owner.
- **New dedicated `platform-design-tokens` repo** — would push the multi-repo
  count from 12 to 13 with no operational benefit at MVP scale.
- **Chosen**: sub-folder of `platform-shared-contracts/` named
  `tailwind-preset/`, distributed as an npm package
  (`@hubinity/tailwind-preset`) via **GitHub Packages**, mirroring the Maven
  contracts distribution model already established for backend services
  (ADR 0006).

## Decision Outcome
**Chosen**: `@hubinity/tailwind-preset` lives in
`platform-shared-contracts/tailwind-preset/`. It ships both:

1. A **JS preset** (`dist/index.{js,cjs,d.ts}`) for projects using
   `tailwind.config.ts`, and
2. **CSS `@theme` files** (`dist/theme.css`, `dist/theme.totem.css`) for
   Tailwind 4's CSS-first configuration mode.

The package exposes tokens only — no components, no opinionated utility
classes, no layout primitives. Fonts (Inter, Manrope, JetBrains Mono) are
explicitly the **consumer's** responsibility and are not runtime deps.

## Consequences
- ✅ Frontends consume tokens via one dependency and one import line.
- ✅ Brand updates (e.g., a new HiBit accent) propagate via a single PR plus
  a coordinated minor version bump.
- ✅ Touch-target tokens (`touch` = 44px, `touch-lg` = 56px) are enforceable
  from a shared vocabulary — `min-h-touch` reads the same in every repo.
- ✅ Re-using GitHub Packages mirrors the Maven contracts model (ADR 0006);
  no new registry infrastructure required.
- ⚠️ Consumers need an `.npmrc` mapping `@hubinity` to
  `npm.pkg.github.com` — documented in the package README.
- ⚠️ Tailwind major version bumps may require coordinated preset version
  bumps; tracked via the preset's own SemVer.
- ⚠️ Sub-folder layout means the package shares a git history with the
  Maven contracts; a future split into `platform-design-tokens` remains
  possible (a tagged release of the sub-folder is forward-compatible).
