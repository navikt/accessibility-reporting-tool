package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.adminUser
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
            call.respondHtmlContent("Admin", NavBarItem.ADMIN) {
                h1 { +"Admin" }
                h2 { +"Genererte rapporter" }
                repository.getReports<ReportShortSummary>(ReportType.AGGREGATED).let { reports ->
                    if (reports.isNotEmpty()) {
                        ul { reports.map { reportListItem(it, true, "/reports/collection") } }
                    } else {
                        p { +"Fant ingen samlerapporter" }
                    }
                }

                repository.getReports<ReportShortSummary>(ReportType.SINGLE).let { reports ->
                    h2 { +"Rapporter for enkeltsider" }
                    ul("report-list") {
                        reports.filter { it.reportType == ReportType.SINGLE }
                            .forEach { report ->
                                reportListItem(
                                    report,
                                    Admins.isAdmin(call.user),
                                    deletePath = "/reports/single"
                                )
                            }
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
            val reportId = call.parameters["id"] ?: throw IllegalArgumentException()
            val report =
                repository.getReport<AggregatedReport>(reportId) ?: throw IllegalArgumentException("Ukjent rapport")
            val srcReports = repository.getReports<ReportShortSummary>(null, report.fromReports.map { it.reportId })
            val organizations = repository.getAllOrganizationUnits()

            call.respondHtmlContent("Tilgjengelighetserklæring", NavBarItem.NONE) {
                reportContainer(
                    report = report,
                    organizations = organizations,
                    updateCriterionUrl = updateCriterionEndpoint,
                    updateMetadataUrl = updateMetadataPath,
                    user = call.user
                ) {
                    add(StatementMetadata(label = "Kilder", null, ddProducer = {
                        dd {
                            ul {
                                report.fromReports.map { from ->
                                    li {
                                        if (from.wasDeleted(srcReports))
                                            +"${from.title} (slettet)"
                                        else {
                                            a {
                                                href = "/reports/${from.reportId}"
                                                +from.title
                                            }
                                            if (from.hasUpdates(srcReports))
                                                button {
                                                    hxTrigger("click")
                                                    hxPost("${report.reportId}/update?srcReport=${from.reportId}")
                                                    +"Hent nytt innhold"
                                                }
                                        }
                                    }
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

        post("/{id}/update") {
            call.unahtorizedIfNotAdmin()
            val id = call.parameters["id"] ?: throw IllegalArgumentException("id for rapport mangler")
            val srcReport = repository.getReport<Report>(
                call.parameters["srcReport"] ?: throw IllegalArgumentException("srcReportId må være satt")
            )
            repository
                .getReport<AggregatedReport>(id)
                ?.updateWithDataFromSource(srcReport)
                ?.also {
                    repository.upsertReportReturning<AggregatedReport>(it)
                }
                ?: throw IllegalArgumentException("Ukjent samlerapport")
            call.response.header("HX-Refresh", "true")
            call.respond(HttpStatusCode.OK)
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

private fun FORM.organizationUnitSelect(organizationUnit: OrganizationUnit) {
    input {
        type = InputType.checkBox
        value = organizationUnit.id
        text(organizationUnit.name)
        hxTrigger("change[target.checked]")
        hxTarget("#test")
        hxGet("admin/aggregated/")
    }
}
