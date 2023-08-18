package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Routing.meta() {

    get("/isalive") {
        call.respond(HttpStatusCode.OK)
    }
    get("/isready") {
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.landingPage(repository: ReportRepository) {
    get {
        val reports = repository.getReports()
        call.respondHtmlContent("a11y rapportering") {

            h1 { +"a11y rapporteringsverktøy for NAV" }
            h2 { +"Rapporter" }
            ul {
                reports.forEach { report ->
                    li {
                        a {
                            href = "reports/${report.reportId}"
                            +report.url
                        }
                    }
                }
            }
        }

    }
}
suspend fun ApplicationCall.respondHtmlContent(title: String, contenbuilder: BODY.() -> Unit) {
    respondHtml {
        lang = "no"
        head { headContent(title) }
        body {
            navbar()
            contenbuilder()
        }

    }
}