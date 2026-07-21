plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.deepseekwidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.deepseekwidget"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Jetpack Glance — Compose-based App Widget framework
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // WorkManager — background refresh scheduling
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // DataStore — typed state persistence for Glance
    implementation("androidx.datastore:datastore-core:1.1.1")

    // OkHttp — lightweight HTTP client for DeepSeek API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Kotlinx Serialization — JSON parsing
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

    // Security — EncryptedSharedPreferences for API key storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // AppCompat — basic Activity for config screen
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
