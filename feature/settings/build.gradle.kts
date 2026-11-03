plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android { namespace = "ir.sarejam.feature.settings" }

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:database"))
    implementation(project(":engine:insight"))
    implementation(libs.androidx.datastore.preferences)
    api(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.junit)
}
