package accessibility.reporting.tool

import accessibility.reporting.tool.NavBarItem.*
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.Status.NON_COMPLIANT
import accessibility.reporting.tool.wcag.Status.NOT_TESTED
import accessibility.reporting.tool.wcag.SuccessCriterion
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML

suspend fun ApplicationCall.respondHtmlContent(title: String, navBarItem: NavBarItem, contenbuilder: BODY.() -> Unit) {
    respondHtml {
        lang = "no"
        head {
            meta { charset = "UTF-8" }
            style {}
            title { +title }
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

        body {
            navbar(navBarItem)
            contenbuilder()
        }

    }
}

fun FlowContent.disclosureArea(
    sc: SuccessCriterion, reportId: String, text: String, summary: String, description: String, dataName: String
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

fun FIELDSET.statusRadio(sc: SuccessCriterion, value: String, status: Status, display: String) {
    div(classes = "radio-with-label") {
        input {
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

fun FlowContent.a11yForm(sc: SuccessCriterion, reportId: String) {
    successCriterionInformation(sc)
    form(classes = sc.cssClass()) {
        hxTrigger("change")
        hxPost("/reports/submit/${reportId}")
        hxTarget(".${sc.cssClass()}")
        hxSelect("form")
        hxSwapOuter()
        attributes["data-hx-vals"] = """{"index": "${sc.successCriterionNumber}"}"""
        div {
            fieldSet {
                attributes["name"] = "status"

                legend { +"Oppfyller alt innhold på siden kravet?" }
                statusRadio(sc, "compliant", Status.COMPLIANT, "Ja")
                statusRadio(sc, "non-compliant", Status.NON_COMPLIANT, "Nei")
                statusRadio(sc, "not-tested", NOT_TESTED, "Ikke testet")
                statusRadio(sc, "not-applicable", Status.NOT_APPLICABLE, "Vi har ikke denne typen innhold")
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
                    sc, reportId, sc.lawDoesNotApply, "Det er innhold i på siden som ikke er underlagt kravet.",

                    "Hvilket innhold er ikke underlagt kravet?", "law-does-not-apply"
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
    val generalCriteriaContent = successCriteria.first()

    div(classes = "criterion-status") {
        h2 { +"${generalCriteriaContent.number}:${generalCriteriaContent.name} (${generalCriteriaContent.wcagLevel})" }
        if (successCriteria.deviationCount() == 0) {
            p { +"Ingen avvik registrert" }
        } else {
            p { +"${successCriteria.deviationCount()} avvik registrert" }
            ul {
                successCriteria.filter { it.status == NON_COMPLIANT && it.breakingTheLaw.isNotEmpty() }
                    .map { it.breakingTheLaw }.let { criterionIssues("Det er innhold på siden som bryter kravet", it) }

                successCriteria.filter { it.status == NON_COMPLIANT && it.lawDoesNotApply.isNotEmpty() }
                    .map { it.lawDoesNotApply }
                    .let { criterionIssues("Det er innhold i på siden som ikke er underlagt kravet", it) }


                successCriteria.filter { it.status == NON_COMPLIANT && it.tooHardToComply.isNotEmpty() }
                    .map { it.tooHardToComply }.let {
                        criterionIssues(
                            "Innholdet er unntatt fordi det er en uforholdsmessig stor byrde å følge kravet.", it
                        )
                    }
            }
        }
    }
}

fun UL.criterionIssues(heading: String, issueList: List<String>) {
    if (issueList.isNotEmpty()) {
        li { +heading }
        ul {
            issueList.forEach { li { +it } }
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

fun DIV.statementMetadataDl(reportId: String, statuses: List<StatementMetadata>) {
    dl {
        statuses.forEach { metadata ->
            dt { +metadata.label }
            metadata.definitionItem(this, reportId)
        }
    }
}

fun DL.definitionInput(text: String, hxUpdateName: String, reportId: String, hxId: String?) {
    dd(classes = "editable-definition") {
        hxId?.let { hxId ->
            id = hxId
            hxOOB("true")
        }
        input {
            hxTrigger("change")
            hxPost("/reports/metadata/$reportId")
            type = InputType.text
            name = hxUpdateName
            value = text
        }
    }
}

fun DL.definitionItem(text: String, hxId: String?) {
    dd {
        hxId?.let { hxId ->
            id = hxId
            hxOOB("true")
        }
        +text
    }
}

class StatementMetadata(
    val label: String,
    val value: String?,
    private val ddProducer: (DL.(String) -> Unit)? = null,
    val hxId: String? = null,
    val hxUpdateName: String? = null
) {
    fun definitionItem(dl: DL, reportId: String) = when {
        ddProducer != null -> ddProducer.let { dl.it(reportId) }
        hxUpdateName != null -> dl.definitionInput(
            text = value!!,
            hxUpdateName = hxUpdateName,
            reportId = reportId,
            hxId = hxId
        )
        else -> dl.definitionItem(value!!, hxId)
    }
}

fun SuccessCriterion.cssClass() = "f" + this.successCriterionNumber.replace(".", "-")

fun BODY.navbar(currentItem: NavBarItem) {
    nav {
        id = "hovedmeny"
        attributes["aria-label"] = "Hovedmeny"
        ul {
            FORSIDE.li(currentItem, this)
            ORG_ENHETER.li(currentItem, this)
            BRUKER.li(currentItem, this)
            LOGG_UT.li(currentItem, this)
        }
    }
}

enum class NavBarItem(val itemHref: String, val itemText: String) {
    FORSIDE("/", "Forside"),
    ORG_ENHETER("/orgunit", "Organisasjonsenheter"),
    BRUKER("/user", "Dine erklæringer"),
    LOGG_UT("/oauth2/logout", "Logg ut"),
    NONE("", "");

    fun li(navBarItem: NavBarItem, ul: UL) =
        if (navBarItem == this@NavBarItem)
            ul.li { +itemText }
        else
            ul.hrefListItem(itemHref, itemText)
}

fun UL.hrefListItem(listHref: String, text: String) {
    li {
        a {
            href = listHref
            +text
        }
    }
}

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
}

fun summaryLinksString(report: Report) = createHTML().ul(classes = "summary") {
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
}


