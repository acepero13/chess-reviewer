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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()       // chess-core:1.0.0 published locally
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }   // chesslib
    }
}

rootProject.name = "GameReviewer"
include(":app")
