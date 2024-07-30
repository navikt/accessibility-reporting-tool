package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonapiteams(organizationRepository: OrganizationRepository) {
    route("teams") {
        get {
            val teams = organizationRepository.getAllOrganizationUnits()
                .map { org -> TeamSummary(id = org.id, name = org.name, email = org.email) }
            call.respond(teams)
        }

        post("new") {
            val newTeam = call.receive<NewTeam>()
            organizationRepository.upsertOrganizationUnit(OrganizationUnit.createNew(newTeam))
            call.respond(HttpStatusCode.OK)
        }

        route("{id}") {
            get("reports") {
                val teamId = call.parameters["id"] ?: throw BadPathParameterException("id")
                val reports = organizationRepository.getReportForOrganizationUnit<ReportListItem>(teamId).second
                call.respond(reports)
            }
            get("details") {
                val teamId = call.parameters["id"] ?: throw BadPathParameterException("id")
                val teamDetails = organizationRepository.getOrganizationUnit(teamId)?:throw ResourceNotFoundException("team",teamId)
                call.respond(teamDetails)
            }
            put("update"){
                val id = call.parameters["id"] ?: throw BadPathParameterException("id")
                val updates = call.receive<TeamUpdate>()

                val existingTeam = organizationRepository.getOrganizationUnit(id) ?: throw ResourceNotFoundException(type = "Team", id = id)
                var updatedTeam = existingTeam

                updates.name?.let {
                    updatedTeam = updatedTeam.copy(name = it)
                }

                updates.email?.let {
                    updatedTeam = updatedTeam.copy(email = it)
                }

                updates.members?.let {
                    updatedTeam = updatedTeam.copy(members = it)
                }
                val result = organizationRepository.upsertOrganizationUnit(updatedTeam)
                call.respond(HttpStatusCode.OK, result)


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

data class TeamUpdate(
    val id: String,
    val name: String? = null,
    val email: String? = null,
    val members: MutableSet<String>? = null
)
