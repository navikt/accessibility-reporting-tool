package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.rest.Admin.isAdmin
import accessibility.reporting.tool.wcag.AggregatedReport
import accessibility.reporting.tool.wcag.Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonapiadmin(reportRepository: ReportRepository, organizationRepository: OrganizationRepository) {
    route("admin") {
        install(AdminCheck)
        delete("teams/{id}") {
            organizationRepository.deleteOrgUnit(call.parameters["id"] ?: throw BadPathParameterException("id"))
            call.respond(HttpStatusCode.OK)
        }
        route("reports/aggregated") {
            route("new") {
                install(AdminCheck)
                post {
                    val newReportRequest = call.receive<NewAggregatedReportRequest>()
                    val newReport = AggregatedReport(
                        url=newReportRequest.url,
                        descriptiveName = newReportRequest.title,
                        organizationUnit = null,
                        reports =reportRepository.getReports<Report>(ids = newReportRequest.reports),
                        user = call.user,
                        notes = newReportRequest.notes,
                    ).let {
                        reportRepository.upsertReportReturning<AggregatedReport>(it)
                    }
                    call.respond(HttpStatusCode.Created, """{ "id": "${newReport.reportId}" }""".trimIndent())
                }
            }
            route("{id}") {
                install(AdminCheck)
                patch {
                    call.respond(HttpStatusCode.NotImplemented)
                }
                delete {
                    call.respond(HttpStatusCode.NotImplemented)
                }
            }
        }
    }
}

val AdminCheck = createRouteScopedPlugin("adminCheck") {
    on(AuthenticationChecked) { call ->
        val user = call.user
        if (!isAdmin(user))
            throw NotAdminUserException(route = call.request.uri, userName = user.username)
    }
}

object Admin {
    private val adminAzureGroup = System.getenv("ADMIN_GROUP")
    fun isAdmin(user: User) = user.groups.contains(adminAzureGroup)
}

class NewAggregatedReportRequest(
    val title: String,
    val url: String,
    val reports: List<String>,
    val notes: String
)