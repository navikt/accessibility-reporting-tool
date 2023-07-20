package accessibility.reporting.tool

import accessibility.reporting.tool.database.Environment
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportV1
import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.css.*

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
                hxPost("/submit")
                hxVals("""{"index": "${sc.successCriterionNumber}"}""")
                name = dataName
                cols = "80"
                rows = "10"
                placeholder = "Leave blank if you're not breaking the law"
            }

        }

    }
}

fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

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


fun main() {
    val environment = Environment()
    Flyway.runFlywayMigrations(Environment())
    ReportRepository(PostgresDatabase(environment)).also {reportRepository ->
        //id som kan brukes når du skal sette opp rapporter: "carls-awesome-test-unit"
        reportRepository.insertOrganizationUnit(OrganizationUnit("carls-awesome-test-unit", "Carls awesome test unit"))

    }

    embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)

}

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

fun Application.api() {
    routing {
        get("/isAlive") {
            call.respond(HttpStatusCode.OK)
        }
        get("/isReady") {
            call.respond(HttpStatusCode.OK)
        }

        post("/submit") {
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val index = formParameters["index"].toString()
            val report = ReportV1.successCriteriaV1.find { it.successCriterionNumber == index }
            report?.let { foundReport ->

                if (status == "non compliant") {
                    fun response() = createHTML().div {
                        a11yForm(foundReport)
                    }
                    call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
                } else {
                    fun response() = createHTML().div {
                        a11yForm(foundReport)
                    }
                    call.respondText(
                        contentType = ContentType.Text.Html,
                        HttpStatusCode.OK, ::response
                    )
                }
            } ?: run {
                call.respond(HttpStatusCode.NotFound, "ENOENT")
            }
        }
        get("/index.html") {
            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    meta { charset = "UTF-8" }
                    style {
                    }
                    title { +"Accessibility reporting" }
                    script { src = "https://unpkg.com/htmx.org/dist/htmx.js" }

                    link {
                        rel = "preload"
                        href = "https://cdn.nav.no/aksel/@navikt/ds-css/2.9.0/index.min.css"
                        attributes["as"] = "style"
                    }
                    link {
                        rel = "stylesheet"
                        href = "static/style.css"

                    }
                }
                body {
                    main {
                        h1 { +"A11y report" }
                        p { +"Hvem fyller ut rapporten?"}
                        p { +"Fyller du ut rapporten på vegne av et annet team?"}
                        p { +"Kontaktperson fra det andre teamet"}
                        h2 {+"Om løsningen" }
                                p { +"Hva heter løsningen?" }
                        p { +"Løsningens base-URL" }
                        p { +"(For PoC'en) URLen som er testet"}
                        label {
                            +"Url:"
                            input { type = InputType.text }
                        }
                        label {
                            +"Multimedia: Har sidene du skal teste multimedia eller innhold som flasher, f.eks. video, lydfiler, animasjoner?"
                            input {
                                type = InputType.checkBox
                                value = """ removes
                                1.2.1 Bare lyd og bare video
                                1.2.2 Teksting (forhåndsinnspilt)
                                1.2.3 Synstolking eller mediealternativ (forhåndsinnspilt)
                                1.2.5 Synstolking (forhåndsinnspilt)
                                1.4.2 Styring av lyd
                                2.3.1 Terskelverdi på maksimalt tre glimt
                                """
                            }
                        }
                        label {
                            +"Skjemaer: Har løsningen din skjemafelter (utenom i dekoratøren), eller mottar løsningen inndata fra brukeren?"
                            input {
                                type = InputType.checkBox
                                value = """ removes

                             1.3.5 Identifiser formål med inndata
                             2.5.3 Ledetekst i navn
                             3.2.2 Inndata
                             3.3.1 Identifikasjon av feil
                             3.3.2 Ledetekster eller instruksjoner
                             3.3.3 Forslag ved feil
                             3.3.4 Forhindring av feil
                            """
                            }
                        }

                        label {
                            +"Interaksjonsmønstre: Har du bevegelsesaktivert innhold, hurtigtaster, eller gestures?"
                            input {
                                type = InputType.checkBox
                                value = """ removes
                        2.1.4 Hurtigtaster som består av ett tegn
                        2.5.1 Pekerbevegelser
                        2.5.4 Bevegelsesaktivering
                        """
                            }
                        }
                        label {
                            +"Tidsbegrensninger og innhold som oppdaterer seg automatisk: Har du innhold med tidsbegrensning? F.eks. automatisk utlogging, begrenset tid til å ta en quiz."
                            input {
                                type = InputType.checkBox
                                value = """ removes
                                 2.2.1 Justerbar hastighet
                                 2.2.2 Pause, stopp, skjul
                               """
                            }
                        }
                        ReportV1.successCriteriaV1.map { a11yForm(it) }
                    }
                }
            }
        }
        staticResources("/static", "static") {
            default("index.html")
            preCompressed(CompressedFileType.GZIP)
        }
    }
}
