package accessibility.reporting.tool.html

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.NavBarItem
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import accessibility.reporting.tool.wcag.Report
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.h1
import kotlinx.html.p
import kotlinx.html.ul

fun Route.userRoute(repository: ReportRepository) {
    get("user") {

        val reports = repository.getReportsForUser<Report>(call.user.oid)
            .sortedBy { it.descriptiveName?.lowercase()?:it.url }
        call.respondHtmlContent("Rapporter for: " + call.user.email, NavBarItem.BRUKER) {
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