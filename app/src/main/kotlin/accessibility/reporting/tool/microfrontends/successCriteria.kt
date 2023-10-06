package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion
import kotlinx.html.*
import kotlinx.html.stream.createHTML

fun FlowContent.disclosureArea(
    readOnly: Boolean,
    sc: SuccessCriterion,
    reportId: String,
    text: String,
    summary: String,
    description: String,
    dataName: String
) {
    details {
        open = text.isNotEmpty()
        summary {
            +summary
        }
        div {
            label {
                htmlFor = "${sc.successCriterionNumber}-${dataName}"
                +description
            }
            textArea {
                disabled = readOnly
                id = "${sc.successCriterionNumber}-${dataName}"
                hxTrigger("keyup changed delay:1500ms")
                hxPost("/reports/submit/${reportId}")
                hxVals("""{"index": "${sc.successCriterionNumber}"}""")
                name = dataName
                cols = "80"
                rows = "10"
                +text
            }
        }
    }
}

fun FIELDSET.statusRadio(sc: SuccessCriterion, value: String, status: Status, display: String, readOnly: Boolean) {
    div(classes = "radio-with-label") {
        input {
            disabled = readOnly
            id = "${sc.number}-${value}"
            type = InputType.radio
            if (sc.status == status) {
                checked = true
            }
            this.value = value
            name = "status"
        }
        label {
            htmlFor = "${sc.number}-${value}"
            +display
        }
    }
}

fun FlowContent.a11yForm(sc: SuccessCriterion, reportId: String, updatePath: String, readOnly: Boolean = false) {
    if (!updatePath.startsWith("/")) throw IllegalArgumentException("updatePath for successcriterion må starte med '/'")
    successCriterionInformation(sc)
    form(classes = sc.cssClass()) {
        hxTrigger("change")
        hxPost("$updatePath/${reportId}")
        hxTarget(".${sc.cssClass()}")
        hxSelect("form")
        hxSwapOuter()
        attributes["data-hx-vals"] = """{"index": "${sc.successCriterionNumber}"}"""
        div {
            fieldSet {
                attributes["name"] = "status"

                legend { +"Oppfyller alt innhold på siden kravet?" }
                statusRadio(sc, "compliant", Status.COMPLIANT, "Ja", readOnly)
                statusRadio(sc, "non-compliant", Status.NON_COMPLIANT, "Nei", readOnly)
                statusRadio(sc, "not-tested", Status.NOT_TESTED, "Ikke testet", readOnly)
                statusRadio(sc, "not-applicable", Status.NOT_APPLICABLE, "Vi har ikke denne typen innhold", readOnly)
            }
        }
        if (sc.status == Status.NON_COMPLIANT) {
            div {
                disclosureArea(
                    readOnly,
                    sc,
                    reportId,
                    sc.breakingTheLaw,
                    "Det er innhold på siden som bryter kravet.",
                    "Beskriv kort hvilket innhold som bryter kravet, hvorfor og konsekvensene dette får for brukeren.",
                    "breaking-the-law"
                )
                disclosureArea(
                    readOnly,
                    sc,
                    reportId,
                    sc.lawDoesNotApply,
                    "Det er innhold i på siden som ikke er underlagt kravet.",

                    "Hvilket innhold er ikke underlagt kravet?",
                    "law-does-not-apply"
                )
                disclosureArea(
                    readOnly,
                    sc,
                    reportId,
                    sc.tooHardToComply,
                    "Innholdet er unntatt fordi det er en uforholdsmessig stor byrde å følge kravet.",
                    "Hvorfor mener vi at det er en uforholdsmessig stor byrde for innholdet å følge kravet?",
                    "too-hard-to-comply"
                )
            }
        }
    }
}

private fun FlowContent.successCriterionInformation(sc: SuccessCriterion) {
    div(classes = "report-info") {
        h2 {
            id = "sc${sc.number}"
            +"${sc.number} ${sc.name}"
        }
        p { +sc.description }

        (sc.helpUrl ?: sc.wcagUrl)?.let { helpUrl ->
            a {
                href = helpUrl
                target = "_blank"
                rel = "noopener noreferrer"
                +if (sc.helpUrl != null) {
                    "Hvordan teste (åpner i ny fane)"
                } else {
                    "WCAG definisjon (åpner i ny fane)"
                }
            }
        }

    }
}

fun SuccessCriterion.cssClass() = "f" + this.successCriterionNumber.replace(".", "-")

/*
fun FlowContent.summaryLinks(report: Report) = ul(classes = "summary") {
    hxOOB("outerHTML:.summary")
    report.successCriteria.forEach {
        li {
            a {
                href = "#sc${it.number}"
                unsafe { +toIcon(it) }
                +"${it.number} ${it.name}"
            }
        }
    }
}*/

fun FlowContent.summaryLinks(report: Report) = div(classes = "summary") {
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
    sortedCriteria?.apply {
        ul {
            forEach {
                li {
                    a {
                        href = "#sc${it.number}"
                        unsafe { +toIcon(it) }
                        +"${it.number} ${it.name}"
                    }
                }
            }
        }
    }
}

fun summaryLinksString(report: Report) = createHTML().div(classes = "summary") {
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