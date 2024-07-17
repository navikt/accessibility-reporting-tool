package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.util.UUID

fun Route.jsonApiReports(repository: ReportRepository) {

    route("reports") {
        get("/list") {
            call.respond(
                repository.getReports<ReportShortSummary>()
                    .map { ReportWithUrl(it.url, it.descriptiveName ?: it.url) })
        }

        get("/{id}") {

            val id =
                call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing or malformed id")

            try {
                val result = repository.getReport<Report>(id)
                    ?.let {
                        FullReport(
                            it.reportId, it.descriptiveName, it.url,
                            team = it.organizationUnit,
                            author = it.author,
                            successCriteria = it.successCriteria,
                            created = it.created,
                            lastChanged = it.lastChanged
                        )
                    }
                if (result != null) {
                    call.respond(result)
                } else {
                    call.respond(HttpStatusCode.NotFound, "Report not found")
                }
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "An error occurred: ${e.message}")
            }
        }



        post("/new") {
            val report = call.receive<Rapport>()
            val newReport = repository.upsertReport(
                SucessCriteriaV1.newReport(

                    organizationUnit = null,
                    reportId = UUID.randomUUID().toString(),
                    url = report.urlTilSiden,
                    user = User(
                        email = User.Email(s = "Markia"),
                        name = null,
                        oid = User.Oid(s = "Taniqua"),
                        groups = listOf()
                    ),
                    descriptiveName = report.name
                )
            )
            call.respondText(status = HttpStatusCode.OK, provider = {
                """{
                "id": "${newReport.reportId}"}
            """.trimMargin()
            }
            )
        }
        put("/{id}/update") {
            val id =
                call.parameters["id"] ?: return@put call.respond(HttpStatusCode.BadRequest, "Missing or malformed id")
            val updatedCriteria = call.receive<List<SuccessCriterion>>()
            try {
                val existingReport = repository.getReport<Report>(id)
                if (existingReport == null) {
                    call.respond(HttpStatusCode.NotFound, "Report not found")
                    return@put
                }
                val updatedReport = existingReport.updateCriteria(updatedCriteria, call.user)
                repository.upsertReport(updatedReport)
                call.respond(HttpStatusCode.OK, "Report criteria updated successfully")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Failed to update report criteria: ${e.message}")
            }
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

    data class FullReport(
        override val reportId: String,
        override val descriptiveName: String?,
        override val url: String,
        val team: OrganizationUnit?,
        val author: Author,
        val successCriteria: List<SuccessCriterion>,
        val created: LocalDateTime,
        val lastChanged: LocalDateTime,
    ) : ReportContent

data class SuccessCriterionWithWcag(
    val name: String,
    val description: String,
    val principle: String,
    val guideline: String,
    val tools: String,
    val number: String,
    val breakingTheLaw: String,
    val lawDoesNotApply: String,
    val tooHardToComply: String,
    val contentGroup: String,
    var status: Status,
    val wcagUrl: String? = null,
    val helpUrl: String? = null,
    val wcagVersion: String = "2.1",
    var wcagLevel: WcagLevel
)
{
fun toSuccessCriterion(): SuccessCriterion {
    return SuccessCriterion(
        name,
        description,
        principle,
        guideline,
        tools,
        number,
        breakingTheLaw,
        lawDoesNotApply,
        tooHardToComply,
        contentGroup,
        status,
        wcagUrl,
        helpUrl,
        wcagVersion
    ).apply {
        this.wcagLevel = this@SuccessCriterionWithWcag.wcagLevel
    }
}
    companion object {
    fun List<SuccessCriterionWithWcag>.toSuccessCriteriaList(): List<SuccessCriterion> {
        return this.map { it.toSuccessCriterion() }
    }
}}

// lage en klasse som inneholder alle felter (inkludert WCAGlevel)
// lage en metode som konvertere klassen din til SuccesssCriterion
// .map lista med dine sukksesscriterier til e liste av SuccessCriterion