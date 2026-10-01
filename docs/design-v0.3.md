# Void Launcher

**Design Doc · Draft v0.3**

**Status:** Draft v0.3
**Package:** `com.hoid.void`
**Stack:** Kotlin, Jetpack Compose, Gradle (KTS)
**Min SDK:** 33
**Target SDK:** 36 · **Compile SDK:** 36
**Target:** Phones (Samsung / One UI priority, other OEMs supported)

---

## 0. What changed from v0.2

| Change | Reason |
|---|---|
| `minSdk` 29 -> **33** | Monochrome icon layers are API 33+. On 29–32 every icon fell back to grayscale, which undercut the app's core identity. |
| DI: framework -> **manual `AppContainer`** | Keeps KSP carrying only Room. Constructor injection throughout, fully testable. |
| Home pages: open question -> **multiple pages in v1** | The pages model is foundational, not an additive later change. |
| §4.7: **swipe-down notification shade removed** | `StatusBarManager.expandNotificationsPanel()` is a no-op for third-party launchers. |
| §4.4: **Doto rendered as a font** for v1 | ~90% of the dot-matrix look for ~5% of the work. Canvas dot-grid deferred to the weather readout. |
| §7: **permissions stance rewritten** | `READ_CONTACTS` is requested at first launch. Core home/drawer need no permissions. |
| §8 M5: **clock + user-configured weather provider** | Keeps the no-network-permission promise. Void never holds network access. |
| §10: **all open questions resolved** | See §11. |
| New: **KSP / AGP 9 compatibility risk** | Recorded in §9 with a mandatory smoke test in Phase 0. |

---

## 1. Overview

Void Launcher is an Android home launcher that recreates the look and feel of the Nothing Launcher — monochrome icons, dot-matrix typography and widgets, strict black/white/gray palette with a single red accent, and crisp haptics — while adding selected productivity features inspired by Smart Launcher.

It copies the *style*, not Nothing's assets, fonts, or branding.

**Core identity**
- Pure black backgrounds
- Monochrome (Material) icons
- Dot-matrix clock & widgets
- Red accent used sparingly
- Clean, calm, focused home screen

**Added Smart Launcher-inspired features**
- Automatic app categories in the drawer
- Smarter search (apps, contacts, quick calc, etc.)
- Hideable dock
- One-handed friendly layout bias
- Multiple home pages

---

## 2. Goals and Non-Goals

**Goals**
- Fully functional home screen: app drawer, multi-page home grid, folders,
  widgets, search, gestures
- Convincing Nothing-inspired aesthetic driven by a theming layer
- Automatic categorization + powerful search
- Hideable dock and one-handed considerations
- Smooth scrolling (60 fps target on mid-range devices)
- Clean module boundaries (UI, data, system integration independently testable)
- Good behavior on Samsung / One UI

**Non-Goals (v1)**
- Glyph Interface / hardware-specific Nothing features
- Recreating Nothing's proprietary fonts, wallpapers, or icon packs
- Full free-form / non-grid layout (keep structured grid)
- Multi-user / work-profile polish beyond basic support
- Tablet, foldable, and DeX layouts
- Built-in news/RSS reader or heavy adaptive blur/glass effects
- Any network access — weather is a user-configured provider (§7)

---

## 3. Architecture

Single-activity, MVVM with unidirectional data flow.

| Module | Responsibility |
|--------|----------------|
| `app` | `LauncherActivity`, manifest, manual DI wiring (`com.hoid.void`) |
| `core:system` | `LauncherApps` wrapper, package callbacks, shortcuts, widget host |
| `core:data` | Room (grid, pages, folders, hidden apps, categories), DataStore (settings) |
| `core:icons` | Monochrome icon pipeline + Material Icons fallback + cache |
| `core:designsystem` | Theme tokens, typography, dot-matrix renderer, haptics |
| `feature:home` | Home grid, page pager, drag & drop, dock (hideable) |
| `feature:drawer` | Categorized app drawer, search, alphabet scroller |
| `feature:widgets` | Widget picker, binding, resize, popup widgets (future) |
| `feature:settings` | Grid size, icon style, gestures, Samsung-specific options |

All nine modules are declared from day one so the boundaries are enforced and
the architecture stays honest, but each gains real code only as its milestone
arrives.

### 3.1 Dependency injection

Manual. A single `AppContainer` in `app` constructs and owns the object graph;
everything below receives its dependencies through constructor parameters. No
annotation processor is used for DI — KSP carries only Room.

This keeps builds fast, avoids plugin compatibility risk, and makes every
dependency explicit in a test fixture.

### 3.2 Manifest

The activity declares `HOME` + `DEFAULT` intent filters, plus:

| Attribute | Value | Why |
|---|---|---|
| `launchMode` | `singleTask` | One home instance |
| `excludeFromRecents` | `true` | Not a task the user returns to |
| `stateNotNeeded` | `true` | System may kill and restore without saved state |
| `configChanges` | `orientation\|screenSize\|smallestScreenSize\|screenLayout\|keyboard\|keyboardHidden\|navigation\|uiMode\|density\|layoutDirection` | A launcher restarting on rotation is very visible |
| `windowSoftInputMode` | `adjustResize` | Search field must stay visible |

---

## 4. Key Components

### 4.1 App repository
- Source of truth: `LauncherApps.getActivityList()`, kept live via `LauncherApps.Callback`
- Exposed as `StateFlow<List<AppEntry>>`
- `AppEntry` contains component name, user handle, label, icon key, and **category**
- Categories are auto-assigned (Communication, Tools, Media, Games, System, etc.)
  with user override support

### 4.2 Home grid and pages
- **Multiple home pages in v1**, presented as a horizontal pager with a page
  indicator
- Fixed N×M cells (default 4×6) + dock row per page
- Items store `page, x, y, spanX, spanY`
- Pages support add / remove / reorder, with a cap to prevent runaway growth
- Horizontal pager content carries side insets (or a gesture-exclusion rect) so
  One UI's edge-back gesture does not collide with page swipes
- Persisted in Room
- Drag-and-drop with cell snapping and folder creation on drop
- **Hideable dock**: double-tap (or configurable gesture) shows/hides the dock

### 4.3 Monochrome icon pipeline
1. Prefer `AdaptiveIconDrawable.monochrome` (API 33+)
2. Fallback: grayscale + contrast threshold via `ColorMatrix`
3. Material Icons used as clean placeholders / overrides where needed
4. LRU memory + disk cache keyed by `component + versionCode + themeVersion`
5. Off-main-thread rendering (`Dispatchers.Default`)

Because `minSdk` is 33, step 1 applies to every supported device. The step 2
fallback is now a rare edge case rather than the common path.

### 4.4 Dot-matrix typography
- **v1: Doto (OFL) rendered as a normal Compose font.** It is already a dotted
  display face, so this delivers most of the effect at a fraction of the cost.
- A Canvas dot-grid renderer (sampling glyph alpha into a configurable dot grid)
  is retained for the weather / readout widgets, where dot density and weight
  need independent control.
- Animated digit transitions apply to the clock.
- Doto ships under the SIL Open Font License and remains OFL regardless of the
  project's own license. The OFL text and attribution must ship with the font.

### 4.5 Categorized App Drawer + Search
- Horizontal category tabs: All · Comm · Tools · Media · Games · System (extensible)
- Auto-categorization on first load + when packages change
- Search bar supports:
  - Apps
  - Contacts (gated behind `READ_CONTACTS`, see §7)
  - Simple calculations
  - (Future: custom providers)
- Alphabet scroller retained

### 4.6 Widget hosting
- Standard `AppWidgetHost` + `AppWidgetManager`
- Built-in widgets (dot-matrix clock) are pure Compose
- Weather is driven by a **user-configured provider**; Void holds no network
  permission and makes no requests of its own
- Popup widgets planned as a later enhancement

### 4.7 Gestures & system integration
- Swipe up -> drawer. Legitimate: as the `HOME` app we own the system home gesture.
- ~~Swipe down -> notification shade~~ **Removed.**
  `StatusBarManager.expandNotificationsPanel()` does nothing for third-party
  launchers on modern Android. Carrying a dead gesture is worse than not having it.
- Double-tap -> hide/show dock (primary) or lock (optional accessibility)
- Long-press -> context menu / widget picker
- Predictive back + edge-to-edge insets handled in root scaffold
- One UI edge gestures contend with the page pager; resolved with horizontal
  insets or a gesture-exclusion rect, verified on device

### 4.8 Samsung / One UI considerations
- Status bar and navigation bar remain system-controlled (One UI style)
- Feature-detect Samsung and offer:
  - Gesture conflict toggles
  - Battery optimization deep-link
  - Optional "force monochrome" override
- Graceful degradation where system shade behavior is unavailable

---

## 5. Design Tokens

| Token | Value |
|-------|-------|
| Background | `#000000` (dark) / `#F5F5F5` (light) |
| Foreground | `#FFFFFF` / `#0A0A0A` |
| Muted | `#8A8A8A` |
| Accent | Nothing-style red `#D71921` (indicators, active states only) |
| Shapes | 20–28 dp rounded cards, thin 1 dp outlines |
| Motion | 150–250 ms, snappy easing (no overshoot) |
| Haptics | `CLOCK_TICK` / `CONFIRM` on snap, drop, toggle |

---

## 6. Performance Plan

- Stable keys + `contentType` in all lazy grids
- Icon loads isolated via `produceState` + cache
- Baseline Profile + R8 enabled for release
- Targets:
  - Drawer scroll >= 60 fps on mid-range
  - Cold start to interactive home < 500 ms
- Profiled with Macrobenchmark (startup, drawer scroll, drag)
- The page pager adds a horizontal scroll surface; it is benchmarked alongside
  the vertical drawer to catch jank introduced by the pager

---

## 7. Permissions and Privacy

**Core features require no permissions.** Home grid, pages, dock, and app
browsing work with an empty permission set.

| Permission | When | Why |
|---|---|---|
| `READ_CONTACTS` | Requested at first launch, for contacts search | Dangerous + runtime-gated. Requested with a rationale screen explaining the benefit *before* the system dialog — a bare prompt for a secondary feature reads as malware-adjacent. Declining leaves the app fully functional. |
| `BIND_APPWIDGET` | System dialog, when adding a third-party widget | Required by the platform |
| Accessibility service | Optional, only if the user enables double-tap-to-lock | Advanced gestures only |
| Usage access | **Not in v1** | Only if "recent apps" is added later |

**No network permission.** Void requests no network access at all in v1. The
weather widget (§4.6) reads from a provider the user configures themselves, so
weather data never transits Void by default.

No analytics. Categories and search stay fully on-device.

---

## 8. Milestones

| # | Milestone | Outcome |
|---|-----------|---------|
| M0 | Toolchain smoke test | KSP + Room proven against AGP 9 built-in Kotlin. **Blocking.** |
| M1 | Shell + drawer | Set as default home, multi-page home grid, searchable app list |
| M2 | Icon pipeline + theme | Monochrome icons, design tokens, haptics, Doto typography |
| M3 | Home grid + hideable dock | Persisted layout, drag & drop, dock show/hide |
| M4 | Categories + smart search | Auto-categorized drawer, contacts (permission-gated), calc |
| M5 | Dot-matrix widgets | Clock, plus user-configured weather readout |
| M6 | Widget hosting | Third-party widgets: bind, configure, resize |
| M7 | Folders, gestures, settings | Feature-complete v1 (incl. Samsung options) |
| M8 | Polish | Benchmarks, baseline profile, accessibility pass |

---

## 9. Risks

| Risk | Mitigation | Status |
|---|---|---|
| **KSP incompatible with AGP 9 built-in Kotlin** | Mandatory Phase 0 smoke test. Fallback: AGP 8.13.1 (also cached) | **Open** |
| **KGP version pin mechanism unknown under built-in Kotlin** | Verified in Phase 0 | **Open** |
| `jvmTarget` mismatch (AGP 9 moved Java defaults to 11) | Pin both `compilerOptions.jvmTarget` and `compileOptions` to 17; verify in Phase 0 | **Open** |
| Widget hosting edge cases | Isolate in `core:system`, crash guards, build after grid is stable | Planned |
| Many apps lack monochrome layers | ColorMatrix fallback + per-app override list. Rare now that `minSdk` is 33 | Planned |
| Drag-and-drop jank in Compose | Hoist drag state, overlay shadow, test early (M3) | Planned |
| Trademark / asset concerns | Original assets only; Doto is OFL; style inspiration only | Mitigated |
| OEM variance (Samsung gestures, shade) | Feature-detect + graceful degradation + settings toggles | Planned |
| One UI edge gestures vs page pager | Horizontal insets / gesture exclusion, verified on device | Planned |
| Category quality | Simple rules + user re-categorization | Planned |
| Custom license has no express patent grant | Revisit before any public release | Open |

---

## 10. Toolchain

Pinned and verified against local availability.

| Component | Version | Note |
|---|---|---|
| AGP | 9.0.1 | Cached locally |
| Gradle | 9.2.0 | Cached locally |
| Kotlin (KGP) | 2.3.20 | Cached locally |
| JDK | 17 (Temurin) | AGP 9 minimum |
| compileSdk / targetSdk | 36 | AGP 9.0 max is API 36.1 |
| minSdk | 33 | |
| Compose BOM | 2026.09.00 | |
| Compose compiler plugin | tied to KGP 2.3.20 | `org.jetbrains.kotlin.plugin.compose` |
| Room | 2.8.5 | |
| KSP | 2.3.12 | Decoupled from KGP since 2.3.0 — verify in Phase 0 |
| DataStore | 1.2.1 | |
| Coroutines | 1.11.0 | |
| Font | Doto (OFL) | |
| DI | manual | No framework |

Gradle requires `local.properties` with `sdk.dir`; `ANDROID_HOME` and
`ANDROID_SDK_ROOT` are unset in this environment.

---

## 11. Resolved Open Questions

The five questions v0.2 left open:

1. **Single page or multiple?** -> **Multiple pages in v1.** The `page` column was
   already in the item model; the pages entity, pager, and page management are
   built from M1 rather than retrofitted.
2. **Expose the dot-matrix clock as an `AppWidgetProvider` too?** -> Not in v1.
   Revisit when widget hosting (M6) exists and the pattern is proven.
3. **How aggressive should auto-categorization be?** -> Conservative keyword
   and package-prefix rules with full user override. Never hide an app the
   categorization does not understand.
4. **Open source from the start?** -> Custom license. Note that Doto remains OFL
   regardless, and a custom license has no express patent grant — revisit before
   public release.
5. **Package name?** -> `com.hoid.void`, as used throughout this doc.

---

## 12. Documentation

| Document | Covers |
|---|---|
| `docs/design-v0.3.md` | This document. What the launcher is |
| `docs/architecture.md` | Module graph, dependency rules, DI, R8, testing |
| `docs/toolchain.md` | Pinned versions, AGP 9 changes, config traps |
| `docs/roadmap.md` | Milestones and status |
| `Todo.md` | Working task list, decisions table, risks |

All published to GitHub Pages at
`https://cephandriusmaxtori.github.io/void/` by `.github/workflows/pages.yml`.
Authored as Markdown and rendered by Jekyll, so the file edited in a pull
request is the file published — no generated HTML in the repo to drift out of
sync. The site's own stylesheet uses the design tokens from §5 rather than a
stock theme.

---

**Document version:** 0.3
**Last updated:** 2026-10-01
**Previous:** v0.2
