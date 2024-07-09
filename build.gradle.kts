import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import dependencies.*

buildscript {
    repositories {
        // Use 'gradle install' to install latest
        mavenLocal()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.github.ben-manes:gradle-versions-plugin:+")
    }
}
apply(plugin = "com.github.ben-manes.versions")

plugins {
    kotlin("jvm").version("1.8.10")
    `kotlin-dsl`
    `maven-publish`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Ktor.jacksonSerialization)
    implementation(Ktor.clientCio)
    implementation(Ktor.micrometer)
    implementation(Ktor.clientCio)
    implementation(Ktor.serverCore)
    implementation(Ktor.serverHostJvm)
    implementation(Ktor.netty)
    implementation(Ktor.htmlBuilder)
    implementation(Ktor.serverAuth)
    implementation(Ktor.serverAuthJwt)
    implementation(Ktor.statusPages)
    implementation(Ktor.serverCors)
    implementation(Ktor.serverContentNegotiation)
    implementation(Database.Flyway.core)
    implementation (Database.Flyway.postgres)
    implementation(Database.HIKARI)
    implementation(Database.POSTGRES)
    implementation(Database.KOTLIQUERY)
    implementation(Jackson.core)
    implementation(Jackson.kotlin)
    implementation(Jackson.datatypeJsr310)
    implementation(Logback.logbackClassic)
    implementation(Logback.logbackCore)
    implementation(Logback.LOGSTASH_ENCODER)
    implementation("io.github.microutils:kotlin-logging:3.0.5")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css:1.0.0-pre.597")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.1")


    testImplementation(Testdependencies.Jupiter.engine)
    testImplementation(Testdependencies.Jupiter.api)
    testImplementation(Ktor.ktorServerTestHost)
    testImplementation(Testdependencies.TestContainers.containers)
    testImplementation(Testdependencies.TestContainers.jupiterRunner)
    testImplementation(Testdependencies.TestContainers.postgres)
    testImplementation(Testdependencies.Kotest.junit5Runner)
    testImplementation(Testdependencies.Kotest.assertionsCore)
    testImplementation(Testdependencies.MOCKK)


}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates").configure {

    // optional parameters
    checkForGradleUpdate = true
    outputFormatter = "json"
    outputDir = "build/dependencyUpdates"
    reportfileName = "dependencies"

    rejectVersionIf {
        isNonStable(candidate.version)
    }
}
/*
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}*/
