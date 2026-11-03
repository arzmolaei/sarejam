plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android { namespace = "ir.sarejam.feature.onboarding" }

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":engine:insight"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
}
