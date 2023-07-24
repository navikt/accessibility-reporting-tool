package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import java.lang.IllegalArgumentException

fun Route.reports(repository: ReportRepository) {

    route("/reports") {

        get() {
            val reports = repository.getReports()
            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    headContent("Select reports")
                }
                body {
                    h1 { +"Select a page" }
                    div {
                        a {
                            href = "reports/foo"
                            +"there's only this one, sir"
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
            // 1 is a good id?
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val index = formParameters["index"].toString()
            val filters = listOf(
                formParameters["multimedia-filter"],
                formParameters["form-filter"],
                formParameters["timelimit-filter"],
                formParameters["interaction-filter"]
            ).map { it.toString() }

            val report = repository.getReport(id)?.successCriteria?.find { it.successCriterionNumber == index }
            report?.let { foundReport ->

                if (status == "non compliant") {
                    // .div because I cannot find a .fragment or similar.
                    // This means that you have to hx-select on the other end
                    fun response() = createHTML().div {
                        a11yForm(foundReport)
                    }
                    call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
                } else {
                    fun response() = createHTML().div {
                        a11yForm(foundReport)
                    }
                    call.respondText(
                        contentType = ContentType.Text.Html,
                        HttpStatusCode.OK, ::response
                    )
                }
            } ?: run {
                call.respond(HttpStatusCode.NotFound, "ENOENT")
            }
        }

        get("{id}") {

            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport(id)?:throw IllegalArgumentException()

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
                            report.successCriteria.map { a11yForm(it) }
                        }
                    }
                }
            }
        }

        delete("{id}") {}

        post("new") {
            fun response() = createHTML().div(classes = "create-form") {
                h2 {
                    +"ny rapport"
                }
                p {
                    a {
                        href = "/reports/foo"
                        +"Rediger rapport"
                    }
                }
                button {
                    hxDelete("/reports/foo")
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
                                +"Url"
                                input {
                                    type = InputType.text
                                    placeholder = "url"

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