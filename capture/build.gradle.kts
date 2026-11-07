plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ir.sarejam.capture"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":engine:parser"))
    implementation(project(":engine:insight"))
    implementation(project(":core:database"))
}
