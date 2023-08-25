package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun Route.statementAdimnRoutes(reportRepository: ReportRepository) {
    route("admin") {
        route("aggregated") {
            get {
                call.respondHtmlContent("Admin – Aggregerte erklæringer") {
                    h1 { +"Tilgjengelighetserklæring " }
                    h2 { +"Genererte rapporter" }
                    ul {
                        li { +"todo - sist oppdatert" }
                    }
                    a {
                        href = "aggregated/new"
                        +"Lag ny generert erklæring"
                    }
                }
            }

            get("new") {
                val reports = reportRepository.getReports()
                val organizationUnits = reportRepository.getAllOrganizationUnits()

                call.respondHtmlContent("Generer rapport") {
                    h1 { +"Generer ny rapport" }
                    form {
                        fieldSet {
                            legend { +"Organisasjonsenhet" }
                            organizationUnits.forEach {
                                input {
                                    type = InputType.checkBox
                                    value = it.id
                                    text(it.name)
                                 }
                            }
                        }

                        div {
                            id="test"
                        }
                    }
                    /*form(classes = "generate-form") {
                        fieldSet {
                            legend { +"Velg rapporter" }
                            reports.forEach {
                                div {
                                    input {
                                        type = InputType.checkBox
                                        value = it.reportId
                                        text(it.descriptiveName ?: it.url)
                                    }
                                }
                            }
                        }
                    }*/
                }
            }

            get("new/reportlist") {
                fun wottevs() = createHTML().p { +"Shitt works" }
                call.respondText(
                    contentType = ContentType.Text.Html,
                    HttpStatusCode.OK, ::wottevs
                )
            }
        }
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