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

rootProject.name = "FrameCutUI"

include(
    ":app",
    ":framecut-ui-core",
    ":framecut-ui-theme",
    ":framecut-ui-assets",
    ":framecut-ui-components",
    ":framecut-ui-screens",
    ":framecut-ui-definitions"
)
