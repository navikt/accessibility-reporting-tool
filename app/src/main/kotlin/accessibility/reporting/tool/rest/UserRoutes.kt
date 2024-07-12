package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonApiUsers(repository: ReportRepository, repo: OrganizationRepository) {
    route("users") {
    get("details") {

        val reports = repository.getReportsForUser(call.user.oid)
            .sortedBy { it.descriptiveName?.lowercase()?:it.url }
        val teams = repo.getOrganizationForUser(call.user.email)
        val userDetails = UserDetails(email = call.user.email.str(), reports = reports, teams = teams)
        call.respond(userDetails)
    }}
}
data class UserDetails(
    val email: String,
    val reports: List<Report>,
    val teams: List<String>
)

