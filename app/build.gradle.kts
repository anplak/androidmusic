plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.anplak.androidmusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.anplak.androidmusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    lint {
        // Fail the build on any lint error or warning
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        // Dependency/version churn is intentional pin noise — track via Dependabot/renovate, not baseline.
        disable +=
            setOf(
                "GradleDependency",
                "NewerVersionAvailable",
                "AndroidGradlePluginVersion",
                // Adaptive icons stay in mipmap-anydpi-v26 (required for linking); minSdk is already 26.
                "ObsoleteSdkInt",
            )
        // Single shared baseline under config/linters/ (repo root)
        baseline = rootProject.file("config/linters/lint-baseline.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Media3 for playback, session, and notifications
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Room for local database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coil for MediaStore album art
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    // Instrumentation test dependencies (androidTest)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        // Pin ktlint engine version for consistency
        ktlint("1.2.1")
        trimTrailingWhitespace()
        endWithNewline()
        indentWithSpaces(4)
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint("1.2.1")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    ignoreFailures = false
    baseline = rootProject.file("config/linters/detekt-baseline.xml")
    config.setFrom(rootProject.files("config/linters/detekt.yml"))
}

// Configure Detekt report formats (HTML for local viewing)
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(false)
        txt.required.set(false)
        xml.required.set(false)
    }
}

// Ensure static analysis runs as part of the verification lifecycle
tasks.named("check").configure {
    dependsOn("spotlessCheck", "detekt", "lint")
}
