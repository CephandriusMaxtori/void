# Void Launcher — Todo

Living task list. Updated as work progresses. Checked items stay for history.

Legend: `[ ]` todo · `[~]` in progress · `[x]` done · `[!]` blocked

---

## Locked decisions

Settled during planning. Do not revisit without explicit instruction.

| Decision | Choice | Rationale |
|---|---|---|
| `minSdk` | **33** | Monochrome layers are API 33+; pre-33 users get muddy grayscale fallbacks that undercut the core identity |
| AGP / Gradle | **9.0.1 / 9.2.0** | Both cached locally. KSP 2.3.1+ and Hilt 2.59+ support AGP 9 built-in Kotlin |
| compileSdk / targetSdk | **36 / 36** | |
| DI | **Manual** (`AppContainer` + constructor injection) | No annotation processing for DI; KSP carries only Room |
| Modules | **All 9 declared, lean content** | Architecture stays honest, per-milestone build stays fast |
| Home pages | **Multiple pages in v1** | Pages entity + pager are foundational from M1, not an additive later change |
| Contacts permission | **Request on first launch** | §7 must be rewritten; use a rationale screen before the system dialog |
| Weather (M5) | **Clock + user-configured provider** | Void never holds the network permission; no data leaves device unprompted |
| License | **Custom** | Doto stays OFL regardless; consider adding an express patent grant |
| Dot-matrix v1 | **Doto rendered as a normal font** | ~90% of the look for ~5% of the work; Canvas dot-grid deferred to weather readout |
| Package | `com.hoid.void` | Per §3 |
| Dot font | **Doto (OFL)** | Confirmed on Google Fonts, fetchable |

---

## Phase 0 — Toolchain smoke test

The whole stack depends on one unverified thing: KSP working with AGP 9's
built-in Kotlin. Do this before writing 9 modules against it.

Throwaway single-module project (temp dir, not this repo).

- [ ] Scaffold throwaway AGP 9.0.1 + Kotlin module
- [ ] Add Room 2.8.5 via KSP 2.3.12 — one `@Entity`, one `@Dao`, one `@Database`
- [ ] `assembleDebug` succeeds
- [ ] **Verify** the idiomatic way to pin KGP 2.3.20 under built-in Kotlin
      (docs describe a buildscript classpath entry; version-catalog form untested)
- [ ] **Verify** `jvmTarget` alignment — AGP 9 moved Java defaults to 11,
      so both `kotlin.compilerOptions.jvmTarget` and `compileOptions` need 17
- [ ] Record findings, then delete the throwaway project

**Fallback if KSP fails:** drop to AGP 8.13.1 + Gradle 8.14 (also fully cached).
Cost is a version bump, nothing else.

- [ ] Commit smoke-test findings as a note in `docs/`

---

## Phase 1 — Skeleton

- [ ] Gradle wrapper at 9.2.0
- [ ] `settings.gradle.kts` — 9 modules, `pluginManagement` with KGP + KSP classpath
- [ ] `build.gradle.kts` — root, shared config only
- [ ] `gradle/libs.versions.toml` — single source of truth for all versions
- [ ] `gradle.properties` — jvmargs, caching, parallel
- [ ] `local.properties` — done, gitignored
- [ ] 9 module build files + manifests
- [ ] `AndroidManifest.xml` — HOME + DEFAULT filters, `singleTask`,
      `excludeFromRecents`, `stateNotNeeded="true"`, broad `configChanges`,
      `windowSoftInputMode`
- [ ] Verify `:app:assembleDebug` builds empty
- [ ] Baseline Profile module scaffold (M8, but declare early)

---

## Phase 2 — M1: Shell + drawer

- [ ] `LauncherActivity` + root scaffold, edge-to-edge, predictive back
- [ ] `core:system` — `LauncherApps` wrapper, `LauncherApps.Callback`, shortcuts
- [ ] `core:data` — `AppEntry`, Room entities, repository
- [ ] `core:designsystem` — tokens, typography, haptics
- [ ] `feature:home` — multi-page pager, 4x6 grid per page + dock row,
      horizontal insets for One UI edge-back
- [ ] Pages entity: add / remove / reorder, page cap, page indicator
- [ ] `feature:drawer` — flat app list + app search (categories wait for M4)
- [ ] Set as default home, verify on device

---

## M2–M8

- [ ] **M2** `core:icons` — monochrome pipeline, LRU + disk cache,
      off-main-thread, `component + versionCode + themeVersion` key
- [ ] **M2** Theme wiring, Doto typography, haptics
- [ ] **M3** Home grid persistence, drag & drop with cell snapping, folder
      creation on drop, hideable dock
- [ ] **M4** Auto-categorization, category tabs, smart search
- [ ] **M4** Contacts search + first-launch rationale screen + `READ_CONTACTS`
- [ ] **M5** Dot-matrix clock, user-configured weather provider
- [ ] **M6** Widget hosting — bind, configure, resize; crash guards in `core:system`
- [ ] **M7** Folders, gestures, settings, Samsung/One UI options
- [ ] **M8** Macrobenchmark (startup, drawer scroll, drag), baseline profile,
      accessibility pass

---

## Documentation

- [x] `.gitignore`
- [x] `local.properties` (gitignored)
- [x] `Todo.md` (this file)
- [ ] Design doc v0.3 — amendments below
- [ ] `docs/architecture.md` — module graph, dependency rules
- [ ] `README.md` — build instructions, prerequisites
- [ ] `docs/toolchain.md` — Phase 0 findings, version compatibility notes
- [ ] Per-milestone notes in `docs/milestones/`

---

## Design doc v0.3 amendments

Changes required before the doc matches the code.

- [ ] `minSdk` 29 -> 33 (§ header, §3)
- [ ] §3 — DI = manual `AppContainer`, not a framework
- [ ] §3 — multi-page home promoted to v1
- [ ] §4.2 — pages entity detail, page cap, page reorder
- [ ] §4.4 — Doto-as-font for v1; Canvas dot-grid deferred
- [ ] §4.7 — **remove swipe-down notification shade**
      (`expandNotificationsPanel()` is a no-op for third-party launchers).
      Swipe *up* stays: as the HOME app we own that gesture.
- [ ] §4.7 — document One UI edge-back contention on horizontal pager
- [ ] §5 — unchanged (tokens are fine)
- [ ] §7 — rewrite permissions stance: no permissions for core home/drawer;
      `READ_CONTACTS` requested at first launch; no network permission in v1
- [ ] §7 — add `stateNotNeeded`, `configChanges` to manifest notes
- [ ] §8 — M5 = clock + user-configured provider, not built-in weather
- [ ] §10 — resolve all five open questions
- [ ] Add — KSP/KGP version decoupling as an explicit risk + smoke test
- [ ] Add — license section (custom; Doto remains OFL)

---

## Risks

| Risk | Mitigation | Status |
|---|---|---|
| KSP incompatible with AGP 9 built-in Kotlin | Phase 0 smoke test | open |
| KGP pin mechanism unknown under built-in Kotlin | Phase 0 smoke test | open |
| `jvmTarget` mismatch (Java 11 default in AGP 9) | Phase 0 smoke test | open |
| Many apps lack monochrome layers | ColorMatrix fallback + per-app override list | planned |
| Drag-and-drop jank in Compose | Hoist drag state, overlay shadow, test early (M3) | planned |
| Widget hosting edge cases | Isolate in `core:system`, crash guards, build after grid is stable (M6) | planned |
| One UI edge gestures vs pager | Horizontal insets / gesture exclusion, test on device | planned |
| Trademark / asset concerns | Original assets only, Doto (OFL) | mitigated |
| OEM variance (Samsung gestures, shade) | Feature-detect + graceful degradation + settings toggles | planned |
| Custom license lacks patent grant | Revisit before public release | open |
