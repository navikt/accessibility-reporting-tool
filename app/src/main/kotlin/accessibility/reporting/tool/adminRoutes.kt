package accessibility.reporting.tool.accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.server.application.*
import io.ktor.server.routing.*
import kotlinx.html.*

fun Route.adminRoutes(reportRepository: ReportRepository) {
    route("admin") {
        get {
            call.respondHtmlContent("Admin – Aggregerte erklæringer", NavBarItem.NONE) {
                h1 { +"Tilgjengelighetserklæring " }
                h2 { +"Genererte rapporter" }
                ul {
                    li { +"todo - aggregerte rapporter" }
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

            call.respondHtmlContent("Generer rapport", NavBarItem.ADMIN) {
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
                        id = "test"
                    }
                }
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