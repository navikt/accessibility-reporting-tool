package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.NavBarItem
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.ul

fun Route.userRoute(repository: ReportRepository) {
    get("user") {

        val reports = repository.getReportsForUser(call.user.oid)
        call.respondHtmlContent("Rapporter for: " + call.user.email.str(), NavBarItem.BRUKER) {
            h1 { +"Dine tilgjengelighetserklæringer" }
            a(classes = "cta") {
                href = "/reports/new"
                +"Lag ny erklæring"
            }

            if (reports.isNotEmpty())
                ul(classes = "report-list") {
                    reports.map { report -> reportListItem(report, report.isOwner(call.user)) }
                }
            else p { +"Du har ingen tilgjengelighetserklæringer enda" }

        }

    }
}