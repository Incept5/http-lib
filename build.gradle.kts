// For local builds, use 0-SNAPSHOT. For CI builds, use the build number from CircleCI
// If a specific version is provided (e.g., from JitPack), use that instead
val providedVersion = findProperty("version") as? String
val buildNumber = findProperty("buildNumber") as? String

// Set version based on parameters
if (buildNumber != null && buildNumber.isNotEmpty()) {
    // If buildNumber is provided, use it
    version = "1.0.$buildNumber"
    println("Using build number for version: $version")
} else if (providedVersion != null && providedVersion != "unspecified" && providedVersion.isNotEmpty()) {
    // If explicit version is provided, use it
    version = providedVersion
    println("Using provided version: $version")
} else {
    // Default version
    version = "1.0.0-SNAPSHOT"
    println("Using default version: $version")
}

// Always ensure we have a valid group ID
val providedGroup = findProperty("group") as? String
group = if (providedGroup.isNullOrBlank()) "com.github.incept5" else providedGroup

// Log the group and version for debugging
println("Building with group: $group")
println("Building with version: $version")

plugins {
    // because: the dependency analysis plugin uses kotlin
    // see: https://github.com/autonomousapps/dependency-analysis-android-gradle-plugin/wiki/FAQ
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt) apply false

    alias(libs.plugins.test.logger)
    alias(libs.plugins.dependency.analysis)
    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // publish to maven repositories
    `maven-publish`
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Configure Kotlin to target JVM 21
kotlin {
    jvmToolchain(21)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    // Get the publishGroupId from root project
    val publishGroupId = rootProject.properties["publishGroupId"]?.toString() ?: rootProject.group.toString()

    java {
        withJavadocJar()
        withSourcesJar()
    }

    // Configure Kotlin to target JVM 21
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    // Configure publishing for all subprojects
    configure<PublishingExtension> {
        repositories {
            mavenLocal()
        }
    }

    // allow us to see from the cli what tests are executing and their results
    apply(plugin = rootProject.libs.plugins.test.logger.get().pluginId)
    testlogger {
        setTheme("mocha-parallel")
    }
}

dependencyAnalysis {
    issues {
        all {
            onAny {
                severity("fail")
            }
        }
    }
}

// For JitPack compatibility
tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

// Always publish to local Maven repository after build for local development
tasks.named("build") {
    finalizedBy(tasks.named("publishToMavenLocal"))
}