package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportContent
import kotlinx.html.*

fun BODY.reportContainer(
    report: Report,
    organizations: List<OrganizationUnit>,
    updateCriterionUrl: String,
    updateMetadataUrl: String,
    metadaProducer: MutableList<StatementMetadata>.()->Unit={}
) {
    main(classes = "report-container") {
        header(classes = "report-header") {
            h1 { +report.h1() }
            div(classes = "statement-metadata") {
                statementMetadataDl(report.reportId,
                    mutableListOf<StatementMetadata>().apply {
                        add(
                            StatementMetadata(
                                "Tittel",
                                report.descriptiveName ?: report.url,
                                hxUpdateName = "report-title",
                                updatePath = updateMetadataUrl
                            )
                        )
                        add(StatementMetadata("URL", report.url, hxUpdateName = "page-url", updatePath = updateMetadataUrl))
                        add(StatementMetadata("Ansvarlig", report.user.email, null))
                        add(StatementMetadata("Status", report.statusString(), hxId = "metadata-status"))
                        add(
                            StatementMetadata(
                                label = "Sist oppdatert",
                                value = report.lastChanged.displayFormat(),
                                hxId = "metadata-oppdatert"
                            )
                        )
                        StatementMetadata(
                            label = "Sist oppdatert av",
                            value = (report.lastUpdatedBy ?: report.user).email,
                            hxId = "metadata-oppdatert-av"
                        )

                        report.contributers.let {
                            if (it.isNotEmpty())
                                StatementMetadata("Bidragsytere", it.joinToString { "," })
                        }
                        add(StatementMetadata(label = "Organisasjonsenhet", value = null, ddProducer = {
                            dd {
                                select {
                                    orgSelector(organizations, report, updateMetadataUrl)
                                }
                            }
                        }))
                        metadaProducer()
                    }

                )


            }
        }

        nav(classes = "sc-toc") {
            attributes["aria-label"] = "Status"
            summaryLinks(report)
        }

        div(classes = "sc-list") {
            report.successCriteria.map { a11yForm(it, report.reportId, updateCriterionUrl) }
        }

        a(classes = "to-top") {
            href = "#sc1.1.1"
            +"Til toppen"
        }
    }
}

fun UL.reportListItem(
    report: ReportContent,
    allowDelete: Boolean = false,
    rootPath: String = "/reports",
    deletePath: String? = null
) {
    li {
        a {
            href = "$rootPath/${report.reportId}"
            +(report.descriptiveName ?: report.url)
        }
        if (allowDelete)
            button {
                hxDelete("${deletePath ?: rootPath}/${report.reportId}")
                hxSwapOuter()
                hxConfirm("Er du sikker på at du vill slette denne erklæringen?")
                hxTarget(".report-list")
                +"Slett"
            }
    }
}
