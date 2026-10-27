plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// google-services.json comes from the GOOGLE_SERVICES_JSON secret in CI (or a local copy).
// If it is absent the app still builds; FCM is simply not configured at runtime.
val hasGoogleServices = file("google-services.json").exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("app/google-services.json not found — building WITHOUT Firebase configuration")
}

fun env(name: String, default: String): String =
    (System.getenv(name) ?: project.findProperty(name) as String?)?.takeIf { it.isNotBlank() } ?: default

android {
    namespace = "com.example.wakealarm"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.wakealarm"
        minSdk = 30
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"

        // Backend settings baked into the APK (set WAKE_BACKEND_URL / WAKE_KEY as CI secrets).
        buildConfigField("String", "BACKEND_URL", "\"${env("WAKE_BACKEND_URL", "")}\"")
        buildConfigField("String", "WAKE_KEY", "\"${env("WAKE_KEY", "")}\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation("androidx.compose.material:material-icons-core:1.7.5")
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")
}
