rootProject.name = "http-lib"
include("http-core")
include("test-http-quarkus")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal() // Add mavenLocal first to find our mock dependencies
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
