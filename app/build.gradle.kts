import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

android {
    namespace = "com.softlabs.clipbq"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.softlabs.clipbq"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        val envProperties = Properties()
        val envFile = project.rootProject.file(".env")
        if (envFile.exists()) {
            envProperties.load(FileInputStream(envFile))
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // Load the environment file manually
        val envProperties = Properties()
        val envFile = project.rootProject.file(".env")
        if (envFile.exists()) {
            envFile.inputStream().use { envProperties.load(it) }
        }

        val finalUrl = envProperties.getProperty("SUPABASE_URL")
            ?: System.getenv("SUPABASE_URL")
            ?: "https://placeholder-fallback.supabase.co"

        val finalKey = envProperties.getProperty("SUPABASE_ANON_KEY")
            ?: System.getenv("SUPABASE_ANON_KEY")
            ?: "placeholder-key"

        getByName("debug") {
            // Force debug builds to see the upper-case variables
            buildConfigField("String", "SUPABASE_URL", "\"$finalUrl\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"$finalKey\"")
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // Force release builds to see the upper-case variables
            buildConfigField("String", "SUPABASE_URL", "\"$finalUrl\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"$finalKey\"")
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
}

dependencies {
    // AndroidX & Core Layouts
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose Components (Locked to a stable BOM tree)
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.0")

    // 1. Install the Supabase BOM (Version 3.0.0 tree)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))

    // 2. Import required modules without typing specific versions manually
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // FIX: Swapped from compose-auth-kt to the verified module naming format
    implementation("io.github.jan-tennert.supabase:compose-auth")

    // Ktor Network Engine Configurations (Ktor 3.x stack)
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Lifecycle & Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3:1.3.1")

    // Testing Foundations
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}