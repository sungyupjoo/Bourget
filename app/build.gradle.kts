plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.bourget"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bourget"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing. The keystore and passwords never live in this repository: they come from
    // ~/.gradle/gradle.properties or environment variables (BOURGET_RELEASE_STORE_FILE,
    // BOURGET_RELEASE_STORE_PASSWORD, BOURGET_RELEASE_KEY_ALIAS, BOURGET_RELEASE_KEY_PASSWORD).
    // Without them, release builds are left unsigned.
    fun signingValue(name: String): String? =
        providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull
    val releaseStoreFile = signingValue("BOURGET_RELEASE_STORE_FILE")
    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = signingValue("BOURGET_RELEASE_STORE_PASSWORD")
                keyAlias = signingValue("BOURGET_RELEASE_KEY_ALIAS")
                keyPassword = signingValue("BOURGET_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    // The language can be changed in the app, so every language must be installed: Play must
    // not split them out of the app bundle by the device's language.
    bundle {
        language {
            enableSplit = false
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
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}