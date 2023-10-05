package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.util.UUID

private const val updateCriterionPath = "/reports/submit"
private const val updateMetadataPath = "/metadata"

fun Route.reports(repository: ReportRepository) {

    route("/reports") {
        get("{id}") {
            val reportId = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport<Report>(reportId) ?: throw IllegalArgumentException()
            if (report.reportType == ReportType.AGGREGATED) {
                call.unahtorizedIfNotAdmin("/reports/collection/$reportId")
            }
            val organizations = repository.getAllOrganizationUnits()
            val readOnly: Boolean = when {
                report.isOwner(call.user) -> false
                Admins.isAdmin(call.user) -> false
                report.organizationUnit != null -> {
                    organizations.find { it.id == report.organizationUnit.id }
                        ?.let { reportOrg -> reportOrg.members.none { it == call.user.email } }
                }
                else -> true
            } ?: true

            call.respondHtmlContent("Tilgjengelighetsærklæring", NavBarItem.NONE) {
                reportContainer(
                    report = report,
                    organizations = organizations,
                    updateCriterionUrl = updateCriterionPath,
                    updateMetadataUrl = updateMetadataPath,
                    readOnly = readOnly
                )
            }
        }
        delete("/{id}") {
            repository.deleteReport(call.id)
            val reports = repository.getReportsForUser(call.user.oid!!) //TODO fjern
            fun response() = createHTML().ul(classes = "report-list") {
                reports.map { report -> reportListItem(report, true) }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }
        delete("/single/{id}") {
            repository.deleteReport(call.id)
            val reports = repository.getReports<ReportShortSummary>(ReportType.SINGLE) //TODO fjern
            fun response() = createHTML().ul(classes = "report-list") {
                reports.map { report -> reportListItem(report, true, deletePath = "/reports/single") }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }


        route("new") {

            post {
                val formParameters = call.receiveParameters()
                val url = formParameters["page-url"].toString()
                val organizationUnit = formParameters["orgunit"].toString().let { id ->
                    repository.getOrganizationUnit(id)
                }
                val descriptiveName = formParameters["descriptive-name"].toString()

                val newReportId = UUID.randomUUID().toString()
                repository.upsertReport(
                    Version1.newReport(
                        organizationUnit, newReportId, url, call.user, descriptiveName
                    )
                )
                call.response.header("HX-Redirect", "/reports/$newReportId")
                call.respond(HttpStatusCode.Created)
            }

            get {
                val orgUnits = repository.getAllOrganizationUnits()
                call.respondHtmlContent("Ny tilgjengelighetserklæring", NavBarItem.NONE) {
                    h1 {
                        +"Lag ny tilgjengelighetserklæring"
                    }
                    div(classes = "create-form") {
                        form {
                            hxPost("/reports/new")
                            hxSwapOuter()
                            hxTarget(".create-form")
                            label {
                                +"Url til siden "
                                input {
                                    type = InputType.text
                                    required = true
                                    placeholder = "Url"
                                    name = "page-url"

                                }
                            }
                            label {
                                +"Tittel"
                                input {
                                    type = InputType.text
                                    required = true
                                    placeholder = "Tittel"
                                    name = "descriptive-name"
                                }
                            }
                            label {
                                +" Organisasjonsenhet/team"
                                select {
                                    name = "orgunit"
                                    option {
                                        disabled = true
                                        selected = true
                                        +"Velg organisasjonsenhet/team"
                                    }
                                    orgUnits.map { orgUnit ->
                                        option {
                                            value = orgUnit.id
                                            +orgUnit.name
                                        }
                                    }
                                }
                            }
                            button {
                                +"Lag ny erklæring"
                            }
                        }
                    }
                }
            }
        }
    }

    updateCriterionRoute(updateCriterionPath) { parameters ->
        val oldReport = repository.getReport<Report>(call.id) ?: throw IllegalArgumentException()
        val criterion: SuccessCriterion = oldReport.updateCriterion(
            criterionNumber = parameters.criterionNumber,
            statusString = parameters.status,
            breakingTheLaw = parameters.breakingTheLaw,
            lawDoesNotApply = parameters.lawDoesNotApply,
            tooHardToComply = parameters.tooHardToComply
        )
        repository.upsertReport(oldReport.withUpdatedCriterion(criterion, call.user))
    }
    updateMetdataRoute(repository = repository, routingPath = updateMetadataPath)

}
