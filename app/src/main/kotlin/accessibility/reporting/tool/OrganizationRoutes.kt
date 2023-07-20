package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

import java.lang.IllegalArgumentException

fun Routing.organizationUnits(repository: ReportRepository) {

    get("orgunit/{id}") {

        call.parameters["id"]?.let { unitId ->
            val (org, reports) = repository.getReportForOrganizationUnit(unitId)

            org?.let { orgUnit ->
                call.respondHtml {
                    body {
                        h1 { +"$orgUnit accessibility reports" }
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