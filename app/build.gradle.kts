import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import dependencies.*
import dependencies.Database.Flyway
import dependencies.Testdependencies.Jupiter
import dependencies.Testdependencies.Kotest
import dependencies.Testdependencies.TestContainers

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    `kotlin-dsl`
    id("io.ktor.plugin") version "2.3.12"

}

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

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
    mavenLocal()
    maven("jitpack.io")
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
    implementation(Ktor.clientContentNegotiation)
    implementation(Ktor.ktorOpenApi)
    implementation(Flyway.core)
    implementation(Flyway.postgres)
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
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.2")


    testImplementation(Jupiter.engine)
    testImplementation(Jupiter.api)
    testImplementation(Ktor.ktorServerTestHost)
    testImplementation(TestContainers.containers)
    testImplementation(TestContainers.jupiterRunner)
    testImplementation(TestContainers.postgres)
    testImplementation(Kotest.junit5Runner)
    testImplementation(Kotest.assertionsCore)
    testImplementation(Testdependencies.MOCKK)


}

application {
    // Define the main class for the application.
    mainClass.set("accessibility.reporting.tool.AppKt")
}


tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
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

tasks.withType<ShadowJar> { isZip64 = true }

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}