package accessibility.reporting.tool.html

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportType
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*


fun Route.openReportRoute(repository: ReportRepository) {

    get("open/{id}") {
        val reportId = call.parameters["id"] ?: throw IllegalArgumentException("mangler rapportid")
        val report = repository.getReport<Report>(reportId) ?: throw IllegalArgumentException()
        if (report.reportType == ReportType.AGGREGATED) {
            call.respond(HttpStatusCode.NotFound)
        }
        call.respondHtmlOpenContent("Tilgjengelighetsærklæring for ${report.descriptiveName}") {
            openReport(report)
        }
    }
}

suspend fun ApplicationCall.respondHtmlOpenContent(
    title: String,
    classes: String = "default-body-class",
    contentbuilder: BODY.() -> Unit
){
    respondHtml {
        lang = "no"
        head {
            meta { charset = "UTF-8" }
            style {}
            title { +title }
            script { src = "https://unpkg.com/htmx.org/dist/htmx.js" }

            link {
                rel = "preload"
                href = "https://cdn.nav.no/aksel/@navikt/ds-css/2.9.0/index.min.css"
                attributes["as"] = "style"
            }
            link {
                rel = "stylesheet"
                href = "/static/style.css"

            }
            link {
                rel = "icon"
                type = "image/x-icon"
                href = "/static/a11y-cat-round.png"
            }
        }

        body(classes) {
            contentbuilder()
        }

    }

}