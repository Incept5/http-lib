plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.quarkus)
}

dependencies {

    api(project(":"))
    api("io.quarkus:quarkus-core")
    api("io.smallrye.config:smallrye-config-core")
    api("jakarta.inject:jakarta.inject-api")
    api("jakarta.ws.rs:jakarta.ws.rs-api")

    implementation(libs.okhttp3.core)
    implementation(enforcedPlatform(libs.quarkus.bom))
    implementation("io.quarkus:quarkus-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    runtimeOnly(libs.kotlin.logging)
    runtimeOnly(libs.quarkus.rest.jackson)
    runtimeOnly("io.quarkus:quarkus-arc")
    runtimeOnly("io.quarkus:quarkus-jdbc-h2")
    runtimeOnly("io.quarkus:quarkus-config-yaml")
    runtimeOnly("io.quarkus:quarkus-flyway")

    testImplementation(libs.quarkus.wiremock.test)
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.eclipse.microprofile.config:microprofile-config-api")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.wiremock:wiremock-standalone")
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}
