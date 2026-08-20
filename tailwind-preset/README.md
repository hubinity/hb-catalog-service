# @hubinity/tailwind-preset

Shared **Tailwind CSS 4** design tokens for the Hubinity multi-frontend
ecosystem. Two brands — **HiBit** (corporate blue + green, backoffices)
and **Star Coffee** (brown + cream, totem) — one source of truth.

## Purpose

Hubinity ships four Angular 22 frontends:

- `hb-catalog-web`, `hb-support-web`, `hb-cashier-web` — HiBit backoffices
- `sc-totem-web` — Star Coffee customer-facing kiosk

Each frontend brings its own Tailwind installation. This package distributes
the colors, fonts, spacing, breakpoints and touch-target tokens they share,
so the visual identity stays consistent without copy-pasted config.

The package is **tokens only**. It deliberately does not export components,
utility classes, or opinionated layouts — those belong to each frontend's
own design system.

## Installation

```bash
npm install --save-dev @hubinity/tailwind-preset tailwindcss@^4
```

> The package is published to **GitHub Packages** under the `@hubinity`
> scope. Consumers need an `.npmrc` mapping the scope to the GitHub
> registry:
>
> ```
> @hubinity:registry=https://npm.pkg.github.com
> //npm.pkg.github.com/:_authToken=${NPM_TOKEN}
> ```

Fonts (Inter, Manrope, JetBrains Mono) are the **consumer's concern** —
install `@fontsource/inter`, `@fontsource/manrope`, `@fontsource/jetbrains-mono`
or load them from a CDN as needed. They are intentionally **not** runtime
dependencies of this preset.

## Consumption patterns

There are two equivalent ways to wire the preset in. Pick one per project.

### Pattern A — JS preset (back-compat with `tailwind.config.ts`)

For projects still using the JavaScript config:

```ts
// tailwind.config.ts
import type { Config } from "tailwindcss";
import preset from "@hubinity/tailwind-preset";

export default {
  presets: [preset],
  content: ["./src/**/*.{html,ts,tsx}"],
} satisfies Config;
```

### Pattern B — CSS-first (Tailwind 4 default, recommended)

For projects using Tailwind 4's CSS-first config:

**Backoffice frontends** (`hb-*-web`):

```css
/* app.css */
@import "tailwindcss";
@import "@hubinity/tailwind-preset/theme.css";
```

**Totem** (`sc-totem-web`):

```css
/* app.css */
@import "tailwindcss";
@import "@hubinity/tailwind-preset/theme.totem.css";
```

The `theme.totem.css` file layers a few overrides on top of the base tokens:
**Manrope** replaces Inter as the default sans, base text sizes are bumped
for kiosk distance viewing, and default transitions are softened.

## Available tokens

### Colors

Each brand exposes two ramps (`primary`, `accent`), both as Tailwind 4
`--color-*` CSS variables and as nested objects on the JS preset.

| Brand        | Ramp      | Anchor (hex) | Tailwind class example           |
|--------------|-----------|--------------|----------------------------------|
| HiBit        | primary   | `#1E40AF`    | `bg-hibit-primary-700`           |
| HiBit        | accent    | `#059669`    | `text-hibit-accent-600`          |
| Star Coffee  | primary   | `#78350F`    | `bg-star-coffee-primary-600`     |
| Star Coffee  | accent    | `#FEF3C7`    | `text-star-coffee-accent-200`    |

Each ramp ships shades `50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950`.

A shared `neutral` grayscale (50–950) is re-exported from the JS preset for
consumers that want a single import surface.

### Brand color hex reference

| Token                                     | Hex        | Notes                       |
|-------------------------------------------|------------|-----------------------------|
| `hibit-primary-500`                       | `#3B5BF6`  | HiBit interactive blue      |
| `hibit-primary-700`                       | `#1E40AF`  | HiBit corporate blue (logo) |
| `hibit-accent-500`                        | `#10B981`  | HiBit success / CTA green   |
| `hibit-accent-600`                        | `#059669`  | HiBit corporate green       |
| `star-coffee-primary-500`                 | `#965F25`  | Brand brown (mid)           |
| `star-coffee-primary-600`                 | `#78350F`  | Brand brown (deep, logo)    |
| `star-coffee-accent-200`                  | `#FEF3C7`  | Brand cream (canvas)        |
| `star-coffee-accent-500`                  | `#F7C744`  | Brand gold (highlight)      |

### Typography

| Token                | Stack                                       |
|----------------------|---------------------------------------------|
| `font-sans`          | Inter, ui-sans-serif, system-ui, sans-serif |
| `font-totem`         | Manrope, ui-sans-serif, system-ui, sans-serif |
| `font-mono`          | JetBrains Mono, ui-monospace, …             |

Totem-only font sizes (JS preset, also re-exposed in `theme.totem.css`):

| Token         | Size      | Line height |
|---------------|-----------|-------------|
| `totem-sm`    | 1.125rem  | 1.75rem     |
| `totem-base`  | 1.5rem    | 2.25rem     |
| `totem-lg`    | 2rem      | 2.75rem     |
| `totem-xl`    | 3rem      | 3.5rem      |

### Spacing — touch targets

| Token        | Value      | Rule                                 |
|--------------|------------|--------------------------------------|
| `touch`      | `2.75rem`  | **WCAG 2.1 minimum** — 44×44px       |
| `touch-lg`   | `3.5rem`   | Comfortable totem default — 56×56px  |

Use as `min-h-touch`, `min-w-touch`, `h-touch`, `w-touch`, `p-touch`, etc.
**Every interactive element in the totem must size ≥ `touch`** (PRD §14).
Backoffices should also respect this on tablet/mobile breakpoints.

### Border radius

| Token             | Value     | Use                       |
|-------------------|-----------|---------------------------|
| `rounded-touch`   | `0.75rem` | Default for touch surfaces |

### Breakpoints

| Token            | Value     | Use                                |
|------------------|-----------|------------------------------------|
| `totem`          | `1080px`  | Kiosk landscape                    |
| `totem-portrait` | `768px`   | Kiosk portrait fallback            |

### Dark mode

The preset sets `darkMode: "class"`. Toggle dark mode by adding `.dark` to
the `<html>` element.

## Versioning

This package follows **SemVer**:

- **PATCH** — internal refactors, comment fixes, no token changes.
- **MINOR** — additive tokens (new shades, new utilities).
- **MAJOR** — token renames, removed shades, breakpoint/spacing value changes,
  Tailwind major bumps that affect the preset surface.

Frontends should pin to a caret range (`^0.x`) until the package reaches
`1.0.0`, after which a caret range remains safe.

## Building

```bash
npm install
npm run build
```

Outputs:

- `dist/index.js` — ESM preset
- `dist/index.cjs` — CommonJS preset
- `dist/index.d.ts` — TypeScript declarations
- `dist/theme.css` — backoffice CSS @theme tokens
- `dist/theme.totem.css` — totem CSS @theme tokens

## Publishing (future — not in this release)

```bash
npm publish
```

Publishing requires:

- A `GITHUB_TOKEN` with `write:packages` scope in CI.
- A non-`SNAPSHOT` version (the publishing CI job is not yet wired up — see
  ADR 0008).

## License

UNLICENSED — internal Hubinity package.
