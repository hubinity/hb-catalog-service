/**
 * @hubinity/tailwind-preset
 *
 * Shared Tailwind CSS 4 design tokens for the Hubinity multi-frontend
 * ecosystem (HiBit backoffice + Star Coffee totem).
 *
 * Two distribution modes:
 *   1. JS preset (this file) — `presets: [preset]` in tailwind.config.ts
 *   2. CSS @theme files       — see ./theme.css and ./theme.totem.css
 */

import type { Config } from "tailwindcss";

/* ------------------------------------------------------------------ */
/* Brand palettes                                                      */
/* ------------------------------------------------------------------ */

/**
 * HiBit (backoffice) palette.
 * - primary: corporate blue, anchored on #1E40AF (Tailwind blue-800-ish)
 * - accent:  corporate green, anchored on #059669 (Tailwind emerald-600-ish)
 */
export const hibitColors = {
  primary: {
    "50": "#EFF4FF",
    "100": "#DBE5FE",
    "200": "#BFCFFE",
    "300": "#93AEFD",
    "400": "#6082FA",
    "500": "#3B5BF6",
    "600": "#2940EB",
    "700": "#1E40AF",
    "800": "#1E3A8A",
    "900": "#1E2F6B",
    "950": "#172044",
  },
  accent: {
    "50": "#ECFDF5",
    "100": "#D1FAE5",
    "200": "#A7F3D0",
    "300": "#6EE7B7",
    "400": "#34D399",
    "500": "#10B981",
    "600": "#059669",
    "700": "#047857",
    "800": "#065F46",
    "900": "#064E3B",
    "950": "#022C22",
  },
} as const;

/**
 * Star Coffee (totem) palette.
 * - primary: brown, anchored on #78350F (Tailwind amber-900-ish)
 * - accent:  cream, anchored on #FEF3C7 (Tailwind amber-100-ish)
 */
export const starCoffeeColors = {
  primary: {
    "50": "#FBF4EC",
    "100": "#F4E2CC",
    "200": "#E8C39A",
    "300": "#D89E68",
    "400": "#B97A3D",
    "500": "#965F25",
    "600": "#78350F",
    "700": "#612C0C",
    "800": "#4B230A",
    "900": "#371907",
    "950": "#1F0E04",
  },
  accent: {
    "50": "#FFFCF2",
    "100": "#FEF8E0",
    "200": "#FEF3C7",
    "300": "#FDE7A0",
    "400": "#FCD86F",
    "500": "#F7C744",
    "600": "#E2AB1F",
    "700": "#B5851A",
    "800": "#8A6516",
    "900": "#604611",
    "950": "#382806",
  },
} as const;

/**
 * Shared neutral grayscale — used by both brands for surfaces, text, borders.
 * Mirrors Tailwind's default `neutral` palette but is re-exported here so
 * consumers have a single import surface for tokens.
 */
export const neutralColors = {
  "50": "#FAFAFA",
  "100": "#F5F5F5",
  "200": "#E5E5E5",
  "300": "#D4D4D4",
  "400": "#A3A3A3",
  "500": "#737373",
  "600": "#525252",
  "700": "#404040",
  "800": "#262626",
  "900": "#171717",
  "950": "#0A0A0A",
} as const;

/* ------------------------------------------------------------------ */
/* Typography, spacing, breakpoints                                    */
/* ------------------------------------------------------------------ */

const fontFamily = {
  sans: ["Inter", "ui-sans-serif", "system-ui", "sans-serif"],
  totem: ["Manrope", "ui-sans-serif", "system-ui", "sans-serif"],
  mono: [
    "JetBrains Mono",
    "ui-monospace",
    "SFMono-Regular",
    "monospace",
  ],
} as const;

const spacing = {
  /** Minimum touch target (44px) — WCAG / totem requirement. */
  touch: "2.75rem",
  /** Comfortable touch target (56px) for the totem. */
  "touch-lg": "3.5rem",
} as const;

const fontSize = {
  "totem-sm": ["1.125rem", { lineHeight: "1.75rem" }],
  "totem-base": ["1.5rem", { lineHeight: "2.25rem" }],
  "totem-lg": ["2rem", { lineHeight: "2.75rem" }],
  "totem-xl": ["3rem", { lineHeight: "3.5rem" }],
} as const;

const borderRadius = {
  /** Soft rounded corner for touch surfaces. */
  touch: "0.75rem",
} as const;

const screens = {
  /** Typical kiosk landscape resolution. */
  totem: "1080px",
  /** Kiosk portrait fallback. */
  "totem-portrait": "768px",
} as const;

/* ------------------------------------------------------------------ */
/* Preset                                                              */
/* ------------------------------------------------------------------ */

const preset: Partial<Config> = {
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        hibit: hibitColors,
        starCoffee: starCoffeeColors,
        neutral: neutralColors,
      },
      fontFamily,
      spacing,
      fontSize,
      borderRadius,
      screens,
    },
  },
};

export default preset;
