// AGP 9 has a *runtime dependency* on KGP 2.2.10 (its bundled minimum). To use a
// different Kotlin version, declare it here so Gradle's conflict resolution
// selects the higher one.
//
// This must be a `buildscript` block. It cannot go in `pluginManagement`, which
// has no `dependencies` block in the Kotlin DSL — putting it there fails at
// script compilation with a receiver type mismatch that looks like a broken
// toolchain. Verified in the Phase 0 smoke test; see docs/toolchain.md.
buildscript {
    dependencies {
        // Literal, not a `libs` accessor: the version catalog is not available
        // yet inside `buildscript {}`. Keep in sync with gradle/libs.versions.toml.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
