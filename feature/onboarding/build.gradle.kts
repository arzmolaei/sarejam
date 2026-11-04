plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android { namespace = "ir.sarejam.feature.onboarding" }

dependencies {
    implementation(project(":core:ui"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
}
