plugins {
    alias(libs.plugins.android.library)
}

android { namespace = "ir.sarejam.data.backup" }

dependencies {
    implementation(project(":core:database"))
}
