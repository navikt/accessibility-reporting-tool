package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.id
import accessibility.reporting.tool.rest.Admin.isAdmin
import accessibility.reporting.tool.wcag.*
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
                    val sourceReports = reportRepository.getReports<Report>(ids = newReportRequest.reports)
                    when {
                        sourceReports.isEmpty() -> throw BadAggregatedReportRequestException("Could not find reports with ids ${newReportRequest.reports}")
                        sourceReports.size != newReportRequest.reports.size -> throw BadAggregatedReportRequestException(
                            "Could not find reports with ids ${newReportRequest.diff(sourceReports)}"
                        )

                        sourceReports.any { it.reportType == ReportType.AGGREGATED } -> throw BadAggregatedReportRequestException(
                            "report with ids ${sourceReports.aggregatedReports()} are aggregated reports and are not valid sources for a new aggregated report"
                        )
                    }

                    val newReport = AggregatedReport(
                        url = newReportRequest.url,
                        descriptiveName = newReportRequest.title,
                        organizationUnit = null,
                        reports = reportRepository.getReports<Report>(ids = newReportRequest.reports),
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
                    val updateReportRequest = call.receive<AggregatedReportUpdateRequest>()
                    if (updateReportRequest.successCriteria!=null) call.respond(HttpStatusCode.NotImplemented)
                    val id = call.id
                    val originalReport = reportRepository.getReport<AggregatedReport>(id)
                    val updatedReport = originalReport?.withUpdatedMetadata(
                        title = updateReportRequest.descriptiveName,
                        pageUrl = updateReportRequest.url,
                        notes = updateReportRequest.notes,
                        updateBy = call.user,
                    ) ?: throw ResourceNotFoundException(type = "Aggregated Report", id = id)

                    println(updatedReport)
                    println(originalReport)
                    val debug = reportRepository.upsertReportReturning(updatedReport)
                    println(updateReportRequest)
                    println(debug)
                    call.respond(HttpStatusCode.OK)
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
) {
    fun diff(foundReports: List<ReportContent>): String {
        val foundIds = foundReports.map { it.reportId }
        return reports.filterNot { foundIds.contains(it) }.joinToString(",")
    }
}

data class AggregatedReportUpdateRequest(
    val descriptiveName: String? = null,
    val url: String? = null,
    val successCriteria: List<SuccessCriterionUpdate>? = null,
    val notes: String? = null
)


private fun List<Report>.aggregatedReports() = filter { it.reportType == ReportType.AGGREGATED }