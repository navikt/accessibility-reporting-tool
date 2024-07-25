package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime

fun Route.jsonApiUsers(reportRepository: ReportRepository, organizationRepository: OrganizationRepository) {
    route("users") {
        get("details") {
            val reports = reportRepository.getReportsForUser<ReportListItem>(call.user.oid)
                .sortedBy { it.descriptiveName?.lowercase() ?: it.url }
            val teams = organizationRepository.getOrganizationForUser(call.user.email)
            val userDetails =
                UserDetails(name = call.user.username, email = call.user.email.str(), reports = reports, teams = teams)
            call.respond(userDetails)
        }
    }
}

data class UserDetails(
    val name: String,
    val email: String,
    val reports: List<ReportListItem>,
    val teams: List<OrganizationUnit>
)
