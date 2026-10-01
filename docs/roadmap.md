---
title: Roadmap
description: Milestones M0 through M8, and what is currently done.
---

# Roadmap

Progress is tracked in `Todo.md` in the repository. This page is the summary.

## M0 — Toolchain smoke test ✅

The whole stack rested on one unverified assumption: that KSP works with AGP 9's
built-in Kotlin. All three questions were resolved on a throwaway project before
any module was written against it.

- The KGP version pin works, via a root `buildscript` block. `pluginManagement`
  has no `dependencies` block in the Kotlin DSL.
- KSP 2.3.12 works with AGP 9.0.1. Room emitted a schema, which is the only
  real proof the processor ran rather than being silently skipped.
- `jvmTarget` 17/17 holds on the *release* variant, where a mismatch would
  otherwise surface as an opaque D8 error.

No fallback to AGP 8.13.1 was needed. Details in [Toolchain](/toolchain.html).

## M1 — Shell and drawer

Set as the default home app. Multi-page home grid, dock row, and a searchable app
list. Establishes the home/drawer coordinator, the `AppRepository` over
`LauncherApps.Callback`, and the first Room entities.

## M2 — Icon pipeline and theme

The monochrome icon pipeline: prefer the app's own monochrome layer, fall back to
`ColorMatrix` grayscale, cache by `component + versionCode + themeVersion`, and
render off the main thread. Plus Doto typography and haptics.

## M3 — Home grid and hideable dock

Persisted layout, drag-and-drop with cell snapping, folder creation on drop, and
dock show/hide. The jank risk lives here, which is why the drag state is hoisted
and benchmarked rather than left to a late polish pass.

## M4 — Categories and smart search

Automatic categorisation with user override, category tabs, and the contacts
search provider including its first-launch rationale screen and `READ_CONTACTS`
request.

## M5 — Dot-matrix widgets

The clock, plus the weather readout driven by a user-configured provider. Void
takes no network permission.

## M6 — Widget hosting

Third-party widgets: bind, configure, resize. Isolated in `core:system` behind
crash guards, and deliberately after the grid is stable.

## M7 — Folders, gestures, settings

Folders, the full gesture set, settings, and Samsung/One UI options including
gesture conflict toggles and a battery-optimisation deep link.

## M8 — Polish

Macrobenchmark for startup, drawer scroll, and drag. Baseline profile.
Accessibility pass.

## Known open items

- No `LICENSE`-adjacent attribution policy for screenshots and demos. Apache 2.0
  does not require crediting screenshots, and pretending otherwise in the
  licence would be unenforceable. Worth stating as a request in the README if
  that matters.
- R8 keep rules for the launcher surface (`LauncherApps.Callback`,
  `AppWidgetProvider`, `AppWidgetHost`) are written conservatively up front and
  should be revisited in M6 once widget hosting is real.
