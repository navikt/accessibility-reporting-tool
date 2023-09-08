package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.AggregatedReport
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.SuccessCriterion
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.html.div
import kotlinx.html.select
import kotlinx.html.stream.createHTML
import kotlin.reflect.KClass

val Parameters.status
    get() = this["status"].toString()
val Parameters.criterionNumber
    get() = this["index"].toString()
val Parameters.breakingTheLaw
    get() = this["breaking-the-law"]
val Parameters.lawDoesNotApply
    get() = this["law-does-not-apply"]
val Parameters.tooHardToComply
    get() = this["too-hard-to-comply"]

val ApplicationCall.id
    get() = parameters["id"] ?: throw IllegalArgumentException()

fun Route.updateCriterionRoute(
    routingPath: String,
    validateAccess: suspend ApplicationCall.() -> Unit = {},
    updateFunction: suspend PipelineContext<Unit, ApplicationCall>.(Parameters) -> Report
) {
    post("$routingPath/{id}") {
        call.validateAccess()
        val formParameters = call.receiveParameters()
        val report = updateFunction(formParameters)
        call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK) {
            updatedMetadataStatus(report) + summaryLinksString(report) + createHTML().div {
                a11yForm(
                    report.findCriterion(formParameters.criterionNumber),
                    report.reportId,
                    routingPath
                )
            }
        }
    }
}

fun Route.updateMetdataRoute(
    routingPath: String,
    repository: ReportRepository,
    getReportFunction: ReportRepository.(String) -> Report? = { id -> getReport<Report>(id) },
    upsertReportFunction: ReportRepository.(Report) -> Report = { id -> upsertReportReturning<Report>(id) },
    validateAccess: suspend ApplicationCall.() -> Unit = {}
) {
    post("$routingPath/{id}") {
        call.validateAccess()
        val formParameters = call.receiveParameters()
        val organizations = repository.getAllOrganizationUnits()
        val report = repository.getReportFunction(call.parameters["id"] ?: throw IllegalArgumentException())
            ?.withUpdatedMetadata(
                title = formParameters["report-title"],
                pageUrl = formParameters["page-url"],
                organizationUnit = formParameters["org-selector"]?.let { orgId ->
                    organizations.first { it.id == orgId }
                },
                updateBy = call.user
            )
            ?.let { repository.upsertReportFunction(it) }

        fun response() = createHTML().select { orgSelector(organizations, report!!, routingPath) }
        call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
    }

}