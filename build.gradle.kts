import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.spring") version "1.3.72"
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "com.healthmetrix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    // spring
    val springBootVersion = "2.3.0.RELEASE"
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-actuator:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-webflux:$springBootVersion")

    // structured logging
    implementation("net.logstash.logback:logstash-logback-encoder:6.3")

    // serialization
    val jacksonVersion = "2.11.0"
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // swagger
    val springdocVersion = "1.3.9"
    implementation("org.springdoc:springdoc-openapi-ui:$springdocVersion")
    implementation("org.springdoc:springdoc-openapi-kotlin:$springdocVersion")

    // spring data
    implementation("org.springframework.data:spring-data-commons:$springBootVersion")
    implementation("io.github.boostchicken:spring-data-dynamodb:5.2.3")

    // dynamodb
    val awsSdkVersion = "1.11.785"
    implementation(platform("com.amazonaws:aws-java-sdk-bom:$awsSdkVersion"))
    implementation("com.amazonaws:aws-java-sdk-dynamodb:$awsSdkVersion")

    // aws secrets
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-caching-java:1.0.1")

    // google fcm
    implementation("com.google.firebase:firebase-admin:6.13.0")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("com.ninja-squad:springmockk:2.0.1")
    testImplementation("org.assertj:assertj-core:3.16.1")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
        exclude(group = "com.vaadin.external.google", module = "android-json")
        exclude(module = "junit")
        exclude(module = "mockito-core")
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<DependencyUpdatesTask> {
        outputFormatter = closureOf<Result> {
            val sb = StringBuilder()
            outdated.dependencies.forEach { dep ->
                sb.append("${dep.group}:${dep.name} ${dep.version} -> ${dep.available.release ?: dep.available.milestone}\n")
            }
            if (sb.isNotBlank()) rootProject.file("build/dependencyUpdates/outdated-dependencies").apply {
                parentFile.mkdirs()
                println(sb.toString())
                writeText(sb.toString())
            } else println("Up to date!")
        }

        // no alphas, betas, milestones, release candidates
        // or whatever the heck jaxb-api is using
        rejectVersionIf {
            candidate.version.contains("alpha") or
                candidate.version.contains("beta") or
                candidate.version.contains(Regex("M[0-9]*(-.*)?$")) or
                candidate.version.contains("RC", ignoreCase = true) or
                candidate.version.contains(Regex("b[0-9]+\\.[0-9]+$")) or
                candidate.version.contains("eap")
        }
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    setProperty("archiveFileName", "lab-res.jar")
}
