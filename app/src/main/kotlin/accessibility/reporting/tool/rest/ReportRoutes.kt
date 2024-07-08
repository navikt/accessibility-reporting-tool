package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.ReportShortSummary
import accessibility.reporting.tool.wcag.SucessCriteriaV1
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.jsonApiReports(repository: ReportRepository) {

    route("reports") {
        get("/list") {
            call.respond(
                repository.getReports<ReportShortSummary>()
                    .map { ReportWithUrl(it.url, it.descriptiveName?:it.url)})
        }

        post("/new") {
            val report = call.receive<Rapport>()
            val newReport=repository.upsertReport(SucessCriteriaV1.newReport(

                organizationUnit =null,
                reportId =UUID.randomUUID().toString(),
                url =report.urlTilSiden,
                user =User(
                    email = User.Email(s = "Markia"),
                    name = null,
                    oid = User.Oid(s = "Taniqua"),
                    groups = listOf()
                ),
                descriptiveName =report.name
            ))
            call.respondText(status = HttpStatusCode.OK, provider ={"""{
                "id": "${newReport.reportId}"}
            """.trimMargin()}
                    )
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
data class Rapport(val name: String, val urlTilSiden: String, val team: String)