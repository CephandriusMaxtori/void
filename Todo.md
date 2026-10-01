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
| Package | `namespace` **`com.hoid.voidlauncher`**, `applicationId` **`com.hoid.void`** | `void` is a Java keyword, so it cannot be a source package. The applicationId keeps the branding. See §Risks |
| Dot font | **Doto (OFL)** | Confirmed on Google Fonts, fetchable |

---

## Phase 0 — Toolchain smoke test — **DONE, all 3 passed**

The stack holds. No fallback to AGP 8.13.1 needed. Full results and the
`buildscript` recipe are in [`docs/toolchain.md`](docs/toolchain.md).

- [x] Scaffold throwaway AGP 9.0.1 + Kotlin module
- [x] Add Room 2.8.5 via KSP 2.3.12 — one `@Entity`, one `@Dao`, one `@Database`
- [x] `assembleDebug` succeeds
- [x] **KSP works with AGP 9 built-in Kotlin** — `kspDebugKotlin` ran *and* Room
      emitted `schemas/.../1.json`, which only happens if the processor actually
      executed rather than being silently skipped
- [x] **KGP pin mechanism found** — `pluginManagement` has no `dependencies`
      block in the Kotlin DSL; the classpath entry goes in the root
      `buildscript`. Confirmed resolving to KGP **2.3.20**, not AGP's bundled 2.2.10
- [x] **`jvmTarget` alignment holds** — 17/17, proven on the *release* variant
      since a mismatch only surfaces as a D8 error at link time. R8, resource
      shrinking, and `lintVitalRelease` all clean
- [x] Findings recorded in `docs/toolchain.md`
- [x] Throwaway project cleaned up

### Also fixed: CI file mode

`gradlew` was committed as mode 100644, so the Linux runner died with
`./gradlew: Permission denied` / exit 126. Root cause: `core.fileMode` is
`false` on Windows, so the exec bit is never recorded.

- [x] `git update-index --chmod=+x gradlew` (now 100755)
- [x] Defensive `chmod +x gradlew` step in `build.yml` and `release.yml`
- [x] Documented in `docs/toolchain.md`

---

## Phase 1 — Skeleton — **DONE, builds**

`:app:assembleDebug` produces a working APK. All nine modules declared and
building.

- [x] Gradle wrapper at 9.2.0
- [x] `settings.gradle.kts` — 9 modules, `FAIL_ON_PROJECT_REPOS`
- [x] `build.gradle.kts` — root, with the KGP `buildscript` classpath pin
- [x] `gradle/libs.versions.toml` — single source of truth for all versions
- [x] `gradle.properties` — jvmargs, parallel, caching, configuration cache
- [x] `local.properties` — gitignored
- [x] 9 module build files + `consumer-rules.pro`
- [x] `AndroidManifest.xml` — HOME + DEFAULT + LAUNCHER filters, `singleTask`,
      `excludeFromRecents`, `stateNotNeeded="true"`, broad `configChanges`,
      `clearTaskOnLaunch`, `windowSoftInputMode`
- [x] **Launcher-safe theme** — `Theme.DeviceDefault.NoActionBar`, not AppCompat
      or Material3. The system instantiates the home activity outside any
      AppCompat delegate, and an AppCompat theme there crashes on cold boot
- [x] `QUERY_ALL_PACKAGES` with a documented `tools:ignore`, plus `READ_CONTACTS`
      declared but not requested
- [x] Launcher icon with a real `monochrome` layer
- [x] `proguard-rules.pro` — keeps for the launcher surface R8 would otherwise
      strip silently
- [x] Verify `:app:assembleDebug` builds
- [ ] Baseline Profile module scaffold (M8, but declare early)

### Version pins settled during Phase 1

| | |
|---|---|
| Compose BOM | **`2026.06.01`** (Compose 1.11.4), not the newest `2026.09.00` |
| `namespace` | `com.hoid.voidlauncher` |
| `applicationId` | `com.hoid.void` |

The BOM is a **three-way lock with AGP and compileSdk**. Compose 1.12.x
(BOM `2026.08.00`+ / Compose `1.12.0`+) needs compileSdk 37 *and* AGP 9.1.0+.
Bumping the BOM alone produces 22 near-identical `checkDebugAarMetadata` errors.
If AGP is ever upgraded past 9.0.x, revisit the BOM in the same change.

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
- [x] Design doc v0.3 — amendments below
- [x] `docs/architecture.md` — module graph, dependency rules
- [x] `README.md` — build instructions, prerequisites
- [x] `docs/toolchain.md` — Phase 0 findings, version compatibility notes
- [x] **`LICENSE`** — Apache 2.0, `Copyright 2026 Nolan Bragan`. The custom
      no-redistribution licence was replaced because it contradicted the public
      repo; Apache 2.0 also carries the express patent grant it lacked
- [x] **`NOTICE`** — Doto attributed under OFL, separately licensed from the
      project; Apache-2.0 deps listed
- [ ] Per-milestone notes in `docs/milestones/`

### Repo & CI

- [x] GitHub repo created: `CephandriusMaxtori/void` (public, `main`)
- [x] `build.yml` — assemble debug + release, unit tests, lint, upload APKs
- [x] `release.yml` — signed release on `v*` tag
- [x] `benchmark.yml` — manual Macrobenchmark on a self-hosted runner
- [x] `pages.yml` — Jekyll docs site
- [x] `dependabot.yml` — Actions + Gradle, grouped
- [x] `gradlew` mode 100755 + defensive `chmod` step
- [ ] Repository secrets for signed releases:
      `VOID_KEYSTORE_B64`, `VOID_KEYSTORE_PASSWORD`, `VOID_KEY_ALIAS`,
      `VOID_KEY_PASSWORD`
- [ ] Confirm CI goes green on the first real push

### Docs site

- [x] Jekyll, with `docs/` as the root so Android sources are structurally
      excluded rather than filtered
- [x] `Gemfile` with pinned Jekyll for reproducibility
- [x] `assets/css/style.css` using the app's own tokens
- [x] `index.md` landing page, `roadmap.md`
- [x] Pages enabled with `build_type: workflow`
- [x] `baseurl: "/void"` — it is a **project** site, served from a subpath
- [ ] Confirm the first deploy succeeds

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
| KSP incompatible with AGP 9 built-in Kotlin | Phase 0 smoke test | **closed — works** |
| KGP pin mechanism unknown under built-in Kotlin | Phase 0 smoke test | **closed — root `buildscript`** |
| `jvmTarget` mismatch (Java 11 default in AGP 9) | Phase 0 smoke test | **closed — 17/17 holds** |
| `gradlew` not executable on Linux CI | `--chmod=+x` + defensive `chmod` step | **closed** |
| R8 stripping the launcher surface (`LauncherApps.Callback`, `AppWidgetProvider`, `AppWidgetHost`) which the system reflects over across process restarts | Keep rules before M6; release build already runs in CI | open |
| Many apps lack monochrome layers | ColorMatrix fallback + per-app override list | planned |
| Drag-and-drop jank in Compose | Hoist drag state, overlay shadow, test early (M3) | planned |
| Widget hosting edge cases | Isolate in `core:system`, crash guards, build after grid is stable (M6) | planned |
| One UI edge gestures vs pager | Horizontal insets / gesture exclusion, test on device | planned |
| Trademark / asset concerns | Original assets only, Doto (OFL) | mitigated |
| OEM variance (Samsung gestures, shade) | Feature-detect + graceful degradation + settings toggles | planned |
| Custom license lacks patent grant | Revisit before public release | open |
