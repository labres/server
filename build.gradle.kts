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

    // spring data
    implementation("org.springframework.data:spring-data-commons:2.2.6.RELEASE")
    implementation("io.github.boostchicken:spring-data-dynamodb:5.2.3")

    // dynamodb
    implementation(platform("com.amazonaws:aws-java-sdk-bom:1.11.228"))
    implementation("com.amazonaws:aws-java-sdk-dynamodb:1.11.759")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<DependencyUpdatesTask> {
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
