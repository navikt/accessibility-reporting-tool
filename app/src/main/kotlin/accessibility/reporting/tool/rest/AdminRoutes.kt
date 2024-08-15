package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.rest.Admin.isAdmin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.jsonapiadmin() {
    route("admin") {
        install(AdminCheck)
        route("reports") {
            get {
                call.respond(HttpStatusCode.OK)
            }
            route("aggregated") {

                get("new") {
                    call.respond(HttpStatusCode.OK)
                }
                post("new") {
                    call.respond(HttpStatusCode.Created)
                }
                route("{id}") {

                    get {
                        call.respond(HttpStatusCode.OK)
                    }

                    patch {
                        call.respond(HttpStatusCode.OK)
                    }

                    delete {
                    }
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
    fun isAdmin(user: User) = user.groups.contains(System.getenv("ADMIN_GROUP"))
}