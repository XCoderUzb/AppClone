plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// GitHub Actions har yig'ishda run raqamini beradi: versiya avtomatik oshadi
val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toIntOrNull() ?: 1

android {
    namespace = "uz.appclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "uz.appclone.app"
        minSdk = 26
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    // Barqaror debug kalit: SHA-1 bir xil, yangilanish ustidan o'rnatiladi
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
    implementation("com.google.android.gms:play-services-ads:23.3.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}
