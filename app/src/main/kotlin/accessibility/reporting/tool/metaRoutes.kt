package accessibility.reporting.tool

import accessibility.reporting.tool.microfrontends.NavBarItem.FORSIDE
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import io.ktor.http.*
import io.ktor.server.application.*
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
        call.respondHtmlContent("a11y rapportering", FORSIDE) {
            img {
                id="uu-katt"
                src = "/static/UU-katt.svg"
                alt = "A11y cat loves you!"
                title = "A11y cat loves you!"
            }

            h1 { +"a11y rapporteringsverktøy for NAV" }
            p {
                a (classes = "cta") {
                    href = "/reports/new"
                    +"Lag ny erklæring"
                }
            }
            h2 { +"Rapporter" }
            ul { reports.forEach { report -> reportListItem(report) } }

        }
    }
}