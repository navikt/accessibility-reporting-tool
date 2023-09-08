package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.adminUser
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

private const val updateCriterionEndpoint = "/collection/submit"
private const val updateMetadataPath = "/collection/metadata"

fun Route.adminRoutes(repository: ReportRepository) {
    route("admin") {
        get {
            call.unahtorizedIfNotAdmin()
            call.respondHtmlContent("Admin", NavBarItem.NONE) {
                h1 { +"Admin" }
                h2 { +"Genererte rapporter" }
                repository.getReports<ReportShortSummary>(ReportType.AGGREGATED).let { reports ->
                    if (reports.isNotEmpty()) {
                        ul(classes = "report-list") { reports.map { reportListItem(it, true, "/reports/collection") } }
                    } else {
                        p { +"Fant ingen aggregrete rapport" }
                    }
                }
                a {
                    href = "reports/collection/new"
                    +"Generer ny erklæring"
                }
            }
        }
    }

    route("/reports/collection/") {

        get("{id}") {
            call.unahtorizedIfNotAdmin()
            val reportId = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport<AggregatedReport>(reportId) ?: throw IllegalArgumentException()
            val organizations = repository.getAllOrganizationUnits()

            call.respondHtmlContent("Tilgjengelighetsærklæring", NavBarItem.NONE) {
                reportContainer(
                    report = report,
                    organizations = organizations,
                    updateCriterionUrl = updateCriterionEndpoint,
                    updateMetadataUrl = updateMetadataPath
                ) {
                    add(StatementMetadata(label = "Kilder", null, ddProducer = {
                        dd {
                            ul {
                                report.fromReports.map { from ->
                                   reportListItem(from, false)
                                }
                            }
                        }
                    }))
                }
            }
        }

        delete("/{id}") {
            call.unahtorizedIfNotAdmin()
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            repository.deleteReport(id)
            val reports = repository.getReports<ReportShortSummary>(ReportType.AGGREGATED)
            fun response() = createHTML().ul(classes = "report-list") {
                reports.map { report -> reportListItem(report, true, "/reports/collection") }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }

        route("new") {
            get {
                val reports = repository.getReports<ReportShortSummary>(ReportType.SINGLE)
                val organizationUnits = repository.getAllOrganizationUnits()

                call.respondHtmlContent("Admin – Generer rapport", NavBarItem.ADMIN) {
                    h1 { +"Generer ny rapport" }
                    form {
                        action = "new"
                        method = FormMethod.post
                        fieldSet {
                            legend { +"Metdata" }
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
                                    organizationUnits.map { orgUnit ->
                                        option {
                                            value = orgUnit.id
                                            +orgUnit.name
                                        }
                                    }
                                }
                            }
                        }
                        fieldSet {
                            legend { +"Velg rapporter" }
                            reports.map {
                                div(classes = "radio-with-label") {
                                    input {
                                        name = "report"
                                        id = "report-${it.reportId}"
                                        type = InputType.checkBox
                                        value = it.reportId
                                    }
                                    label {
                                        htmlFor = "report-${it.reportId}"
                                        +(it.descriptiveName ?: it.url)
                                    }
                                }
                            }
                        }
                        input {
                            type = InputType.submit
                            value = "Genererer"
                        }
                    }
                }
            }

            post {
                val formParameters = call.receiveParameters()

                AggregatedReport(
                    url = formParameters["page-url"].toString(),
                    descriptiveName = formParameters["descriptive-name"].toString(),
                    organizationUnit = formParameters["orgunit"].toString().let { id ->
                        repository.getOrganizationUnit(id)
                    },
                    user = call.adminUser,
                    reports = repository.getReports<Report>(ids = formParameters.getAll("report"))
                ).let {
                    repository.upsertReportReturning<AggregatedReport>(it)
                }.apply {
                    call.respondRedirect("/reports/collection/$reportId")
                }
            }
        }
    }


    updateCriterionRoute(updateCriterionEndpoint) { parameters ->
        val oldReport = repository.getReport<AggregatedReport>(call.id) ?: throw IllegalArgumentException()
        val criterion: SuccessCriterion = oldReport.updateCriterion(
            criterionNumber = parameters.criterionNumber,
            statusString = parameters.status,
            breakingTheLaw = parameters.breakingTheLaw,
            lawDoesNotApply = parameters.lawDoesNotApply,
            tooHardToComply = parameters.tooHardToComply
        )
        repository.upsertReportReturning<AggregatedReport>(oldReport.withUpdatedCriterion(criterion, call.user))
    }


    updateMetdataRoute(
        repository = repository,
        routingPath = updateMetadataPath,
        upsertReportFunction = { report -> upsertReportReturning<Report>(report) },
        getReportFunction = { id -> getReport<AggregatedReport>(id) },
        validateAccess = ApplicationCall::unahtorizedIfNotAdmin
    )
}

suspend fun ApplicationCall.unahtorizedIfNotAdmin(redirectToAdmin: String? = null) {
    when {
        !Admins.isAdmin(user) -> respond(
            HttpStatusCode.Unauthorized,
            "Du har ikke tilgang til denne siden"
        )

        redirectToAdmin != null -> respondRedirect(redirectToAdmin)
    }
}

private fun FORM.organizationUnitSelect(organizationUnit: OrganizationUnit, checked: Boolean) {
    input {
        type = InputType.checkBox
        value = organizationUnit.id
        text(organizationUnit.name)
        hxTrigger("change[target.checked]")
        hxTarget("#test")
        hxGet("admin/aggregated/")
    }
}


/*
* get {
            val reports = reportRepository.getReports()
            val groupedCriteria = reports.map { it.successCriteria }
                .flatten()
                .groupBy { it.number }
                .map { it.value }

            call.respondHtmlContent("Status for hele NAV") {
                h1 { +"Status for hele NAV" }
                groupedCriteria.forEach { criterionStatus(it) }
            }
        }
*
*
* */