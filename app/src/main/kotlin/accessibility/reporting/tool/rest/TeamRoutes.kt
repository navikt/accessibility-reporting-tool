package accessibility.reporting.tool.rest

import accessibility.reporting.tool.database.ReportRepository
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonapiteams (repository: ReportRepository){
    route("teams"){
        get {
            val teams = repository.getAllOrganizationUnits().map { org -> TeamSummary(id = org.id, name = org.name,email = org.email) }
            call.respond(teams)
        }

    }
}
data class TeamSummary (
    val id: String,
    val name: String,
    val email: String,

)