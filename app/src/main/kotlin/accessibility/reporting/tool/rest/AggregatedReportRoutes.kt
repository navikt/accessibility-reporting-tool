package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.annotation.JsonFormat
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Route.jsonApiAggregatedReports(reportRepository: ReportRepository) {

    route("reports/aggregated") {
        get {
            call.respond(reportRepository.getReports<ReportListItem>(ReportType.AGGREGATED))
        }
        get("{id}") {
            val id = call.parameters["id"] ?: throw BadRequestException("Missing id")
            val report = reportRepository.getReport<AggregatedReport>(id)
                ?: throw ResourceNotFoundException("report", id)
            call.respond(AggregatedReportWithAccessPolicy(report,call.user))
        }
    }
}

class AggregatedReportWithAccessPolicy(
    report: AggregatedReport,
    user: User
) : ReportContent {
    override val reportId: String = report.reportId
    override val descriptiveName: String = report.descriptiveName ?: report.url
    override val url: String = report.url
    val team: OrganizationUnit? = report.organizationUnit
    val author: Author = report.author
    val successCriteria: List<SuccessCriterion> = report.successCriteria

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val created: LocalDateTime = report.created

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val lastChanged: LocalDateTime = report.lastChanged
    val hasWriteAccess: Boolean = report.writeAccess(user)
    val lastUpdatedBy: String = report.lastUpdatedBy?.email ?: author.email
    val notes: String = report.notes
    val fromTeams = report.fromOrganizations.toSet()
    val fromReports = report.fromReports
}