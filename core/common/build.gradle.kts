plugins {
    alias(libs.plugins.android.library)
}

android { namespace = "ir.sarejam.core.common" }

dependencies {
    implementation(project(":core:model"))
}
