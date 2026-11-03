plugins {
    id("com.android.application") version "8.2.2"
    id("org.jetbrains.kotlin.android") version "1.9.22"
}

android {
    namespace = "com.aryavarta.partysync"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aryavarta.partysync"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0-Aryavarta"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".party"
            isDebuggable = true
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
    // Pure Android SDK implementation for zero dependency conflicts & ultra-fast APK build
}
