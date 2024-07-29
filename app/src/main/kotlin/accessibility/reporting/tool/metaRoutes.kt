package accessibility.reporting.tool

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

fun Routing.meta(prometehusRegistry: PrometheusMeterRegistry) {

    get("/isalive") {
        call.respond(HttpStatusCode.OK)
    }
    get("/isready") {
        call.respond(HttpStatusCode.OK)
    }
    get("open/metrics") {
        call.respond(prometehusRegistry.scrape())
    }
}