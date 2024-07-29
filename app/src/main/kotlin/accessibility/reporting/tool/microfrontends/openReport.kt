package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.criteria.Status
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion
import accessibility.reporting.tool.wcag.report.Report
import kotlinx.html.*
import java.time.format.DateTimeFormatter

fun BODY.openReport(report: Report) {
    main {
        header {
            h1 {
                id="top"
                span {
                    id = "report-titles"
                    +(report.descriptiveName ?: report.url)
                }
            }
            dl(classes = "statement-metadata") {
                dt { +"Sist oppdatert av "}
                dd { +"${report.lastUpdatedBy?.email}" }
                dt { +"Sist oppdatert"}
                dd { +"${report.lastChanged.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}" }
            }
        }

        div (classes = "open-report") {
            attributes["aria-label"] = "Status"
            h2 {
                div {
                    +"Status: "
                }
                div {
                    id = "metadata-status"
                    hxOOB("true")
                    +report.statusString()
                }
            }
            openSummaryLinks(report)
        }
        a(classes = "to-top") {
            href = "#top"
            +"Til toppen"
        }
    }
}

fun FlowContent.openSummaryLinks(report: Report) = div(classes = "summary") {
    hxOOB("outerHTML:.summary")
    val sortedCriteria = report.successCriteria.groupBy { it.status }

    h3 { +"Ikke testet" }
    sortedSummaryLinks(sortedCriteria[Status.NOT_TESTED])

    h3 { +"Avvik" }
    sortedSummaryLinks(sortedCriteria[Status.NON_COMPLIANT])

    h3 { +"Ikke aktuelt" }
    sortedSummaryLinks(sortedCriteria[Status.NOT_APPLICABLE])

    h3 { +"OK" }
    sortedSummaryLinks(sortedCriteria[Status.COMPLIANT])
}

private fun DIV.sortedSummaryLinks(sortedCriteria: List<SuccessCriterion>?) {
    if (sortedCriteria.isNullOrEmpty()) {
        p { +"Ingen kriterier" }
    } else {
        sortedCriteria?.apply {
            ul {
                forEach {
                    li {
                        h4 {
                            +"${it.number} ${it.name}"
                        }
                        p {
                            +"${it.describeFindings()}"
                        }
                    }
                }
            }
        }
    }
}
