package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun Route.admin(reportRepository: ReportRepository) {
    route("admin") {
        get {
            call.respondHtmlContent("Status for hele NAV", NavBarItem.NONE) {
                h1 { +"Admin verktøy" }
                a {
                    href = "/status"
                    +"Status for hele NAV"
                }
                h2 { +"Aggregerte rapporter" }
                reportList(reportRepository.getAggregatedReports())
                a {
                    href = "/admin/aggregated/new"
                    +"Legg til ny aggregert rapport"
                }
            }

        }
        route("aggregated") {
            route("new") {
                get {
                    val reports = reportRepository.getReports()
                    val orgs = reportRepository.getAllOrganizationUnits()

                    call.respondHtmlContent("Rapport aggregering", NavBarItem.NONE) {
                        h1 { +"Lag ny rapport" }
                        form {
                            orgCheckboxGroup(true, orgs)
                        }

                        form {
                            id = "reports"
                            reportSelector(reports)

                            unsafe { "..." }
                        }
                    }
                }
            }
            get("org/filter") {
                val x = call.parameters.getAll("org") ?: emptyList<String>()
                print(x)
                val filteredReports = reportRepository.getReports()
                    .filter { (it.organizationUnit != null) && x.contains(it.organizationUnit.id) }

                fun response() = createHTML().form {
                    reportSelector(filteredReports)
                }
                call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
            }
            get("org/alle") {
                val orgs = reportRepository.getAllOrganizationUnits()
                fun response() = createHTML().form {
                    orgCheckboxGroup(call.request.queryParameters["org-selector-all"] != null, orgs)
                }
                call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
            }
        }
    }

    get("status") {
        val groupedCriteria = reportRepository.getReports()
            .map { it.successCriteria }
            .flatten()
            .groupBy { it.number }
            .map { it.value }

        call.respondHtmlContent("Status for hele NAV", NavBarItem.NONE) {
            h1 { +"Status for hele NAV" }
            groupedCriteria.forEach { criterionStatus(it) }
        }
    }


}

private fun FORM.reportSelector(reports: List<Report>) {
    fieldSet {
        legend { +"Velg rapporter" }
        reports.forEach { report -> reportCheckbox(report) }
    }

}

private fun FORM.orgCheckboxGroup(allIsChecked: Boolean, orgs: List<OrganizationUnit>) {
    id = "org-selection"
    hxTrigger("change from:.org-check")
    attributes["hx-get"] = "org/filter"
    attributes["hx-target"] = "#reports"
    fieldSet {
        legend { +"Velg organisasjonsenhet(er)" }
        input {
            attributes["hx-get"] = "org/alle"
            attributes["hx-target"] = "#org-selection"
            name = "org-selector-all"
            type = InputType.checkBox
            value = "true"
            checked = allIsChecked
            +"Alle"
        }
        orgs.forEach {
            input(classes = "org-check") {
                name = "org"
                type = InputType.checkBox
                value = it.id
                checked = allIsChecked
                +it.name
            }
        }
    }


}


private fun FIELDSET.reportCheckbox(report: Report) {
    input {
        name = "report"
        type = InputType.checkBox
        value = report.reportId
        +(report.descriptiveName ?: report.url)
    }
}
