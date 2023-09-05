package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.AzureAuthContext
import accessibility.reporting.tool.authenitcation.installAuthentication
import accessibility.reporting.tool.database.Environment
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import mu.KotlinLogging
import java.lang.IllegalArgumentException

fun main() {
    val environment = Environment()
    val authContext = AzureAuthContext()
    Flyway.runFlywayMigrations(Environment())
    val repository = ReportRepository(PostgresDatabase(environment))
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt()?:8081, module = { this.api(repository) { installAuthentication(authContext) } }).start(
        wait = true
    )
}


fun Application.api(repository: ReportRepository, authInstaller: Application.() -> Unit) {
    val log = KotlinLogging.logger {  }
    authInstaller()

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
        }
        meta()
        staticResources("/static", "static") {
            preCompressed(CompressedFileType.GZIP)
        }
    }
}