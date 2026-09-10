plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val gitCommitHash: String = try {
    ProcessBuilder("git", "rev-parse", "--short=7", "HEAD")
        .directory(project.rootDir)
        .start()
        .inputStream
        .bufferedReader()
        .readText()
        .trim()
        .ifEmpty { "819690c" }
} catch (e: Exception) {
    "819690c"
}

android {
    namespace = "anhiutangerine.prettiembee"
    compileSdk = 34

    defaultConfig {
        applicationId = "anhiutangerine.prettiembee"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "GIT_HASH", "\"$gitCommitHash\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // LibSu - John Wu's Root Shell library
    implementation(libs.libsu.core)
    implementation(libs.libsu.io)

    // Serialization & JSON
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Coil for Compose image preview
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.ui.tooling)
}
