# Void Launcher

An Android home launcher with a monochrome, dot-matrix aesthetic and a strict
black/white/gray palette with a single red accent. Style is original; no
third-party launcher assets, fonts, or branding are used.

Current state: **pre-M1**. Toolchain validation in progress.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 | AGP 9 minimum. Temurin 17.0.20.1 verified. |
| Android SDK | Platform 36 + Build-Tools 36.0.0 | |
| Gradle | via wrapper (9.2.0) | Don't use a system Gradle; the wrapper pins it. |

The SDK location must be discoverable by Gradle. Either export `ANDROID_HOME`,
or create a `local.properties` in the repo root:

```properties
sdk.dir=C:/path/to/Android/Sdk
```

`local.properties` is gitignored — every developer sets their own.

## Build

```bash
# Debug APK
./gradlew assembleDebug          # Windows: .\gradlew.bat assembleDebug

# Release (exercises R8 + resource shrinking)
./gradlew assembleRelease

# Everything CI runs
./gradlew assembleDebug assembleRelease testDebugUnitTest lintDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## Signed release

The signing config reads from environment variables so no keystore or password
is ever committed:

| Variable | Purpose |
|---|---|
| `VOID_KEYSTORE_FILE` | Absolute path to the `.jks` |
| `VOID_KEYSTORE_PASSWORD` | Keystore password |
| `VOID_KEY_ALIAS` | Key alias |
| `VOID_KEY_PASSWORD` | Key password |

If these are absent, `assembleRelease` produces an **unsigned** APK, which is
what CI does for its shrinker check. The `Release` workflow supplies them from
repository secrets and fails loudly if they are missing.

## Module layout

```
app                    LauncherActivity, manifest, manual DI (AppContainer)
core/system            LauncherApps wrapper, package callbacks, widget host
core/data              Room (grid, pages, folders, categories), DataStore (settings)
core/icons             Monochrome icon pipeline + cache
core/designsystem      Theme tokens, typography, haptics
feature/home           Home grid, page pager, drag & drop, dock
feature/drawer         App drawer, search, alphabet scroller
feature/widgets        Widget picker, binding, resize
feature/settings       Grid size, icon style, gestures, Samsung options
benchmark              Macrobenchmark (M8)
```

All modules are declared from day one. Each gains real code only as its
milestone arrives.

## Testing

```bash
./gradlew testDebugUnitTest     # JVM unit tests
./gradlew connectedDebugAndroidTest   # instrumented, needs a device
```

Macrobenchmark needs a real device or a KVM-capable emulator and runs on a
self-hosted runner labelled `benchmark` — see `.github/workflows/benchmark.yml`.

## CI

| Workflow | Trigger | What it does |
|---|---|---|
| `build.yml` | push / PR to `main` | Assemble debug + release, unit tests, lint, upload APKs |
| `release.yml` | tag `v*` | Signed release APK attached to the GitHub release |
| `benchmark.yml` | manual | Macrobenchmark on a self-hosted device runner |

Dependencies and Actions are kept current by Dependabot.

## Documentation

- [`docs/design-v0.3.md`](docs/design-v0.3.md) — the design doc
- [`docs/architecture.md`](docs/architecture.md) — module graph and dependency rules
- [`docs/toolchain.md`](docs/toolchain.md) — pinned versions, compatibility findings
- [`Todo.md`](Todo.md) — what still needs doing

## License

Custom. Note that the bundled **Doto** font is licensed under the SIL Open Font
License and remains OFL regardless of this project's license; the OFL text and
attribution ship with the font.
