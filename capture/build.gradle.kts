plugins {
    alias(libs.plugins.android.library)
}

android { namespace = "ir.sarejam.capture" }

dependencies {
    implementation(project(":engine:parser"))
    implementation(project(":engine:insight"))
    implementation(project(":core:database"))
}
