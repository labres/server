import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.3.71"
    kotlin("plugin.spring") version "1.3.71"
    id("org.springframework.boot") version "2.2.6.RELEASE"
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
    implementation("org.springframework.boot:spring-boot-starter-web:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-actuator:2.2.6.RELEASE")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.10.3")
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.2.6.RELEASE")

    // swagger
    implementation("io.springfox:springfox-swagger2:2.9.2")
    implementation("io.springfox:springfox-swagger-ui:2.9.2")

    // spring data
    implementation("org.springframework.data:spring-data-commons:2.2.6.RELEASE")
    implementation("io.github.boostchicken:spring-data-dynamodb:5.2.3")

    // dynamodb
    implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.760"))
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.11.760")

    // testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.1")
    testImplementation("com.ninja-squad:springmockk:2.0.0")
    testImplementation("org.assertj:assertj-core:3.15.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:2.2.6.RELEASE") {
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
