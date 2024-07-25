package accessibility.reporting.tool

import accessibility.reporting.tool.microfrontends.NavBarItem.FORSIDE
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import accessibility.reporting.tool.wcag.ReportShortSummary
import accessibility.reporting.tool.wcag.ReportType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.html.*

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