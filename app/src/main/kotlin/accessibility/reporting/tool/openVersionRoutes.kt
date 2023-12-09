package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.NavBarItem
import accessibility.reporting.tool.microfrontends.openReport
import accessibility.reporting.tool.microfrontends.reportContainer
import accessibility.reporting.tool.microfrontends.respondHtmlContent
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.openReportRoute(repository: ReportRepository) {

    get("open/{id}") {
        val reportId = call.parameters["id"] ?: throw IllegalArgumentException("mangler rapportid")
        val report = repository.getReport<Report>(reportId) ?: throw IllegalArgumentException()
        if (report.reportType == ReportType.AGGREGATED) {
            //TODO
            call.respondRedirect("/reports/collection/$reportId")
        }
        call.respondHtmlContent("Tilgjengelighetsærklæring for ${report.descriptiveName}", NavBarItem.NONE) {
            openReport(report)
        }
    }
}