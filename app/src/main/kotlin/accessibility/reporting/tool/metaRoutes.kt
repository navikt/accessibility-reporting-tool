package accessibility.reporting.tool

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.meta() {

    get("/isAlive") {
        call.respond(HttpStatusCode.OK)
    }
    get("/isReady") {
        call.respond(HttpStatusCode.OK)
    }

}