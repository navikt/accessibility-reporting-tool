package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.SuccessCriterion
import kotlinx.html.*

fun BODY.openReport(report: Report) {
    main(classes = "report-container") {
        header(classes = "report-header") {
            h1 {
                span {
                    +report.h1()
                    +" for "
                }

                span {
                    id = "report-titles"
                    +(report.descriptiveName ?: report.url)
                }
            }

            details {
                summary {
                    +"Vis/rediger metadata"
                }
                div(classes = "statement-metadata") {
                    dl {
                        //Tittel
                        //URL
                        //Sist oppdatert
                        //
                    }
                }
            }
        }

        nav(classes = "sc-toc") {
            attributes["aria-label"] = "Status"
            h2 {
                span {
                    +"Status: "
                }

                span {
                    id = "metadata-status"
                    hxOOB("true")
                    +report.statusString()
                }


            }

            summaryLinks(report)
        }

        div(classes = "sc-list") {
            h2 {
                +"Detaljert status"
            }
            report.successCriteria.map { succsessCriteriaSummary(it) }
        }

        a(classes = "to-top") {
            href = "#sc1.1.1"
            +"Til toppen"
        }
    }
}

fun DIV.succsessCriteriaSummary(successCriterion: SuccessCriterion){
    h3 { "${successCriterion.name } ${successCriterion.name}" }
    p { +successCriterion.describeFindings() }
}