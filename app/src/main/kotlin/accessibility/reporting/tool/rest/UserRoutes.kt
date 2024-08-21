package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonApiUsers(reportRepository: ReportRepository, organizationRepository: OrganizationRepository) {
    //TODO remove
    route("users") {
        get("details") {
            val reports = reportRepository.getReportsForUser<ReportListItem>(call.user.oid)
                .sortedBy { it.descriptiveName?.lowercase() ?: it.url }
            val teams = organizationRepository.getOrganizationForUser(call.user.email)
            val userDetails =
                UserDetails(call.user, reports = reports, teams = teams)
            call.respond(userDetails)
        }
    }

    get("user") {
        val reports = reportRepository.getReportsForUser<ReportListItem>(call.user.oid)
            .sortedBy { it.descriptiveName?.lowercase() ?: it.url }
        val teams = organizationRepository.getOrganizationForUser(call.user.email)
        val userDetails =
            UserDetails(call.user, reports = reports, teams = teams)
        call.respond(userDetails)
    }
}

class UserDetails(
    user: User,
    val reports: List<ReportListItem>,
    val teams: List<OrganizationUnit>
) {
    val name = user.name
    val email = user.email
    val isAdmin = Admin.isAdmin(user)
}

