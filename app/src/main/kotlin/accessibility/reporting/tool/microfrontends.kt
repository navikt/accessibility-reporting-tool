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
    div(classes = "radiogroup") {
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
    form(classes = sc.cssClass()) {
        h2 { +sc.name }
        p {
            +sc.description
        }
        div {
            fieldSet {
                hxPost("/reports/submit/${reportId}")
                hxTarget(".${sc.cssClass()}")
                hxSelect("form")
                hxSwapOuter()
                attributes["name"] = "status"
                attributes["hx-vals"] = """{"index": "${sc.successCriterionNumber}"}"""
                legend { +"Oppfyller alt innhold på siden kravet?" }
                statusRadio(sc, "compliant", Status.COMPLIANT, "Ja")
                statusRadio(sc, "non compliant", Status.NON_COMPLIANT, "Nei")
                statusRadio(sc, "not tested", Status.NOT_TESTED, "Ikke testet")
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


fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

fun BODY.navbar(email:String) {
    nav {
        ul {
            li {
                a {
                    href = "/"
                    +"Forside"
                }
            }
            li {
                a {
                    href = "/orgunit"
                    +"Organisasjonsenheter"
                }
            }
            li {
                a {
                    href = "/user"
                    +"Dine rapporter"
                }
            }
            li {
                a {
                    href = "/oauth2/logout"
                    +"Logg ut ($email)"
                }
            }
        }
    }
}
