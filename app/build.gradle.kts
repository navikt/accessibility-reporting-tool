import dependencies.*
import dependencies.Database.Flyway
import dependencies.Testdependencies.Jupiter
import dependencies.Testdependencies.Kotest
import dependencies.Testdependencies.TestContainers

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

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
    implementation(Flyway.core)
    implementation (Flyway.postgres)
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

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("accessibility.reporting.tool.AppKt")
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
