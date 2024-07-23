package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.LocalDateTime
import java.util.UUID

fun Route.jsonApiReports(reportRepository: ReportRepository, organizationRepository: OrganizationRepository) {

    route("reports") {
        get("/list") {
            call.respond(
                reportRepository.getReports<ReportShortSummary>()
                    .map { ReportWithUrl(it.url, it.descriptiveName ?: it.url) })
        }

        get("/{id}") {

            val id =
                call.parameters["id"] ?: throw BadRequestException("Missing id")

            val result = reportRepository.getReport<Report>(id)
                ?.toFullReport()

            if (result != null) {
                call.respond(result)
            } else {
                call.respond(HttpStatusCode.NotFound, "Report not found")
            }

        }

        post("/new") {
            val report = call.receive<Rapport>()
            val organizationUnit = organizationRepository.getOrganizationUnit(report.teamId)

            val newReport = reportRepository.upsertReport(
                SucessCriteriaV1.newReport(

                    organizationUnit = organizationUnit,
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
                call.parameters["id"] ?: throw BadPathParameterException("Missing id")
            val updatedCriteria = call.receive<FullReport>()

            val existingReport =
                reportRepository.getReport<Report>(id) ?: throw ResourceNotFoundException(type = "Report", id = id)

            val updatedReport = existingReport.updateCriteria(updatedCriteria.successCriteria, call.user)
            val result = reportRepository.upsertReport(updatedReport).toFullReport()
            call.respond(HttpStatusCode.OK, result)

        }
        patch("/{id}/update") {
            val id = call.parameters["id"] ?: throw BadPathParameterException("Missing id")
            val updates = call.receive<ReportUpdate>()

            val existingReport =
                reportRepository.getReport<Report>(id) ?: throw ResourceNotFoundException(type = "Report", id = id)

            var updatedReport = existingReport

            updates.descriptiveName?.let {
                updatedReport = updatedReport.copy(descriptiveName = it)
            }
            updates.team?.let {
                updatedReport = updatedReport.copy(organizationUnit = it)
            }
            updates.author?.let {
                updatedReport = updatedReport.copy(author = it)
            }
            updates.created?.let {
                updatedReport = updatedReport.copy(created = LocalDateTime.parse(it))
            }
            updates.lastChanged?.let {
                updatedReport = updatedReport.copy(lastChanged = LocalDateTime.parse(it))
            }

            updates.successCriteria?.let { criteriaList ->
                updatedReport = updatedReport.copy(successCriteria = updatedReport.successCriteria.map { currentCriterion ->
                    val updatedCriterion = criteriaList.find { it.number == currentCriterion.number }
                        SuccessCriterion(
                            name = currentCriterion.name,
                            description = currentCriterion.description,
                            principle = currentCriterion.principle,
                            guideline = currentCriterion.guideline,
                            tools = currentCriterion.tools,
                            number = currentCriterion.number,
                            breakingTheLaw = updatedCriterion?.breakingTheLaw ?: currentCriterion.breakingTheLaw,
                            lawDoesNotApply = updatedCriterion?.lawDoesNotApply ?: currentCriterion.lawDoesNotApply,
                            tooHardToComply = updatedCriterion?.tooHardToComply ?: currentCriterion.tooHardToComply,
                            contentGroup = updatedCriterion?.contentGroup ?: currentCriterion.contentGroup,
                            status = updatedCriterion?.status?.let { Status.valueOf(it) } ?: currentCriterion.status,
                            wcagUrl = currentCriterion.wcagUrl,
                            helpUrl = currentCriterion.helpUrl,
                            wcagVersion = currentCriterion.wcagVersion
                        ).apply {
                            wcagLevel = currentCriterion.wcagLevel
                        }
                    })
            }

            val result = reportRepository.upsertReport(updatedReport).toFullReport()
            call.respond(HttpStatusCode.OK, result)

    }}}


data class ReportWithUrl(
    val url: String,
    val navn: String,
)

data class Rapport(val name: String, val urlTilSiden: String, val teamId: String)

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

fun Report.toFullReport(): FullReport {
    return FullReport(
        reportId = this.reportId,
        descriptiveName = this.descriptiveName,
        url = this.url,
        team = this.organizationUnit,
        author = this.author,
        successCriteria = this.successCriteria,
        created = this.created,
        lastChanged = this.lastChanged
    )
}

data class ReportUpdate(
    val descriptiveName: String? = null,
    val team: OrganizationUnit? = null,
    val author: Author? = null,
    val created: String? = null,
    val lastChanged: String? = null,
    val successCriteria: List<SuccessCriterionUpdate>? = null
)

data class SuccessCriterionUpdate(
    val number: String,
    val breakingTheLaw: String? = null,
    val lawDoesNotApply: String? = null,
    val tooHardToComply: String? = null,
    val contentGroup: String? = null,
    val status: String? = null
)

