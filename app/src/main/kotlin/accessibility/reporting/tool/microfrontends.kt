package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion
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
fun FlowContent.disclosureArea(sc: SuccessCriterion, summary: String, description: String, dataName: String) {
    details {
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
                hxPost("/reports/submit")
                hxVals("""{"index": "${sc.successCriterionNumber}"}""")
                name = dataName
                cols = "80"
                rows = "10"
                placeholder = "Leave blank if you're not breaking the law"
            }

        }

    }
}

fun FIELDSET.statusRadio(sc: SuccessCriterion, value_: String, status: Status, display: String) {
    label {
        input {

            type = InputType.radio
            if (sc.status == status) {
                checked = true
            }
            value = value_
            name = "status"
        }
        +"${display}"
    }
}

fun FlowContent.a11yForm(sc: SuccessCriterion) {
    form(classes = "${sc.cssClass()}") {
        h2 { +"${sc.successCriterionNumber} ${sc.name}" }
        div {

            fieldSet {
                hxPost("/submit")
                hxTarget(".${sc.cssClass()}")
                hxSelect("form")
                hxSwapOuter()
                attributes["name"] = "status"
                attributes["hx-vals"] = """{"index": "${sc.successCriterionNumber}"}"""
                legend { +"Oppfyller alt innhold i testsettet kravet?" }
                statusRadio(sc, "compliant", Status.COMPLIANT, "Ja")
                statusRadio(sc, "non compliant", Status.NON_COMPLIANT, "Nej")
                statusRadio(sc, "not tested", Status.NOT_TESTED, "Ikke testet")
                statusRadio(sc, "not applicable", Status.NOT_APPLICABLE, "Vi har ikke denne typen av innhold")
            }
        }
        if (sc.status == Status.NON_COMPLIANT) {
            div {
                disclosureArea(
                    sc,
                    "Det er innhold i testsettet som bryter kravet.",
                    "Beskriv kort hvilket innhold som bryter kravet, hvorfor og konsekvensene dette får for brukeren.",
                    "breaking-the-law"
                )
                disclosureArea(
                    sc, "Det er innhold i testsettet som ikke er underlagt kravet.",
                    "Hvilket innhold er ikke underlagt kravet?", "law-does-not-apply"
                )
                disclosureArea(
                    sc,
                    "Innholdet er unntatt fordi det er en uforholdsmessig stor byrde å følge kravet.",
                    "Hvorfor mener vi at det er en uforholdsmessig stor byrde for innholdet å følge kravet?",
                    "too-hard-to-comply"
                )
            }
        }
    }
}
