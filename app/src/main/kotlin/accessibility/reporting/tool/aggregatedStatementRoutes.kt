package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
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

            call.respondHtmlContent("Status for hele NAV") {
                h1 { +"Status for hele NAV" }
                groupedCriteria.forEach { criterionStatus(it) }
            }
        }
    }
}