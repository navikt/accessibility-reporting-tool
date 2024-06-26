package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.AzureAuthContext
import accessibility.reporting.tool.authenitcation.installAuthentication
import accessibility.reporting.tool.database.Environment
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.faqRoute
import accessibility.reporting.tool.rest.reports
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
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
        module = { this.api(repository) { installAuthentication(authContext) } }).start(
        wait = true
    )
}


fun Application.api(repository: ReportRepository, authInstaller: Application.() -> Unit) {
    val prometehusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val log = KotlinLogging.logger { }
    authInstaller()
    install(MicrometerMetrics) {
        registry = prometehusRegistry
    }
    install(ContentNegotiation){
        jackson()
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
            route("api"){
                reports(repository)
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
