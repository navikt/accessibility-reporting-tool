package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.disputedDeviationCount
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.body
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.lang.IllegalArgumentException
import java.util.UUID

fun Route.aggregatedReports(repository: ReportRepository) {

    get("reports/orgunit/{id}/all") {
        call.parameters["id"]?.let { unitId ->
            val (org, reports) = repository.getReportForOrganizationUnit(unitId)
            var deviationCount = 0
            var disputedCount = 0
            reports.forEach {
                deviationCount += it.successCriteria.deviationCount()
                disputedCount += it.successCriteria.disputedDeviationCount()
            }

            org?.let { orgUnit ->
                call.respondHtmlContent(orgUnit.name) {
                    h1 { +"Status for ${orgUnit.name} " }
                    p { + "Antall punkter avvik: $deviationCount" }
                    p { + "Antall punkter med merknad: $disputedCount" }
                }
            } ?: run { call.respond(HttpStatusCode.NotFound) }
        }
    }
}
