package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.SuccessCriterion
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.div
import kotlinx.html.select
import kotlinx.html.stream.createHTML


fun Route.updateCriterionRoute(getUser: ApplicationCall.() -> User, routingPath: String, repository: ReportRepository) {
    post("$routingPath/{id}") {
        val id = call.parameters["id"] ?: throw IllegalArgumentException()
        val formParameters = call.receiveParameters()
        val status = formParameters["status"].toString()
        val criterionNumber = formParameters["index"].toString()
        val breakingTheLaw = formParameters["breaking-the-law"]
        val lawDoesNotApply = formParameters["law-does-not-apply"]
        val tooHardToComply = formParameters["too-hard-to-comply"]
        val oldReport: Report = repository.getReport(id) ?: throw IllegalArgumentException()
        val criterion: SuccessCriterion = oldReport.updateCriterion(
            criterionNumber = criterionNumber,
            statusString = status,
            breakingTheLaw = breakingTheLaw,
            lawDoesNotApply = lawDoesNotApply,
            tooHardToComply = tooHardToComply
        )
        val report = repository.upsertReport(oldReport.withUpdatedCriterion(criterion, call.getUser()))

        fun response(): String = updatedMetadataStatus(report) + summaryLinksString(report) + createHTML().div {
            a11yForm(
                report.findCriterion(criterionNumber),
                report.reportId,
                routingPath
            )
        }

        call.respondText(
            contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response
        )

    }

}

fun Route.updateMetdataRoute(getUser: ApplicationCall.() -> User, routingPath: String, repository: ReportRepository) {
    post("{id}/$routingPath") {
        val formParameters = call.receiveParameters()
        repository.getReport<Report>(call.parameters["id"] ?: throw IllegalArgumentException())
            ?.withUpdatedMetadata(
                title = formParameters["report-title"],
                pageUrl = formParameters["page-url"],
                organizationUnit = null, //TODO
                updateBy = call.getUser()
            )
            ?.let { repository.upsertReport(it) }
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.updateOrganizationUnitRoute(
    getUser: ApplicationCall.() -> User,
    routingPath: String,
    repository: ReportRepository
) {
    post("/{id}$routingPath") {
        val formParameters = call.receiveParameters()
        val organizations = repository.getAllOrganizationUnits()
        val report =
            repository.getReport<Report>(
                call.parameters["id"] ?: throw IllegalArgumentException("Rapport-id mangler")
            )
                ?.withUpdatedMetadata(
                    title = null,
                    pageUrl = null,
                    organizationUnit = organizations.find { it.id == formParameters["org-selector"] },
                    updateBy = call.getUser()
                )
                ?.let { repository.upsertReport(it) }

        fun response() = createHTML().select {
            orgSelector(organizations, report!!, routingPath)
        }
        call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
    }
}