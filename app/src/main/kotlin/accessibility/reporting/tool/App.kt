package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.AzureAuthContext
import accessibility.reporting.tool.authenitcation.installAuthentication
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.faqRoute
import accessibility.reporting.tool.rest.jsonApiReports
import accessibility.reporting.tool.rest.jsonapiteams
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import mu.KotlinLogging
import java.lang.IllegalArgumentException


fun main() {
    val environment = Environment()
    val authContext = AzureAuthContext()
    Flyway.runFlywayMigrations(Environment())
    val repository = ReportRepository(PostgresDatabase(environment))
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8081,
        module = {
            this.api(
                corsAllowedOrigins = environment.corsAllowedOrigin,
                repository = repository
            ) { installAuthentication(authContext) }
        }).start(
        wait = true
    )
}


fun Application.api(
    corsAllowedOrigins: String,
    corsAllowedSchemes: List<String> = listOf("https"),
    repository: ReportRepository,
    authInstaller: Application.() -> Unit
) {
    val prometehusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val log = KotlinLogging.logger { }
    authInstaller()
    install(MicrometerMetrics) {
        registry = prometehusRegistry
    }
    install(ContentNegotiation) {
        jackson()
    }
    install(CORS) {
        allowHost(host = corsAllowedOrigins, schemes = corsAllowedSchemes)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> {
                    log.debug { "Feil i request fra bruker: ${cause.message}" }
                    call.respondText(status = HttpStatusCode.BadRequest, text = cause.message ?: "Bad request")
                }

                else -> {
                    log.error { "Ukjent feil: ${cause.message}" }
                    log.error { cause.stackTrace.contentToString() }
                    call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)

                }
            }
        }
    }

    routing {
        authenticate {
            organizationUnits(repository)
            userRoute(repository)
            reports(repository)
            landingPage(repository)
            adminRoutes(repository)
            faqRoute()
            route("api") {
                jsonApiReports(repository)
                jsonapiteams(repository)
            }
        }
        meta(prometehusRegistry)
        openReportRoute(repository)
        staticResources("/static", "static") {
            preCompressed(CompressedFileType.GZIP)
        }
    }

    allRoutes(plugin(Routing))
        .filter { it.selector is HttpMethodRouteSelector }
        .forEach { println("route: $it") }
}


fun allRoutes(root: Route): List<Route> {
    return listOf(root) + root.children.flatMap { allRoutes(it) }
}

class Environment(
    dbHost: String = System.getenv("DB_HOST"),
    dbPort: String = System.getenv("DB_PORT"),
    dbName: String = System.getenv("DB_DATABASE"),
    val dbUser: String = System.getenv("DB_USERNAME"),
    val dbPassword: String = System.getenv("DB_PASSWORD"),
    val corsAllowedOrigin: String = System.getenv("CORS_ALLOWED_ORIGIN")

) {
    val dbUrl: String = if (dbHost.endsWith(":$dbPort")) {
        "jdbc:postgresql://${dbHost}/$dbName"
    } else {
        "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
    }
}
