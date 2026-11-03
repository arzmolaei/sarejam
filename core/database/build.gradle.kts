plugins {
    alias(libs.plugins.android.library)
}

android { namespace = "ir.sarejam.core.database" }

dependencies {
    implementation(project(":core:model"))
}
