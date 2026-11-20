plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.appclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.appclone.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2.0"
    }

    // Barqaror debug kalit: SHA-1 har yig'ishda bir xil bo'ladi (Google OAuth uchun kerak)
    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/appclone-debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
}
