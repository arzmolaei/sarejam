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
include(
    ":app",
    ":core:model",
    ":core:common",
    ":core:database",
    ":core:ui",
    ":engine:parser",
    ":engine:insight",
    ":capture",
    ":data:backup",
    ":feature:today",
    ":feature:finance",
    ":feature:work",
    ":feature:life",
    ":feature:settings",
    ":feature:onboarding",
)
