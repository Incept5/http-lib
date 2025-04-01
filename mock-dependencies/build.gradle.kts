plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "com.github.incept5"
version = "1.0.6"

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.incept5"
            artifactId = "json-core"
            version = "1.0.6"
            
            from(components["java"])
        }
        
        create<MavenPublication>("correlation") {
            groupId = "com.github.incept5"
            artifactId = "correlation"
            version = "1.0.18"
            
            from(components["java"])
        }
        
        create<MavenPublication>("error") {
            groupId = "com.github.incept5.error-lib"
            artifactId = "error-core"
            version = "1.0.20"
            
            from(components["java"])
        }
    }
    
    repositories {
        mavenLocal()
    }
}