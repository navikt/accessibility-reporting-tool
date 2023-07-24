package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.html.*

import java.lang.IllegalArgumentException

private val ApplicationCall.user: String
    get() = principal<User>()?.username ?: "testuser"

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

        val reports = repository.getReportForUser(call.user)

            call.respondHtml {
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

                    a("")
                }
            }
        }

}