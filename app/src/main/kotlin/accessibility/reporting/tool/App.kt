package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion
import accessibility.reporting.tool.wcag.Version1Report
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

fun FlowContent.disclosureArea(sc: SuccessCriterion, summary: String, dataName: String) {
    details {
        summary {
            +"${summary}"
        }
        div {
            textArea {
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
                legend { +"Oppfyller alt innhold i testsettet kravet?"}
                statusRadio(sc,"compliant", Status.COMPLIANT, "Ja")
                statusRadio(sc,"non compliant", Status.NON_COMPLIANT, "Nej")
                statusRadio(sc,"not tested", Status.NOT_TESTED, "Ikke testet")
                statusRadio(sc, "not applicable", Status.NOT_APPLICABLE, "Vi har ikke denne typen av innhold")
            }
        }
        if (sc.status == Status.NON_COMPLIANT) {
            div {
                disclosureArea(sc, "Det er innhold i testsettet som bryter kravet.", "breaking-the-law")
                disclosureArea(sc, "Det er innhold i testsettet som ikke er underlagt kravet.", "law-does-not-apply")
                disclosureArea(
                    sc,
                    "Innholdet er unntatt fordi det er en uforholdsmessig stor byrde å følge kravet.",
                    "too-hard-to-comply"
                )
            }
        }
    }
}


fun main() {
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
            val report = Version1Report.successCriteriaV1.find { it.successCriterionNumber == index }
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
                call.respond(HttpStatusCode.NotFound)
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
                        Version1Report.successCriteriaV1.map { a11yForm(it) }
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
