plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.speechtotext"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.speechtotext"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // ML Kit Translation
    implementation("com.google.mlkit:translate:17.0.1")
    // Standard Android Speech Recognition is built-in,
// but ML Kit requires the Google Play Services
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}