package accessibility.reporting.tool

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun main() {
        embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

fun Application.api(){
    routing {
        get("/isAlive"){
            call.respond(HttpStatusCode.OK)
        }
        get("/isReady"){
            call.respond(HttpStatusCode.OK)
        }
    }
}
