package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.authenitcation.userOrNull
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.annotation.JsonFormat
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
        get {
            call.respond(reportRepository.getReports<ReportListItem>(ReportType.SINGLE))
        }
        post("/new") {
            val report = call.receive<NewReport>()
            val organizationUnit = organizationRepository.getOrganizationUnit(report.teamId)

            val newReport = reportRepository.upsertReport(
                SucessCriteriaV1.newReport(
                    organizationUnit = organizationUnit,
                    reportId = UUID.randomUUID().toString(),
                    url = report.urlTilSiden,
                    user = call.user,
                    descriptiveName = report.name,
                    isPartOfNavNo = report.isPartOfNavNo ?: true
                )
            )
            call.respondText(status = HttpStatusCode.OK, provider = {
                """{
     "id": "${newReport.reportId}"}
            """.trimMargin()
            }
            )
        }
        route("{id}") {
            get {
                val id = call.reportId

                val result = reportRepository.getReport<Report>(id)
                    ?.also {
                        if (it.reportType == ReportType.AGGREGATED) {
                            throw BadRequestException("Rapport med $id er en samlerapport, tilgjengelig på api/reports/aggregated/$id")
                        }
                    }
                    ?.toFullReportWithAccessPolicy(call.userOrNull)
                    ?: throw ResourceNotFoundException("report", id)

                call.respond(result)
            }
            patch {
                val id = call.reportId
                val updates = call.receive<ReportUpdate>()

                val existingReport =
                    reportRepository.getReport<Report>(id) ?: throw ResourceNotFoundException(type = "Report", id = id)

                var updatedReport = existingReport.copy(
                    descriptiveName = updates.descriptiveName,
                    organizationUnit = updates.team,
                    author = updates.author,
                    lastChanged = LocalDateTimeHelper.nowAtUtc(),
                    lastUpdatedBy = call.user.toAuthor(),
                    notes = updates.notes
                )

                updates.successCriteria?.let { pendingUpdateList ->
                    val currentCriteria = existingReport.successCriteria
                    val newCriteria = currentCriteria.map { currentCriterion ->
                        val updatedCriterion = pendingUpdateList.find { it.number == currentCriterion.number }
                        if (updatedCriterion != null) {
                            SuccessCriterion(
                                name = currentCriterion.name,
                                description = currentCriterion.description,
                                principle = currentCriterion.principle,
                                guideline = currentCriterion.guideline,
                                tools = currentCriterion.tools,
                                number = currentCriterion.number,
                                breakingTheLaw = updatedCriterion.breakingTheLaw ?: currentCriterion.breakingTheLaw,
                                lawDoesNotApply = updatedCriterion.lawDoesNotApply ?: currentCriterion.lawDoesNotApply,
                                tooHardToComply = updatedCriterion.tooHardToComply ?: currentCriterion.tooHardToComply,
                                contentGroup = updatedCriterion.contentGroup ?: currentCriterion.contentGroup,
                                status = updatedCriterion.status?.let { Status.valueOf(it) }
                                    ?: currentCriterion.status,
                                wcagUrl = currentCriterion.wcagUrl,
                                helpUrl = currentCriterion.helpUrl,
                                wcagVersion = currentCriterion.wcagVersion
                            ).apply {
                                wcagLevel = currentCriterion.wcagLevel
                            }
                        } else {
                            currentCriterion
                        }
                    }

                    updatedReport = updatedReport.updateCriteria(newCriteria, call.user)
                }

                reportRepository.upsertReport(updatedReport)
                call.respond(HttpStatusCode.OK)
            }
            delete {
                val id = call.reportId
                val report = reportRepository.getReport<Report>(id) ?: throw ResourceNotFoundException("report", id)
                val reportWithAccessPolicy = report.toFullReportWithAccessPolicy(call.userOrNull)

                if (reportWithAccessPolicy.hasWriteAccess) {
                    reportRepository.deleteReport(id)
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }


        //TODO: remove
        get("/list") {
            call.respond(reportRepository.getReports<ReportListItem>())
        }

        patch("/{id}/update") {
            val id = call.parameters["id"] ?: throw BadPathParameterException("Missing id")
            val updates = call.receive<ReportUpdate>()

            val existingReport =
                reportRepository.getReport<Report>(id) ?: throw ResourceNotFoundException(type = "Report", id = id)

            var updatedReport = existingReport.copy(
                descriptiveName = updates.descriptiveName,
                organizationUnit = updates.team,
                author = updates.author,
                lastChanged = LocalDateTimeHelper.nowAtUtc(),
                lastUpdatedBy = call.user.toAuthor()
            )

            updates.successCriteria?.let { pendingUpdateList ->
                val currentCriteria = existingReport.successCriteria
                val newCriteria = currentCriteria.map { currentCriterion ->
                    val updatedCriterion = pendingUpdateList.find { it.number == currentCriterion.number }
                    if (updatedCriterion != null) {
                        SuccessCriterion(
                            name = currentCriterion.name,
                            description = currentCriterion.description,
                            principle = currentCriterion.principle,
                            guideline = currentCriterion.guideline,
                            tools = currentCriterion.tools,
                            number = currentCriterion.number,
                            breakingTheLaw = updatedCriterion.breakingTheLaw ?: currentCriterion.breakingTheLaw,
                            lawDoesNotApply = updatedCriterion.lawDoesNotApply ?: currentCriterion.lawDoesNotApply,
                            tooHardToComply = updatedCriterion.tooHardToComply ?: currentCriterion.tooHardToComply,
                            contentGroup = updatedCriterion.contentGroup ?: currentCriterion.contentGroup,
                            status = updatedCriterion.status?.let { Status.valueOf(it) }
                                ?: currentCriterion.status,
                            wcagUrl = currentCriterion.wcagUrl,
                            helpUrl = currentCriterion.helpUrl,
                            wcagVersion = currentCriterion.wcagVersion
                        ).apply {
                            wcagLevel = currentCriterion.wcagLevel
                        }
                    } else {
                        currentCriterion
                    }
                }

                updatedReport = updatedReport.updateCriteria(newCriteria, call.user)
            }

            reportRepository.upsertReport(updatedReport)
            call.respond(HttpStatusCode.OK)
        }
    }
}

class NewReport(val name: String, val urlTilSiden: String, val teamId: String, val isPartOfNavNo: Boolean? = null)

class FullReportWithAccessPolicy(
    override val reportId: String,
    override val descriptiveName: String?,
    override val url: String,
    val team: OrganizationUnit?,
    val author: Author,
    val successCriteria: List<SuccessCriterion>,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val created: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val lastChanged: LocalDateTime,
    val hasWriteAccess: Boolean,
    val lastUpdatedBy: String,
    val isPartOfNavNo: Boolean,
    val notes: String
) : ReportContent


data class ReportUpdate(
    val descriptiveName: String? = null,
    val team: OrganizationUnit? = null,
    val author: Author? = null,
    val created: String? = null,
    val lastChanged: String? = null,
    val successCriteria: List<SuccessCriterionUpdate>? = null,
    val isPartOfNavNo: Boolean? = null,
    val notes: String? = null
)

data class SuccessCriterionUpdate(
    val number: String,
    val breakingTheLaw: String? = null,
    val lawDoesNotApply: String? = null,
    val tooHardToComply: String? = null,
    val contentGroup: String? = null,
    val status: String? = null
)

private val ApplicationCall.reportId
    get() = parameters["id"] ?: throw BadPathParameterException("Missing report {id}")
