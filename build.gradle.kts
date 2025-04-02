plugins {
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // publish jars to nexus
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

    // needed for testing XML redaction
    testImplementation("jakarta.xml.bind:jakarta.xml.bind-api")
    testRuntimeOnly("org.glassfish.jaxb:jaxb-runtime:4.0.5")

    // test web interactions
    testImplementation(libs.mockwebserver)
    testImplementation("com.squareup.okio:okio:3.6.0")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

tasks.test {
    useJUnitPlatform()
}

// Sources jar is already configured by the java plugin in the root project
// No need to register a separate sourcesJar task

// Jenkins builds will inject the build number into the build, otherwise we default to 0-SNAPSHOT
val buildNumber = findProperty("buildNumber") as? String ?: "0-SNAPSHOT"
group = "org.incept5"
version = "1.0.$buildNumber"

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
                name.set("Http Library")
                description.set("A library for Http client communication with external rest APIs")
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