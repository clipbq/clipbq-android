import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
}

android {
    namespace = "com.softlabs.clipbq"
    compileSdk {
        version = release(37)
    }

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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }



    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

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
}