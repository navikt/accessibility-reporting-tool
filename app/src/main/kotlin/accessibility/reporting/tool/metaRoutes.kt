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
import io.micrometer.prometheus.PrometheusMeterRegistry
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

fun Route.landingPage(repository: ReportRepository) {
    get {
        val reports = repository.getReports<ReportShortSummary>().sortedBy { it.title.lowercase() }
        call.respondHtmlContent("a11y rapportering", FORSIDE) {
            img {
                id = "uu-katt"
                src = "/static/UU-katt.svg"
                alt = "A11y cat loves you!"
                title = "A11y cat loves you!"
            }

            h1 { +"a11y rapporteringsverktøy for NAV" }
            p {
                a(classes = "cta") {
                    href = "/reports/new"
                    +"Lag ny erklæring"
                }
            }
            h2 { +"Rapporter for enkeltsider" }
            if (reports.filter { it.reportType == ReportType.SINGLE }.isNullOrEmpty()) {
                p {+"Ingen rapporter"}
            } else
            ul("report-list") {
                reports.filter { it.reportType == ReportType.SINGLE }
                    .forEach { report -> reportListItem(report) }
            }

            h2 { +"Samlerapporter" }
            if (    reports.filter { it.reportType == ReportType.AGGREGATED }.isNullOrEmpty()) {
                p {+"Ingen samlerapporter"}
            } else
            ul {
                reports.filter { it.reportType == ReportType.AGGREGATED }.forEach { report -> reportListItem(report) }
            }
        }
    }
}