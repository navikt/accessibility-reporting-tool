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
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotliquery.queryOf
import mu.KotlinLogging
import java.lang.IllegalArgumentException

private val log = KotlinLogging.logger {  }

fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

val testOrg =
    OrganizationUnit(id = "carls-awesome-test-unit", name = "Carls awesome test unit", email = "awesome@nav.no")

fun main() {
    val environment = Environment()
    val authContext = AzureAuthContext()
    Flyway.runFlywayMigrations(Environment())
    val repository = ReportRepository(PostgresDatabase(environment))
    log.info { "Sånn her kan du logge" }
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
                    call.respondText(status = HttpStatusCode.BadRequest, text = cause.message ?: "Bad request")
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
            landingPage(repository)
        }
        meta()

        staticResources("/static", "static") {
            preCompressed(CompressedFileType.GZIP)
        }
    }
}

fun Route.landingPage(repository: ReportRepository) {
    get {
        val reports = repository.getReports()
        call.respondHtml {
            head {
                headContent("a11y reporting tool")
            }

            body {
                h1 { +"a11y reporting tool for NAV" }
                a {
                    href="user"
                    +"See your reports"
                }
                ul {
                    reports.forEach { report ->
                        li {
                            a {
                                href = "reports/${report.reportId}"
                                +"Report for ${report.url}"
                            }
                        }
                    }
                }
            }
        }
    }
}
