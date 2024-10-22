package dependencies

abstract class Dependency(
    val version: String,
    val groupId: String
) {
    init {
        require(version != "")
        require(groupId != "")
    }
    fun dependency(artifactId: String, groupidExt: String = "") = "$groupId$groupidExt:$artifactId:$version"
}

object Kotlin {
    const val VERSION = "1.9.23"
}
object Database {
    object Flyway : Dependency(version = "10.17.2", groupId = "org.flywaydb") {

        val core = dependency("flyway-core")
        val postgres = dependency("flyway-database-postgresql")
    }

    const val KOTLIQUERY = "com.github.seratch:kotliquery:1.9.0"
    const val HIKARI = "com.zaxxer:HikariCP:5.1.0"
    const val POSTGRES = "org.postgresql:postgresql:42.7.4"
}

object Jackson : Dependency(version = "2.17.2", groupId = "com.fasterxml.jackson") {

    val core = dependency("jackson-core", ".core")
    val kotlin = dependency("jackson-module-kotlin", ".module")
    val datatypeJsr310 = dependency("jackson-datatype-jsr310", ".datatype")
}

object Logback : Dependency(version = "1.5.7", groupId = "ch.qos.logback") {

    val logbackCore = dependency("logback-core")
    val logbackClassic = dependency("logback-classic")
    const val LOGSTASH_ENCODER = "net.logstash.logback:logstash-logback-encoder:8.0"
}

object Ktor : Dependency("2.3.12", groupId = "io.ktor") {

    val jacksonSerialization = dependency("ktor-serialization-jackson")
    val clientCio = dependency("ktor-client-cio")
    val micrometer = dependency("ktor-server-metrics-micrometer")
    val clientContentNegotiation = dependency("ktor-client-content-negotiation")
    val serverContentNegotiation = dependency("ktor-server-content-negotiation")
    val serverCore = dependency("ktor-server-core")
    val netty = dependency("ktor-server-netty")
    val htmlBuilder = dependency("ktor-server-html-builder")
    val serverAuth = dependency("ktor-server-auth")
    val serverAuthJwt = dependency("ktor-server-auth-jwt")
    val statusPages = dependency("ktor-server-status-pages")
    val serverCors = dependency("ktor-server-cors")
    val serverHostJvm = dependency("ktor-server-host-common-jvm")
    val ktorServerTestHost = dependency("ktor-server-test-host-jvm")

}

object Testdependencies {

    const val MOCKK = "io.mockk:mockk:1.13.12"
    object TestContainers : Dependency(version = "1.20.1", groupId = "org.testcontainers") {
        val containers = dependency("testcontainers")
        val jupiterRunner = dependency("junit-jupiter")
        val postgres = dependency("postgresql")
    }

    object Kotest : Dependency(version = "5.9.1", groupId = "io.kotest") {

        val junit5Runner = dependency("kotest-runner-junit5")
        val assertionsCore = dependency("kotest-assertions-core")
    }

    object Jupiter : Dependency("5.11.0", "org.junit.jupiter") {
        private const val ENGINE_ARTIFACT_ID = "junit-jupiter-engine"

        val engine = dependency(ENGINE_ARTIFACT_ID)
        val api = "$groupId:$ENGINE_ARTIFACT_ID:junit-jupiter-api"
        val params = dependency("junit-jupiter-params")
    }
}
/*
2024-10-22 10:04:10: 30 outdated dependencies
ch.qos.logback:  1.5.7 -> 1.5.11
ch.qos.logback:  1.5.7 -> 1.5.11
com.fasterxml.jackson.core:  2.17.2 -> 2.18.0
com.fasterxml.jackson.datatype:  2.17.2 -> 2.18.0
com.fasterxml.jackson.module:  2.17.2 -> 2.18.0
com.zaxxer:  5.1.0 -> 6.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor:  2.3.12 -> 3.0.0
io.ktor.plugin:  2.3.12 -> 3.0.0
io.mockk:  1.13.12 -> 1.13.13
org.flywaydb:  10.17.2 -> 10.20.0
org.flywaydb:  10.17.2 -> 10.20.0
org.junit.jupiter:  5.11.0 -> 5.11.2
org.junit.jupiter:  5.11.0 -> 5.11.2
org.testcontainers:  1.20.1 -> 1.20.2
org.testcontainers:  1.20.1 -> 1.20.2
org.testcontainers:  1.20.1 -> 1.20.2
**Ignored dependencies
org.jetbrains.kotlin
org.gradle.kotlin.kotlin-dsl
io.micrometer