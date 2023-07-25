package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.body
import kotlinx.css.form
import kotlinx.html.*


fun Route.organizationUnits(repository: ReportRepository) {

    get("orgunit/{id?}") {

        call.parameters["id"]?.let { unitId ->
            val (org, reports) = repository.getReportForOrganizationUnit(unitId)

            org?.let { orgUnit ->
                call.respondHtmlContent("Organisasjonsenhter") {
                    h1 { +"${orgUnit.name} accessibility reports" }
                    ul {
                        reports.forEach { report ->
                            li {
                                a {
                                    href = "reports/${report.reportId}"
                                    +"Rapport for ${report.url}"
                                }
                            }
                        }
                    }
                }
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }
        } ?: run {
            call.respondHtmlContent("Organisasjonsenheter") {
                h1 { +"Organisasjonsenheter" }
                ul {
                    repository.getAllOrganizationUnits().forEach { orgUnit ->
                        li {
                            a {
                                href = "orgunit/${orgUnit.id}"
                                +"Rapporter for ${orgUnit.name}"
                            }
                        }
                    }
                }

                a {
                    href = "orgunit/new"
                    +"Legg til organisajonsenhet"
                }


            }

        }
    }

    route("orgunit/new") {
        get {
            call.respondHtmlContent("Legg til organisasjonsenhet") {
                form {
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
                        hxPost("/orgunit/new")
                        +"opprett enhet"
                    }
                }
            }

        }

        post {
            val params = call.receiveParameters()
            val email = params["email"] ?: throw IllegalArgumentException("Organisasjonsenhet må ha en email")
            val name = params["name"] ?: throw IllegalArgumentException("Orhanisasjonsenhet må ha ett navn")

            repository.insertOrganizationUnit(
                OrganizationUnit.createNew(
                    name = name,
                    email = email
                )
            )

            call.response.headers.append("HX-Redirect", "/orgunit")
            call.respond(HttpStatusCode.Created) {

            }

        }
    }

}

fun Route.userRoute(repository: ReportRepository) {
    get("user") {

        val reports = repository.getReportsForUser(call.user.email)
        call.respondHtmlContent(call.user.email) {
            h1 { +"${call.user.email} accessibility reports" }
            ul {
                reports.forEach { report ->
                    li {
                        a {
                            href = "reports/${report.reportId}"
                            +"Rapport for ${report.url}"
                        }
                    }
                }
            }

            a {
                href = "/reports/new"
                +"Start a new report"
            }
        }

    }

}