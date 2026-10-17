plugins {
    // Kotlin itself is compiled by AGP's built-in Kotlin support (AGP 9+),
    // so there is no kotlin-android plugin and no Kotlin version here.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.sluer.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sluer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.0"
    }

    signingConfigs {
        // Configured only when the keystore is provided: CI decodes it from
        // the KEYSTORE_BASE64 secret and exports KEYSTORE_FILE plus the
        // credentials. Local builds without those env vars simply skip it.
        val keystorePath = System.getenv("KEYSTORE_FILE")
        if (keystorePath != null) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfigs.findByName("release")?.let { signingConfig = it }
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

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.activity:activity-compose:1.13.0")

    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
