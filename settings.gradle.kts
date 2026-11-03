pluginManagement { repositories { gradlePluginPortal(); mavenCentral() } }
dependencyResolutionManagement { repositories { maven("https://plugins.gradle.org/m2/"); mavenCentral() } }
rootProject.name = "sarejam"
include(":engine:parser")
