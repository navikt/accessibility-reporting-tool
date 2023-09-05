package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.NavBarItem
import accessibility.reporting.tool.microfrontends.criterionStatus
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.h1

fun Route.aggregatedStatements(reportRepository: ReportRepository) {
    route("aggregated") {
        get {
            val reports = reportRepository.getReports()
            val groupedCriteria = reports.map { it.successCriteria }
                .flatten()
                .groupBy { it.number }
                .map { it.value }

            call.respondHtmlContent("Status for hele NAV", NavBarItem.NONE) {
                h1 { +"Status for hele NAV" }
                groupedCriteria.forEach { criterionStatus(it) }
            }
        }
    }
}