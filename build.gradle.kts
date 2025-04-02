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
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // publish to maven repositories
    `maven-publish`
}
dependencies {
    api(libs.slf4j.api)

    // Incept5 libraries
    implementation(libs.incept5.correlation)
    implementation(libs.incept5.json.core)
    api(libs.incept5.error.core)

    // jakarta
    implementation(platform(libs.jakarta.bom))

    // jackson
    implementation(platform(libs.jackson.bom))
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.16.1")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")

    // okhttp
    api(libs.okhttp3.core)
    implementation(libs.okhttp3.logging.interceptor)

    // unit testing
    testImplementation(platform(libs.kotest.bom))
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-assertions-shared")
    testImplementation("io.kotest:kotest-framework-api")
    testImplementation(libs.mockk.jvm)
    testImplementation(libs.mockk.dsl)
    testRuntimeOnly("io.kotest:kotest-runner-junit5-jvm")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    // needed for testing XML redaction
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    // test web interactions
    testImplementation(libs.mockwebserver)
    testImplementation("com.squareup.okio:okio:3.6.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest("1.9.22") // Using a fixed version instead of trying to access from libs
        }
    }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Explicitly set groupId, artifactId, and version
            groupId = project.group.toString()
            artifactId = "http-lib"
            version = project.version.toString()

            from(components["java"])

            // POM information is automatically included with sources and javadoc
            pom {
                name.set("JSON Library")
                description.set("A library for connecting to REST services")
                url.set("https://github.com/incept5/http-lib")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("incept5")
                        name.set("Incept5")
                        email.set("info@incept5.com")
                    }
                }
            }
        }
    }

    // Configure local Maven repository for local builds
    repositories {
        mavenLocal()
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