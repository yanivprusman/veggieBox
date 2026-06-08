import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("android-flavors")
}

// Version derived from git commit count so each APK records which commit it came from.
val gitCommitCount = providers.exec {
    commandLine("git", "rev-list", "--count", "HEAD")
}.standardOutput.asText.get().trim().toIntOrNull() ?: 1

val gitShortHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.get().trim().ifEmpty { "dev" }

// Local, gitignored build config (android/.env). Holds the backend base URL so it
// stays out of the public repo and can differ per worker/deployment. Baked into the
// APK at build time.
val envFile = rootProject.file(".env")
val envProps = Properties()
if (envFile.exists()) envFile.inputStream().use { envProps.load(it) }
val isProd = envProps.getProperty("IS_PROD", "false").toBoolean()
val apiBaseUrl = envProps.getProperty("API_BASE_URL", "https://veggiebox.prod.ya-niv.com/")

android {
    namespace = "com.automatelinux.veggieBox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.automatelinux.veggieBox"
        minSdk = 26
        targetSdk = 35
        versionCode = gitCommitCount
        versionName = "v${gitCommitCount} (${gitShortHash})"

        buildConfigField("boolean", "IS_PROD", isProd.toString())
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
}

dependencies {
    implementation(project(":shared"))
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines (Retrofit suspend functions + viewModelScope)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Map (OpenStreetMap via osmdroid — free, no API key, matches PT app)
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Image loading (delivery photos)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Core
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)

    // Feedback lib
    implementation(project(":feedback-lib"))
}
