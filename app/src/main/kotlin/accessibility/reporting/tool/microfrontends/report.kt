package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportContent
import kotlinx.html.*

fun BODY.reportContainer(
    report: Report,
    organizations: List<OrganizationUnit>,
    updateCriterionUrl: String,
    updateMetadataUrl: String,
    user: User,
    metadataProducer: MutableList<StatementMetadata>.() -> Unit = {}
) {
    val readOnly = !report.writeAccess(user)
    main(classes = "report-container") {

        header(classes = "report-header") {
            h1 {

                span {
                    +report.h1()
                    +" for "
                }

                span {
                    id = "report-titles"
                    hxOOB("true")
                    +(report.descriptiveName ?: report.url)
                }
            }

            details {
                summary {
                    +"Vis/rediger metadata"
                }
                div(classes = "statement-metadata") {
                    statementMetadataDl(
                        readOnly = readOnly,
                        reportId = report.reportId,
                        mutableListOf<StatementMetadata>().apply {
                            add(
                                StatementMetadata(
                                    "Tittel",
                                    report.descriptiveName ?: report.url,
                                    hxUpdateName = "report-title",
                                    updatePath = updateMetadataUrl
                                )
                            )
                            add(
                                StatementMetadata(
                                    "URL",
                                    report.url,
                                    hxUpdateName = "page-url",
                                    updatePath = updateMetadataUrl
                                )
                            )
                            add(StatementMetadata("Ansvarlig", report.author.email, null))

                            add(
                                StatementMetadata(
                                    label = "Sist oppdatert",
                                    value = report.lastChanged.displayFormat(),
                                    hxId = "metadata-oppdatert"
                                )
                            )
                            StatementMetadata(
                                label = "Sist oppdatert av",
                                value = (report.lastUpdatedBy ?: report.author).email,
                                hxId = "metadata-oppdatert-av"
                            )

                            report.contributors.let {
                                if (it.isNotEmpty())
                                    StatementMetadata("Bidragsytere", it.joinToString { "," })
                            }
                            add(
                                StatementMetadata(
                                    label = "Organisasjonsenhet",
                                    value = report.organizationUnit?.name,
                                    ddProducer = {
                                        dd {
                                            select {
                                                orgSelector(organizations, report, updateMetadataUrl)
                                            }
                                        }
                                    })
                            )
                            metadataProducer()
                        }
                    )
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
                +"Registrer funn"
            }
            report.successCriteria.map { a11yForm(it, report.reportId, updateCriterionUrl, readOnly) }
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
