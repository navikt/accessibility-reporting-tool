package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.lang.IllegalArgumentException
import java.util.UUID

fun Route.reports(repository: ReportRepository) {

    route("/reports") {

        get {
            val reports = repository.getReports()
            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    headContent("Select reports")
                }
                body {
                    h1 { +"Select a page" }
                    div {
                        reports.map { report ->
                            a {
                                href = "reports/${report.reportId}"
                                +report.url
                            }
                        }
                    }
                    div {
                        a {
                            href = "/reports/new"
                            +"Lag ny rapport"
                        }
                    }
                    reports.map { it.url }
                }
            }
        }

        post("/submit/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val index = formParameters["index"].toString()
            val oldReport: Report = repository.getReport(id) ?: throw IllegalArgumentException()
            val criterion: SuccessCriterion =
                oldReport.successCriteria.find { it.successCriterionNumber == index }.let { criteria ->
                    criteria?.copy(status = Status.undisplay(status))
                        ?: throw IllegalArgumentException("ukjent successkriterie")
                }

            val report = repository.upsertReport(
                Report(
                    organizationUnit = oldReport.organizationUnit,
                    reportId = id,
                    successCriteria = oldReport.successCriteria.map { if (it.number == criterion.number) criterion else it },
                    testData = oldReport.testData,
                    url = oldReport.url,
                    user = call.user,
                    version = Version.V1
                )
            )
            if (status == "non compliant") {
                // .div because I cannot find a .fragment or similar.
                // This means that you have to hx-select on the other end
                fun response() = createHTML().div {
                    a11yForm(report.findCriterion(index), id)
                }
                call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
            } else {
                fun response() = createHTML().div {
                    a11yForm(report.findCriterion(index), id)
                }
                call.respondText(
                    contentType = ContentType.Text.Html,
                    HttpStatusCode.OK, ::response
                )
            }
        }


        get("{id}") {

            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport(id) ?: throw IllegalArgumentException()

            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    headContent("A11y report")
                }
                body {
                    p {
                        +"${report.user.email}"
                    }
                    a {
                        href = "/"
                        +"back to top"
                    }
                    main {

                        h1 { +"A11y report" }
                        p { +"Hvem fyller ut rapporten?" }
                        p { +"Fyller du ut rapporten på vegne av et annet team?" }
                        p { +"Kontaktperson fra det andre teamet" }
                        h2 { +"Om løsningen" }
                        p { +"Hva heter løsningen?" }
                        p { +"Løsningens base-URL" }
                        p { +"(For PoC'en) URLen som er testet" }
                        div {
                            report.successCriteria.map { a11yForm(it, id) }
                        }
                    }
                }
            }
        }

        delete("{id}") {}

        post("new") {

            val formParameters = call.receiveParameters()
            val url = formParameters["page-url"].toString()

            val newReportId = UUID.randomUUID().toString()
            repository.upsertReport(
                Report(
                    organizationUnit = null,
                    reportId = newReportId,
                    successCriteria = Version1.criteria,
                    testData = null,
                    url = url,
                    user = call.user,
                    version = Version.V1
                )
            )
            fun response() = createHTML().div(classes = "create-form") {
                h2 {
                    +"ny rapport"
                }
                p {
                    a {
                        href = "/reports/${newReportId}"
                        +"Rediger rapport"
                    }
                }
                button {
                    hxDelete("/reports/${newReportId}")
                    +"slett rapport"
                }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }

        get("new") {
            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    headContent("Lag ny rapport")
                }
                body {
                    h1 {
                        +"Lag ny rapport"
                    }
                    div(classes = "create-form") {
                        form {
                            label {
                                +"Url til siden "
                                input {
                                    type = InputType.text
                                    placeholder = "Url"
                                    name = "page-url"

                                }
                                button {
                                    hxSwapOuter()
                                    hxTarget(".create-form")
                                    hxPost("/reports/new")
                                    +"Lag ny rapport"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
