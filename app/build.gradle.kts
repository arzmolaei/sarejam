plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ir.sarejam.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "ir.sarejam.app" // never change
        minSdk = 26
        targetSdk = 37
        versionCode = (System.getenv("SAREJAM_VERSION_CODE") ?: "1").toInt()
        versionName = System.getenv("SAREJAM_VERSION_NAME") ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing comes only from GitHub Secrets (CI) or a local keystore.properties that is
    // never committed. Without them a release build is signed with the debug key, which is only
    // good for testing on the emulator — the tag workflow refuses to publish such a build.
    val keystoreFile = System.getenv("SAREJAM_KEYSTORE_FILE")
    signingConfigs {
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("SAREJAM_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SAREJAM_KEY_ALIAS")
                keyPassword = System.getenv("SAREJAM_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":capture"))
    implementation(project(":data:backup"))
    implementation(project(":feature:today"))
    implementation(project(":feature:finance"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.compose.ui.test.manifest)
}
