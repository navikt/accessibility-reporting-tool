package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
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


fun Route.reports(repository: ReportRepository) {

    route("/reports") {

        get("{id}") {

            val reportId = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport(reportId) ?: throw IllegalArgumentException()
            val organizations = repository.getAllOrganizationUnits()

            call.respondHtmlContent("Tilgjengelighetsærklæring", NavBarItem.NONE) {
                main(classes = "report-container") {
                    header(classes = "report-header") {
                        h1 { +"Tilgjengelighetserklæring (enkeltside)" }
                        div(classes = "statement-metadata") {
                            statementMetadataDl(report.reportId,
                                mutableListOf<StatementMetadata>().apply {
                                    add(
                                        StatementMetadata(
                                            "Tittel",
                                            report.descriptiveName ?: report.url,
                                            hxUpdateName = "report-title"
                                        )
                                    )

                                    add(StatementMetadata("URL", report.url, hxUpdateName = "page-url"))
                                    add(StatementMetadata("Ansvarlig", report.user.email, null))
                                    add(StatementMetadata("Status", report.statusString(), hxId = "metadata-status"))
                                    add(
                                        StatementMetadata(
                                            label = "Sist oppdatert",
                                            value = report.lastChanged.displayFormat(),
                                            hxId = "metadata-oppdatert"
                                        )
                                    )
                                    StatementMetadata(
                                        label = "Sist oppdatert av",
                                        value = (report.lastUpdatedBy ?: report.user).email,
                                        hxId = "metadata-oppdatert-av"
                                    )

                                    report.contributers.let {
                                        if (it.isNotEmpty())
                                            StatementMetadata("Bidragsytere", it.joinToString { "," })
                                    }
                                    add(StatementMetadata(label = "Organisasjonsenhet", value = null, ddProducer = {
                                        dd {
                                            select {
                                                orgSelector(organizations, report)
                                            }
                                        }
                                    }))

                                }

                            )


                        }
                    }

                    nav(classes = "sc-toc") {
                        attributes["aria-label"] = "Status"
                        summaryLinks(report)
                    }

                    div(classes = "sc-list") {
                        report.successCriteria.map { a11yForm(it, reportId) }
                    }

                    a(classes = "to-top") {
                        href = "#sc1.1.1"
                        +"Til toppen"
                    }
                }
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            repository.deleteReport(id)
            val reports = repository.getReportsForUser(call.user.oid!!) //TODO fjern
            fun response() = createHTML().ul(classes = "report-list") {
                reports.map { report -> reportListItem(report, true) }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }

        post("/submit/{id}") {
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
            val report = repository.upsertReport(oldReport.withUpdatedCriterion(criterion, call.user))

            fun response(): String = updatedMetadataStatus(report) + summaryLinksString(report) + createHTML().div {
                a11yForm(
                    report.findCriterion(criterionNumber), id
                )
            }

            call.respondText(
                contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response
            )
        }

        post("/metadata/{id}") {
            val formParameters = call.receiveParameters()
            repository.getReport(call.parameters["id"] ?: throw IllegalArgumentException())
                ?.withUpdatedMetadata(
                    title = formParameters["report-title"],
                    pageUrl = formParameters["page-url"],
                    organizationUnit = null, //TODO
                    updateBy = call.user
                )
                ?.let { repository.upsertReport(it) }

            call.respond(HttpStatusCode.OK)
        }

        post("/organization/{id}") {
            val formParameters = call.receiveParameters()
            val organizations = repository.getAllOrganizationUnits()
            val report =
                repository.getReport(call.parameters["id"] ?: throw IllegalArgumentException("Rapport-id mangler"))
                    ?.withUpdatedMetadata(
                        title = null,
                        pageUrl = null,
                        organizationUnit = organizations.find { it.id == formParameters["org-selector"] },
                        updateBy = call.user
                    )
                    ?.let { repository.upsertReport(it) }


            fun response() = createHTML().select {
                orgSelector(organizations, report!!)
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
}
