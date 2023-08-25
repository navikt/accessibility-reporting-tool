package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.util.UUID


fun Route.reports(repository: ReportRepository) {

    route("/reports") {

        get("{id}") {

            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport(id) ?: throw IllegalArgumentException()

            call.respondHtmlContent("Tilgjengelighetsærklæring", NavBarItem.NONE) {
                main {
                    h1 { +"Tilgjengelighetserklæring" }
                    div(classes = "statement-metadata") {
                        statementMetadata("Løsning", report.url)
                        report.descriptiveName?.let {
                            statementMetadata("Beskrivelse", it)
                        }
                        statementMetadata("Ansvarlig", report.user.email)
                        statementMetadata("Status", report.status(), "metadata-status")
                        statementMetadata("Sist oppdatert", report.lastChanged.displayFormat(), "metadata-oppdatert")
                        statementMetadata(
                            "Sist oppdatert av",
                            (report.lastUpdatedBy ?: report.user).email,
                            "metadata-oppdatert-av"
                        )
                        statementContributors(report.contributers)
                        statementOrganizationUnit(report.organizationUnit)
                    }
                    nav {
                        attributes["aria-label"] = "Status"
                        summaryLinks(report)
                    }
                    div {
                        report.successCriteria.map { a11yForm(it, id) }
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
            val criterion: SuccessCriterion =
                oldReport.updateCriterion(
                    criterionNumber = criterionNumber,
                    statusString = status,
                    breakingTheLaw = breakingTheLaw,
                    lawDoesNotApply = lawDoesNotApply,
                    tooHardToComply = tooHardToComply
                )
            val report = repository.upsertReport(oldReport.withUpdatedCriterion(criterion, call.user))

            fun response(): String = updatedMetadataString(report) + summaryLinksString(report) + createHTML().div {
                a11yForm(
                    report.findCriterion(criterionNumber), id
                )
            }

            call.respondText(
                contentType = ContentType.Text.Html,
                HttpStatusCode.OK, ::response
            )
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
                        organizationUnit,
                        newReportId,
                        url,
                        call.user,
                        descriptiveName
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
                                +"Beskrivelse (kort)"
                                input {
                                    type = InputType.text
                                    required = true
                                    placeholder = "Beskrivelse"
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

internal fun DIV.statementOrganizationUnit(organizationUnit: OrganizationUnit?) {
    p {
        span(classes = "bold") { +"Organisasjonsenhet/team: " }
        organizationUnit?.let { org ->
            a {
                href = "/orgunit/${org.id}"
                +org.name
            }
        }
    }
}

internal fun DIV.statementContributors(contributers: List<User>) {
    if (contributers.isNotEmpty())
        p { span(classes = "bold") { +"Bidragsytere " } }
    ul {
        contributers.map { contributer ->
            li {
                +contributer.email
            }
        }
    }
}

fun updatedMetadataString(report: Report): String = """
    ${createHTML().p { statementMetadataInnerHtml("Status", report.status(), "metadata-status") }}
    ${createHTML().p { statementMetadataInnerHtml("Sist oppdatert", report.lastChanged.displayFormat(), "metadata-oppdatert")}}
    ${createHTML().p { statementMetadataInnerHtml(
    "Sist oppdatert av",
    (report.lastUpdatedBy ?: report.user).email,
    "metadata-oppdatert-av"
) }}""".trimMargin()


private fun LocalDateTime.displayFormat(): String = "$dayOfMonth.$monthValue.$year"
