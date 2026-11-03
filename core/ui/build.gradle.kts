plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android { namespace = "ir.sarejam.core.ui" }

dependencies {
    api(project(":core:common"))
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.ui.tooling.preview)
}
