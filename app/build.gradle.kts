import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
val unsplashKeyValue: String = localProps.getProperty("UNSPLASH_API_KEY", "")
val pexelsKeyValue: String = localProps.getProperty("PEXELS_API_KEY", "")
val pixabayKeyValue: String = localProps.getProperty("PIXABAY_API_KEY", "")

android {
    namespace = "com.kapoue.agora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kapoue.agora"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val unsplashKey = unsplashKeyValue
        buildConfigField("String", "UNSPLASH_API_KEY", "\"$unsplashKey\"")
        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsKeyValue\"")
        buildConfigField("String", "PIXABAY_API_KEY", "\"$pixabayKeyValue\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Activity
    implementation(libs.activity.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    // DataStore
    implementation(libs.datastore.preferences)

    // Gson
    implementation(libs.gson)

    // Coroutines
    implementation(libs.coroutines.android)
}

kapt {
    correctErrorTypes = true
}
