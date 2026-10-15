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
        versionCode = 2
        versionName = "1.0.0"
    }

    sourceSets {
        getByName("main") {
            // Bundle the standalone game HTML from the repository root as an Android WebView asset.
            assets.srcDir(rootProject.projectDir)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}