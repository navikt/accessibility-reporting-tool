package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.Status.NON_COMPLIANT
import accessibility.reporting.tool.wcag.Status.NOT_TESTED
import accessibility.reporting.tool.wcag.SuccessCriterion
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import kotlinx.html.*


fun HEAD.headContent(title: String) {
    meta { charset = "UTF-8" }
    style {
    }
    title { +"${title}" }
    script { src = "https://unpkg.com/htmx.org/dist/htmx.js" }

    link {
        rel = "preload"
        href = "https://cdn.nav.no/aksel/@navikt/ds-css/2.9.0/index.min.css"
        attributes["as"] = "style"
    }
    link {
        rel = "stylesheet"
        href = "/static/style.css"

    }
}

fun FlowContent.disclosureArea(
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
            +"${summary}"
        }
        div {
            label {
                htmlFor = "${sc.successCriterionNumber}-${dataName}"
                +"${description}"
            }
            textArea {
                id = "${sc.successCriterionNumber}-${dataName}"
                hxTrigger("keyup changed delay:1500ms")
                hxPost("/reports/submit/${reportId}")
                hxVals("""{"index": "${sc.successCriterionNumber}"}""")
                name = dataName
                cols = "80"
                rows = "10"
                +"${text}"
            }
        }
    }
}

fun FIELDSET.statusRadio(sc: SuccessCriterion, value_: String, status: Status, display: String) {
    div(classes = "radio-with-label") {
        input {
            id = "${sc.number}-${value_}"
            type = InputType.radio
            if (sc.status == status) {
                checked = true
            }
            value = value_
            name = "status"
        }
        label {
            htmlFor = "${sc.number}-${value_}"
            +display
        }
    }
}

fun FlowContent.a11yForm(sc: SuccessCriterion, reportId: String) {
    successCriterionInformation(sc)
    form(classes = sc.cssClass()) {
        attributes["data-hx-trigger"] = "change from: .${sc.cssClass()} label"
        hxPost("/reports/submit/${reportId}")
        hxTarget(".${sc.cssClass()}")
        hxSelect("form")
        hxSwapOuter()
        attributes["hx-vals"] = """{"index": "${sc.successCriterionNumber}"}"""
        div {
            fieldSet {
                attributes["name"] = "status"

                legend { +"Oppfyller alt innhold på siden kravet?" }
                statusRadio(sc, "compliant", Status.COMPLIANT, "Ja")
                statusRadio(sc, "non compliant", Status.NON_COMPLIANT, "Nei")
                statusRadio(sc, "not tested", NOT_TESTED, "Ikke testet")
                statusRadio(sc, "not applicable", Status.NOT_APPLICABLE, "Vi har ikke denne typen innhold")
            }
        }
        if (sc.status == Status.NON_COMPLIANT) {
            div {
                disclosureArea(
                    sc,
                    reportId,
                    sc.breakingTheLaw,
                    "Det er innhold på siden som bryter kravet.",
                    "Beskriv kort hvilket innhold som bryter kravet, hvorfor og konsekvensene dette får for brukeren.",
                    "breaking-the-law"
                )
                disclosureArea(
                    sc,
                    reportId,
                    sc.lawDoesNotApply,
                    "Det er innhold i på siden som ikke er underlagt kravet.",

                    "Hvilket innhold er ikke underlagt kravet?",
                    "law-does-not-apply"
                )
                disclosureArea(
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

fun BODY.criterionStatus(successCriteria: List<SuccessCriterion>) {
    val notTestedCount = successCriteria.count { it.status != NOT_TESTED }
    val generalCriteriaContent = successCriteria.first()

    div {
        h2 { +"${generalCriteriaContent.number}:${generalCriteriaContent.name} (${generalCriteriaContent.wcagLevel})" }
        if (successCriteria.deviationCount() == 0) {
            p { +"Ingen avvik registrert" }
            if(notTestedCount!=0){
                p{ +"$notTestedCount gjenstår" }
            }
        } else {
            successCriteria.filter { it.status == NON_COMPLIANT && it.breakingTheLaw.isNotEmpty() }
                .map { it.breakingTheLaw }.let {
                    h3 { +"Det er innhold på siden som bryter kravet" }
                    ul {
                        it.forEach { li { +it } }
                    }
                }

            successCriteria.filter { it.status == NON_COMPLIANT && it.lawDoesNotApply.isNotEmpty() }
                .map { it.lawDoesNotApply }.let {
                    h3 { +"Det er innhold i på siden som ikke er underlagt kravet" }
                    ul {
                        it.forEach { li { +it } }
                    }
                }

            successCriteria.filter { it.status == NON_COMPLIANT && it.tooHardToComply.isNotEmpty() }
                .map { it.tooHardToComply }.let {
                    h3 { +"Innholdet er unntatt fordi det er en uforholdsmessig stor byrde å følge kravet." }
                    ul {
                        it.forEach { li { +it } }
                    }
                }
        }
    }
}

private fun FlowContent.successCriterionInformation(sc: SuccessCriterion) {
    div(classes = "report-info") {
        h2 { +"${sc.number} ${sc.name}" }
        p { +sc.description }
        sc.helpUrl?.let {
            a {
                href = it
                target = "_blank"
                rel = "noopener noreferrer"
                +"Hvordan teste (åpner i ny fane)"
            }
        }
        sc.wcagUrl?.let {
            a {
                href = it
                target = "_blank"
                rel = "noopener noreferrer"
                +"WCAG definisjon (åpner i ny fane)"

            }
        }
    }
}

internal fun DIV.statementMetadata(label: String, value: String) {
    p {
        span(classes = "bold") { +"$label: " }
        span { +value }
    }
}

fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

fun BODY.navbar() {
    nav {
        ul {
            hrefListItem("/", "Forside")
            hrefListItem("/orgunit", "Organisasjonsenheter")
            hrefListItem("/user", "Dine erklæringer")
            hrefListItem("/oauth2/logout", "Logg ut")
        }
    }
}

fun UL.hrefListItem(listHref: String, text: String) {
    li {
        a {
            href = listHref
            +text
        }
    }

}