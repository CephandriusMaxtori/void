plugins {
    alias(libs.plugins.android.application)
    // AGP 9 provides Kotlin built in. Do NOT add org.jetbrains.kotlin.android.
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    // `void` is a Java keyword, so it cannot appear in a source package name.
    // The namespace is therefore `com.hoid.voidlauncher` while the
    // applicationId below stays `com.hoid.void`, which is what the Play listing
    // and the installed package are identified by. The two are deliberately
    // different; see docs/toolchain.md.
    namespace = "com.hoid.voidlauncher"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hoid.void"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Populated only when the VOID_* environment variables are present, which
        // is what the Release workflow supplies and what CI deliberately does
        // not. Without them `assembleRelease` produces an unsigned APK, which is
        // all the CI shrinker check needs.
        create("release") {
            val storePath = providers.environmentVariable("VOID_KEYSTORE_FILE").orNull
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = providers.environmentVariable("VOID_KEYSTORE_PASSWORD").orNull
                keyAlias = providers.environmentVariable("VOID_KEY_ALIAS").orNull
                keyPassword = providers.environmentVariable("VOID_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (providers.environmentVariable("VOID_KEYSTORE_FILE").isPresent) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = true
        checkDependencies = true
        // local.properties is gitignored and per-machine, but lint still walks
        // the project directory and applies generic property-file rules to it.
        // PropertyEscape demands escaping the drive-letter colon in the SDK
        // path, which is noise about a file nobody else will ever see — and it
        // would only ever fire for a Windows developer, never in CI.
        disable += "PropertyEscape"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    // Exported schemas make Room migrations reviewable in code review rather
    // than discovered at runtime. See docs/architecture.md.
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.datastore.preferences)

    // Modules. Direction matters: app may see everything, nothing sees app.
    // See the dependency rules in docs/architecture.md.
    implementation(project(":core:system"))
    implementation(project(":core:data"))
    implementation(project(":core:icons"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:home"))
    implementation(project(":feature:drawer"))
    implementation(project(":feature:widgets"))
    implementation(project(":feature:settings"))

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.compose.ui.tooling)
}
