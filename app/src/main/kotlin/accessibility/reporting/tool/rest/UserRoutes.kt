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
import java.time.format.DateTimeFormatter

fun Route.jsonApiUsers(repository: ReportRepository, repo: OrganizationRepository) {
    route("users") {
        get("details") {

            val reports = repository.getReportsForUser<ReportListItem>(call.user.oid)
                .sortedBy { it.descriptiveName?.lowercase() ?: it.url }
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
    val reports: List<ReportListItem>,
    val teams: List<OrganizationUnit>
)

class ReportListItem(
    @JsonProperty("id")
    override val reportId: String,
    @JsonProperty("title")
    override val descriptiveName: String?,
    @JsonIgnore
    override val url: String,
    val teamId: String,
    lastChanged: LocalDateTime
) : ReportContent {
    val date: String = "yyyy-MM-dd".datestr(lastChanged)

    companion object {

        fun fromJson(jsonNode: JsonNode) = ReportListItem(
            reportId = jsonNode.reportId,
            descriptiveName = jsonNode.descriptiveName,
            url = jsonNode.url,
            teamId = jsonNode.orgnaizationUnit()?.id ?: "",
            lastChanged = jsonNode.reportLastChanged()
        )

    }
}
