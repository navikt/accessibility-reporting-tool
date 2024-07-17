package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.data

fun Route.jsonapiteams(repository: ReportRepository) {
    route("teams") {
        get {
            val teams = repository.getAllOrganizationUnits()
                .map { org -> TeamSummary(id = org.id, name = org.name, email = org.email) }
            call.respond(teams)
        }

        post("new") {
            val newTeam = call.receive<NewTeam>()
            println(call.user)
            repository.upsertOrganizationUnit(OrganizationUnit.createNew(newTeam))
            call.respond(HttpStatusCode.OK)
        }

        route("{id}") {
            get("reports") {
                val teamId = call.parameters["id"] ?: throw BadPathParameterException("id")
                val reports = repository.getReportForOrganizationUnit<ReportListItem>(teamId).second
                call.respond(reports)
            }
            get("details") {
                val teamId = call.parameters["id"] ?: throw BadPathParameterException("id")
                val teamDetails = repository.getOrganizationUnit(teamId)?:throw ResourceNotFoundException("team",teamId)
                call.respond(teamDetails)
            }
        }
    }
}

data class NewTeam(
    val name: String,
    val email: String,
    val members: List<String> = emptyList()
)

data class TeamSummary(
    val id: String,
    val name: String,
    val email: String,
)