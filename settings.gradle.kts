pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // NOTE: no `dependencies` block here. pluginManagement does not have one in
    // the Kotlin DSL — putting the KGP classpath entry there fails at script
    // compilation. The Kotlin version pin lives in the root buildscript instead.
    // See build.gradle.kts and docs/toolchain.md.
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "void"

include(":app")
include(":core:system")
include(":core:data")
include(":core:icons")
include(":core:designsystem")
include(":feature:home")
include(":feature:drawer")
include(":feature:widgets")
include(":feature:settings")
