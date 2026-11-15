@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
@Suppress("UnstableApiUsage")
fun DependencyResolutionManagement.configureRepos() {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories.apply {
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement.configureRepos()

rootProject.name = "ObsWifiCams"
include(":app")
