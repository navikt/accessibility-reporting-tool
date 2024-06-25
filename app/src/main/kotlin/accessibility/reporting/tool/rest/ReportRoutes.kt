package accessibility.reporting.tool.rest

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.ReportShortSummary
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.reports(repository: ReportRepository) {

    route("reports") {
        get("/summary") {
            call.respond(
                repository.getReports<ReportShortSummary>()
                    .map { ReportWithUrl(it.url, it.descriptiveName?:it.url)})
        }
    }
}

data class ReportWithUrl(
    val url: String,
    val navn: String,
) {
    fun List<ReportShortSummary>.toReportWithUrl() = this.map {
        ReportWithUrl(it.url, it.descriptiveName ?: url)
    }
}