package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*


fun Route.organizationUnits(repository: ReportRepository) {
    route("orgunit") {

        get {
            call.respondHtmlContent("Organisasjonsenheter") {
                h1 { +"Organisasjonsenheter" }
                ul {
                    repository.getAllOrganizationUnits().forEach { orgUnit ->
                        hrefListItem("orgunit/${orgUnit.id}", orgUnit.name)
                    }
                }
                a {
                    href = "orgunit/new"
                    +"Legg til organisajonsenhet"
                }
            }
        }

        get("{id}") {

            call.parameters["id"]!!.let { unitId ->
                val (org, reports) = repository.getReportForOrganizationUnit(unitId)

                org?.let { orgUnit ->
                    call.respondHtmlContent(orgUnit.name) {
                        h1 { +orgUnit.name }
                        p {
                            +"epost: ${orgUnit.email}"
                        }
                        if (reports.isNotEmpty()) {
                            h2 { +"Tilgjengelighetserklæringer" }
                            ul { reports.forEach { report -> reportListItem(report) } }
                        } else p { + "${orgUnit.name} har ingen tilgjengelighetserklæringer enda" }
                    }
                } ?: run { call.respond(HttpStatusCode.NotFound) }
            }
        }

        route("new") {
            get {
                call.respondHtmlContent("Legg til organisasjonsenhet") {
                    h1 { +"Legg til organisasjonsenhet" }
                    form {
                        hxPost("/orgunit/new")
                        label {
                            htmlFor = "text-input-name"
                            +"Navn"
                        }
                        input {
                            id = "text-input-name"
                            name = "name"
                            type = InputType.text
                            required = true
                        }
                        label {
                            htmlFor = "input-email"
                            +"email"
                        }
                        input {
                            id = "input-email"
                            name = "email"
                            type = InputType.email
                            required = true
                        }

                        button {
                            +"opprett enhet"
                        }
                    }
                }
            }

            post {
                val params = call.receiveParameters()
                val email = params["email"] ?: throw IllegalArgumentException("Organisasjonsenhet må ha en email")
                val name = params["name"] ?: throw IllegalArgumentException("Organisasjonsenhet må ha ett navn")

                repository.insertOrganizationUnit(
                    OrganizationUnit.createNew(
                        name = name,
                        email = email
                    )
                )

                call.response.headers.append("HX-Redirect", "/orgunit")
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

fun Route.userRoute(repository: ReportRepository) {
    get("user") {

        val reports = repository.getReportsForUser(call.user.oid!!) //TODO: fjern optional når rapportert er oppdatert
        call.respondHtmlContent(call.user.email) {
            h1 { +"Dine tilgjengelighetserklæringer" }
            if (reports.isNotEmpty())
                ul(classes = "report-list") {
                    reports.map { report -> reportListItem(report, report.userIsOwner(call.user)) }
                }
            else
            else p { + "Du har ingen tilgjengelighetserklæringer enda" }
            a {
                href = "/reports/new"
                +"Lag ny erklæring"
            }
        }

    }
}

fun UL.reportListItem(report: Report, allowDelete: Boolean = false) {
    li {
        a {
            href = "/reports/${report.reportId}"
            +(report.descriptiveName ?: report.url)
        }
        if (allowDelete)
            button {
                hxDelete("/reports/${report.reportId}")
                hxSwapOuter()
                hxConfirm("Er du sikker på at du vill slette denne erklæringen?")
                hxTarget(".report-list")
                +"Slett"
            }
    }
}
