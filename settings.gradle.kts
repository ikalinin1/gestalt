rootProject.name = "gestalt"
include("gestalt-core", "gestalt-hocon", "gestalt-json", "gestalt-git", "gestalt-kotlin", "gestalt-s3",
    "gestalt-sample", "gestalt-yaml")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        jcenter()
    }
}
include("gestalt-git")
