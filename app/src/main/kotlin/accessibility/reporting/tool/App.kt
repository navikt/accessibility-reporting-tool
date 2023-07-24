package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.AzureAuthContext
import accessibility.reporting.tool.authenitcation.installAuthentication
import accessibility.reporting.tool.database.Environment
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.SuccessCriterion
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.lang.IllegalArgumentException

fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

val testOrg =
    OrganizationUnit(id = "carls-awesome-test-unit", name = "Carls awesome test unit", email = "awesome@nav.no")

fun main() {
    val environment = Environment()
    val authContext = AzureAuthContext()
    Flyway.runFlywayMigrations(Environment())
    val repository = ReportRepository(PostgresDatabase(environment)).also { reportRepository ->
        //id som kan brukes når du skal sette opp rapporter: "carls-awesome-test-unit"
        reportRepository.insertOrganizationUnit(testOrg)
    }

    embeddedServer(Netty, port = 8081, module = { this.api(repository) { installAuthentication(authContext) } }).start(
        wait = true
    )
}

fun Application.api(repository: ReportRepository, authInstaller: Application.() -> Unit) {
    authInstaller()

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> {
                    call.respondText(status=HttpStatusCode.BadRequest, text = cause.message?:"Bad request")
                }
                else -> call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
            }
        }
    }

    routing {
        authenticate {
            organizationUnits(repository)
            userRoute(repository)
            reports(repository)
        }
        meta()

        staticResources("/static", "static") {
            default("index.html")
            preCompressed(CompressedFileType.GZIP)
        }
    }
}
