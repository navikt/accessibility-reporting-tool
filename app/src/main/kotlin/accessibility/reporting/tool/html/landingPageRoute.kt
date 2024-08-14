package accessibility.reporting.tool.html

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.NavBarItem
import accessibility.reporting.tool.microfrontends.reportListItem
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import accessibility.reporting.tool.wcag.ReportShortSummary
import accessibility.reporting.tool.wcag.ReportType
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Route.landingPage(repository: ReportRepository) {
    get {
        val reports = repository.getReports<ReportShortSummary>().sortedBy { it.title.lowercase() }
        call.respondHtmlContent("a11y rapportering", NavBarItem.FORSIDE) {
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

object NewPage {
    val url = resolveNewPageUrl()
    private fun resolveNewPageUrl(): String =
        when (System.getenv("NAIS_CLUSTER_NAME")) {
            "prod" -> "https://a11y-statement-ny.ansatt.nav.no/"
            "dev" -> "https://a11y-statement-ny.ansatt.dev.nav.no/"
            else -> "https://a11y-statement-ny.ansatt.dev.nav.no/"
        }
}