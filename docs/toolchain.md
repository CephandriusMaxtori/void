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

## Phase 0 smoke test — results

Run against a throwaway single-module project. **All three tests passed; the
pinned stack is confirmed. No fallback to AGP 8.13.1 needed.**

| # | Question | Result |
|---|---|---|
| 1 | How do you raise the KGP version under built-in Kotlin? | **`buildscript { dependencies { classpath(...) } }` in the root `build.gradle.kts`** |
| 2 | Does KSP 2.3.12 work with AGP 9's built-in Kotlin? | **Yes.** `kspDebugKotlin` ran, Room emitted a schema |
| 3 | Does the release build hold with 17/17 `jvmTarget`? | **Yes.** R8, resource shrinking, and `lintVital` all clean |

### Test 1 — pinning the Kotlin version

`pluginManagement` has **no `dependencies` block** in the Kotlin DSL. Putting the
classpath entry there fails at script compilation with a receiver type mismatch,
which reads like a broken toolchain rather than a config mistake.

The working form goes in the root `build.gradle.kts`:

```kotlin
buildscript {
    dependencies {
        // AGP 9 has a *runtime dependency* on KGP 2.2.10. Declaring a higher
        // version here makes Gradle's conflict resolution select it.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}

plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
}
```

Confirmed via `buildEnvironment`, which reports
`org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20` — genuinely 2.3.20, not AGP's
bundled 2.2.10.

### Test 2 — KSP with built-in Kotlin

Works. Two independent signals, because "the task did not fail" is weaker
evidence than it looks:

- `kspDebugKotlin` executed
- Room wrote `schemas/com.hoid.smoke.data.VoidDatabase/1.json`, which only
  happens if the annotation processor actually ran and emitted output

So the KSP 2.3.x / KGP version decoupling is real, and KSP 2.3.12 + KGP 2.3.20 is
a valid combination. `org.jetbrains.kotlin.android` is correctly **not** applied.

### Test 3 — release build and `jvmTarget`

`assembleRelease` with `isMinifyEnabled` and `isShrinkResources` both true
completed clean, including `minifyReleaseWithR8`,
`convertShrunkResourcesToBinaryRelease`, and `lintVitalRelease`. A `jvmTarget`
mismatch surfaces at link time as a D8 error, so this had to be proven on the
release variant, not just the debug one. It holds at 17/17.

Room needed no extra keep rules — it ships its own consumer rules. The real R8
risk in this project is not Room but the launcher surface (`LauncherApps.Callback`,
`AppWidgetProvider`, `AppWidgetHost`), which the system reflects over across
process restarts. Not yet exercisable; stays a watch item for M6.

## Windows and CI: file modes

`gradlew` was initially committed with mode **100644** and CI failed with:

```
/home/runner/work/_temp/....sh: line 1: ./gradlew: Permission denied
##[error]Process completed with exit code 126.
```

Cause: `core.fileMode` is `false` on Windows, so the POSIX exec bit is never
recorded when committing from Windows. The Linux runner checks the file out
without `+x`.

Fix, committed:

```
git update-index --chmod=+x gradlew
```

Both workflows also run `chmod +x gradlew` after checkout, because
`core.fileMode=false` means the bit can be lost again on a future re-commit, and
exit code 126 is an unpleasant thing to debug from a log.

`.gitattributes` separately forces LF on `gradlew`. A CRLF checkout fails
differently and just as confusingly: `/bin/sh^M: bad interpreter`.

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
