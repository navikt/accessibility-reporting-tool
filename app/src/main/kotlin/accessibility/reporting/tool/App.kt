package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.AzureAuthContext
import accessibility.reporting.tool.authenitcation.installAuthentication
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.html.*
import accessibility.reporting.tool.microfrontends.faqRoute
import accessibility.reporting.tool.rest.RequestException
import accessibility.reporting.tool.rest.jsonApiReports
import accessibility.reporting.tool.rest.jsonapiteams
import accessibility.reporting.tool.rest.jsonApiUsers
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
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
    val organizationRepository = OrganizationRepository(PostgresDatabase(environment))
    embeddedServer(
        Netty,
        port = System.getenv("PORT")?.toInt() ?: 8081,
        module = {
            this.api(
                corsAllowedOrigins = environment.corsAllowedOrigin,
                reportRepository = repository,
                organizationRepository = organizationRepository,
            ) { installAuthentication(authContext) }
        }).start(
        wait = true
    )
}


fun Application.api(
    corsAllowedOrigins: List<String>,
    corsAllowedSchemes: List<String> = listOf("https"),
    reportRepository: ReportRepository,
    organizationRepository: OrganizationRepository,
    authInstaller: Application.() -> Unit
) {
    val prometehusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val log = KotlinLogging.logger { }
    authInstaller()
    install(MicrometerMetrics) {
        registry = prometehusRegistry
    }
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }
    install(CORS) {
        corsAllowedOrigins.forEach { allowedHost ->
            allowHost(host = allowedHost, schemes = corsAllowedSchemes)
        }
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowCredentials = true
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> {
                    log.debug { "Feil i request fra bruker: ${cause.message}" }
                    call.respondText(status = HttpStatusCode.BadRequest, text = cause.message ?: "Bad request")
                }

                is BadRequestException -> {
                    log.debug(cause.message)
                    call.respondText(status = HttpStatusCode.BadRequest, text = cause.message ?: "Bad request")
                }

                is RequestException -> {
                    log.error(cause) { cause.message }
                    call.respondText(status = cause.responseStatus, text = cause.message!!)
                }

                else -> {
                    log.error(cause) { "Feil i request fra bruker: ${cause.message}" }
                    log.error { "Ukjent feil: ${cause.message}" }
                    log.error { cause.stackTrace.contentToString() }
                    call.respondText(text = "500: ${cause.message}", status = HttpStatusCode.InternalServerError)

                }
            }
        }
    }
    routing {
        authenticate {
            organizationUnits(organizationRepository =organizationRepository)
            userRoute(reportRepository)
            reports(reportRepository =reportRepository, organizationRepository =organizationRepository)
            landingPage(reportRepository)
            adminRoutes(reportRepository =reportRepository, organizationRepository =organizationRepository)
            faqRoute()
            route("api") {
                jsonApiReports(organizationRepository = organizationRepository, reportRepository = reportRepository)
                jsonapiteams(organizationRepository = organizationRepository)
                jsonApiUsers(organizationRepository = organizationRepository, reportRepository = reportRepository)
            }
        }
        meta(prometehusRegistry)
        openReportRoute(reportRepository)
        staticResources("/static", "static") {
            preCompressed(CompressedFileType.GZIP)
        }
        //openAPI(path="openapi", swaggerFile = "openapi/documentation.yaml") {
        //}
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
    val corsAllowedOrigin: List<String> = System.getenv("CORS_ALLOWED_ORIGIN").split(",")

) {
    val dbUrl: String = if (dbHost.endsWith(":$dbPort")) {
        "jdbc:postgresql://${dbHost}/$dbName"
    } else {
        "jdbc:postgresql://${dbHost}:${dbPort}/${dbName}"
    }
}