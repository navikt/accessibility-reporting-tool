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

        get("{id}") {

            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val report = repository.getReport(id) ?: throw IllegalArgumentException()

            call.respondHtmlContent("A11y rapport") {
                p {
                    +"Ansvarlig: ${report.user.email}"
                }
                main {
                    h1 { +"A11y report" }
                    div {
                        report.successCriteria.map { a11yForm(it, id) }
                    }
                }
            }
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            repository.deleteReport(id)
            val reports = repository.getReportsForUser(call.user.email)
            fun response() = createHTML().ul(classes = "report-list") { reports.map { reportListItem(it) } }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }

        post("/submit/{id}") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException()
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val index = formParameters["index"].toString()
            val breakingTheLaw = formParameters["breaking-the-law"]
            val lawDoesNotApply = formParameters["law-does-not-apply"]
            val tooHardToComply = formParameters["too-hard-to-comply"]
            val oldReport: Report = repository.getReport(id) ?: throw IllegalArgumentException()
            val criterion: SuccessCriterion =
                oldReport.successCriteria.find { it.successCriterionNumber == index }.let { criteria ->
                    criteria?.copy(
                        status = Status.undisplay(status),
                        breakingTheLaw = breakingTheLaw ?: criteria.breakingTheLaw,
                        lawDoesNotApply = lawDoesNotApply ?: criteria.lawDoesNotApply,
                        tooHardToComply = tooHardToComply ?: criteria.tooHardToComply
                    )
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

        route("new") {

            post {

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
                call.response.header("HX-Redirect", "/reports/$newReportId")
                call.respond(HttpStatusCode.Created)
            }

            get {
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
}