plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "de.freezzzgames.radarscreenbridge"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.freezzzgames.radarscreenbridge"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }
}