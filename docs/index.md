---
title: Void Launcher
description: >-
  An Android home launcher with a monochrome, dot-matrix aesthetic and a strict
  black/white/gray palette with a single red accent.
---

# Void Launcher

An Android home launcher built around a monochrome, dot-matrix aesthetic: pure
black, white foreground, one muted grey, and a single red accent used only for
indicators and active states. Kotlin and Jetpack Compose, nine modules, no DI
framework, no network permission.

<p style="margin:1.5rem 0">
  <a href="https://github.com/CephandriusMaxtori/void">Source on GitHub</a>
</p>

## What it does

- **Monochrome icons.** Prefers each app's own `AdaptiveIconDrawable` monochrome
  layer, which every Android 13+ device has. `minSdk` is 33 specifically so
  there is no population of users getting a muddy grayscale fallback.
- **Dot-matrix clock and readouts.** Doto, an open font, rendered as a typeface
  rather than a per-frame alpha-sampling effect, with a Canvas dot-grid
  renderer reserved for the weather readout where dot density needs independent
  control.
- **Multi-page home grid.** Pages, folders, a hideable dock, and drag-and-drop
  with cell snapping.
- **Categorised drawer.** Conservative keyword and package-prefix rules with full
  user override. Never hides an app the categoriser does not understand.
- **Search** across apps, contacts, and simple calculations.

## What it deliberately does not do

- No network access. Not "no analytics" — no `INTERNET` permission at all. The
  weather widget reads from a provider the user configures themselves, so
  weather data does not transit Void by default.
- No dangerous permissions for the core experience. `READ_CONTACTS` is the one
  exception, requested at first launch with a rationale screen, and declining
  leaves the app fully functional.
- No Glyph Interface, no third-party launcher assets, fonts, or branding.

## Documentation

<ul class="cards">
  <li>
    <a href="/design-v0.3.html">Design</a>
    <p>Goals, components, tokens, milestones, and the reasoning behind the
       decisions that were contested.</p>
  </li>
  <li>
    <a href="/architecture.html">Architecture</a>
    <p>Module graph, the six dependency rules, the manual DI pattern, and how
       to keep R8 from quietly breaking a launcher.</p>
  </li>
  <li>
    <a href="/toolchain.html">Toolchain</a>
    <p>Pinned versions, the AGP 9 behaviour changes that matter, and a
       catalogue of config traps that each produced a misleading error.</p>
  </li>
  <li>
    <a href="/roadmap.html">Roadmap</a>
    <p>Milestones M0 through M8 and what is currently done.</p>
  </li>
</ul>

## Status

Pre-M1. The toolchain is verified and the nine-module skeleton builds; the
feature milestones have not started. See the [roadmap](/roadmap.html).

## Licence

Apache 2.0. The bundled [Doto](https://fonts.google.com/specimen/Doto) font is
under the SIL Open Font License and is separately licensed — see
[NOTICE](https://github.com/CephandriusMaxtori/void/blob/main/NOTICE).
