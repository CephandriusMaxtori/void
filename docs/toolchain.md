# Toolchain

Pinned versions, why they were chosen, and the compatibility findings that
matter. Anything here that is marked **unverified** is a known unknown scheduled
for the Phase 0 smoke test.

## Pinned versions

| Component | Version | Source |
|---|---|---|
| AGP | 9.0.1 | Latest stable at decision time was 9.4.1; 9.0.1 chosen because it is already in the local Gradle cache, making the first build fast |
| Gradle | 9.2.0 | AGP 9.0 requires 9.1+; 9.2.0 is cached locally |
| Kotlin (KGP) | 2.3.20 | Cached locally |
| JDK | 17 (Temurin 17.0.20.1) | AGP 9 minimum |
| compileSdk / targetSdk | 36 | AGP 9.0's maximum supported API level is 36.1 |
| minSdk | 33 | Monochrome icon layers are API 33+ |
| Compose BOM | 2026.09.00 | |
| Compose compiler plugin | tied to KGP 2.3.20 | `org.jetbrains.kotlin.plugin.compose` |
| Room | 2.8.5 | Latest stable |
| KSP | 2.3.12 | Latest stable; 2.3.1+ required for AGP 9 built-in Kotlin |
| DataStore | 1.2.1 | Latest stable |
| Coroutines | 1.11.0 | |
| Font | Doto (OFL) | Confirmed available on Google Fonts |
| DI | manual | No framework, no annotation processor |

## Local environment (verified)

| Thing | Value |
|---|---|
| JDK | Temurin 17.0.20.1 |
| SDK location | `C:\Users\melis\AppData\Local\Android\Sdk` |
| `ANDROID_HOME` / `ANDROID_SDK_ROOT` | **Both unset** — hence `local.properties` is required |
| Platforms installed | 30, 33, 34, 35, 36, 36.1, 37.0 |
| Build-tools installed | 34.0.0, 35.0.0, 36.0.0, 36.1.0, 37.0.0 |
| Cached in `~/.gradle` | AGP 9.0.1, Kotlin 2.3.20, several Gradle distributions |
| Not cached | Room, KSP — both download on first build |
| Network | Google Maven and Maven Central both reachable |

## AGP 9 notes

AGP 9.0 shipped January 2026 and carries a reputation for breakage that is
mostly *stale*. At launch, KSP and Hilt genuinely did not support its built-in
Kotlin. That was fixed:

- KSP gained AGP 9 / built-in Kotlin support in **2.3.1**
- Dagger/Hilt gained AGP 9 support in **2.59**

So the "AGP 9 is a disaster" consensus reflects Oct 2025 – Jan 2026, not
October 2026.

Behaviour changes that affect this project:

- **Built-in Kotlin.** Do not apply `org.jetbrains.kotlin.android`; AGP provides
  it. Applying both is an error.
- **Legacy variant API is gone.** `applicationVariants` and friends are
  removed. Use `androidComponents`. Not an issue here — this project has no
  custom build logic.
- **Java defaults moved from 8 to 11.** Both `kotlin.compilerOptions.jvmTarget`
  and `compileOptions` are pinned explicitly to 17. See below.
- **R8 defaults are stricter.** `proguard-android.txt` is no longer available;
  only `proguard-android-optimize.txt`. Keep rules must be complete. This
  matters for a launcher: `LauncherApps.Callback`, `AppWidgetProvider`, and
  `AppWidgetHost` all get reflected over by the system across process restarts.
- **`kapt` is incompatible.** Use KSP. This project uses KSP for Room anyway.
- `targetSdk` now defaults to `compileSdk` rather than `minSdk`. Pinned
  explicitly regardless.

## Open risks

### KSP + AGP 9 built-in Kotlin — **unverified**

This is the load-bearing assumption. JetBrains' compatibility matrix states KSP
is no longer version-tied to the Kotlin compiler as of 2.3.0, and that AGP 9
support landed in 2.3.1. KSP 2.3.12 with KGP 2.3.20 is *expected* to work, but
the decoupling is recent enough that it should be proven before nine modules
depend on it.

**Verified by:** the Phase 0 smoke test in `Todo.md`.
**Fallback if it fails:** AGP 8.13.1 + Gradle 8.14, both fully cached. Cost is a
version bump.

### How to pin the KGP version — **unverified**

AGP 9 has a *runtime dependency* on KGP 2.2.10 (its bundled minimum). Raising it
is documented as adding a `kotlin-gradle-plugin` classpath entry to the
buildscript. The idiomatic form for a version-catalog project has not been
confirmed, and getting it wrong produces a confusing version-mismatch error
rather than a clear one.

**Verified by:** the Phase 0 smoke test.

### jvmTarget alignment

AGP 9 moved Java source/target defaults to 11. Kotlin's `jvmTarget` must match
`compileOptions`, and both are pinned to 17. A mismatch produces a D8 error at
link time, not a compile error, so it is easy to miss until the release build.

**Verified by:** the Phase 0 smoke test, which builds a release variant.

## CI

| Workflow | Purpose |
|---|---|
| `build.yml` | assemble debug + release, unit tests, lint, upload APKs |
| `release.yml` | signed APK on a `v*` tag |
| `benchmark.yml` | Macrobenchmark, manual dispatch, self-hosted device runner |

The release build is part of ordinary CI on purpose: R8 and resource shrinking
only run there, so it is the only thing that catches shrinker breakage before a
device does.

GitHub-hosted runners export `ANDROID_HOME`, so the gitignored
`local.properties` is not needed in CI. It *is* needed locally.

Macrobenchmark needs a real device or a KVM-capable emulator. GitHub's hosted
runners do not provide a usable one, so `benchmark.yml` targets a self-hosted
runner labelled `benchmark`.

## Action versions

Verified against the GitHub API on 2026-10-01:

| Action | Version |
|---|---|
| `actions/checkout` | v7 |
| `actions/setup-java` | v6 |
| `actions/upload-artifact` | v7 |
| `actions/download-artifact` | v8 |
| `gradle/actions` | v6 |
