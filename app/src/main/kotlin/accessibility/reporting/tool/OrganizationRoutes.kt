package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*


fun Route.organizationUnits(repository: ReportRepository) {

    get("orgunit/{id?}") {

        call.parameters["id"]?.let { unitId ->
            val (org, reports) = repository.getReportForOrganizationUnit(unitId)

            org?.let { orgUnit ->
                call.respondHtml {
                    body {
                        h1 { +"${orgUnit.name} accessibility reports" }
                        ul {
                            reports.forEach { report ->
                                li {
                                    a {
                                        href = "report/${report.reportId}"
                                        +"Rapport for ${report.url}"
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: run {
                call.respond(HttpStatusCode.NotFound)
            }
        } ?: run {
            call.respondHtml {
                body {
                    h1 { +"Organization units" }
                    ul {
                        repository.getAllOrganizationUnits().forEach { orgUnit ->
                            li {
                                a {
                                    href = "report/${orgUnit.id}"
                                    +"Rapporter for ${orgUnit.name}"
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

fun Route.userRoute(repository: ReportRepository) {
    get("user") {

        val reports = repository.getReportsForUser(call.user.email)
        call.respondHtml {
            head {
                headContent(call.user.email)
            }
            body {
                h1 { +"${call.user} accessibility reports" }
                ul {
                    reports.forEach { report ->
                        li {
                            a {
                                href = "report/${report.reportId}"
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

}