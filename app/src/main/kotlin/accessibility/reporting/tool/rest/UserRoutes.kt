package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.rest.ReportInfo.Companion.toReportInfo
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun Route.jsonApiUsers(repository: ReportRepository, repo: OrganizationRepository) {
    route("users") {
        get("details") {

            val reports = repository.getReportsForUser<Report>(call.user.oid)
                .sortedBy { it.descriptiveName?.lowercase() ?: it.url }
                .map { it.toReportInfo() }
            val teams = repo.getOrganizationForUser(call.user.email)
            val userDetails =
                UserDetails(name = call.user.username, email = call.user.email.str(), reports = reports, teams = teams)
            call.respond(userDetails)
        }
    }
}

data class UserDetails(
    val name: String,
    val email: String,
    val reports: List<ReportInfo>,
    val teams: List<OrganizationUnit>
)

data class ReportInfo(val title: String, val id: String, val teamId: String, val date: String) {
    companion object {
        fun Report.toReportInfo() = ReportInfo(
            title = descriptiveName ?: this.url,
            id = reportId,
            teamId = organizationUnit?.id ?: "",
            date = "yyyy-MM-dd".datestr(lastChanged)
        )
    }
}

private fun String.datestr(date: LocalDateTime) = let {
    val formatter = DateTimeFormatter.ofPattern(this)
    date.format(formatter)
}
