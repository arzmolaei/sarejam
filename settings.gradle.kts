pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "sarejam"

include(":app")
include(":core:model", ":core:common", ":core:database", ":core:ui")
include(":engine:parser", ":engine:insight")
include(":capture")
include(":data:backup")
include(":feature:today", ":feature:finance", ":feature:settings", ":feature:onboarding")
// feature:work and feature:life stay out until v1.0.0 (CLAUDE.md rule 11).
