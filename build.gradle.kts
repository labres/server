import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    application
    kotlin("jvm") version "1.3.71"
    kotlin("kapt") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.github.ben-manes.versions") version "0.28.0"
}

group = "com.healthmetrix"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-core:1.3.2")
    implementation("io.ktor:ktor-server-netty:1.3.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    // DI
    implementation("com.google.dagger:dagger:2.26")
    kapt( "com.google.dagger:dagger-compiler:2.26")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClassName
            )
        )
    }

    archiveFileName.set("lab-res.jar")
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
