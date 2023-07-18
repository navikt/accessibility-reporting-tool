package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.Status
import accessibility.reporting.tool.wcag.SuccessCriterion
import accessibility.reporting.tool.wcag.Version1Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.css.*

fun HTMLTag.hxPost(url: String) {
    attributes["hx-post"] = url
}

fun HTMLTag.hxGet(url: String) {
    attributes["hx-get"] = url
}

fun HTMLTag.hxTarget(selector: String) {
    attributes["hx-target"] = selector
}

fun SELECT.a11yOption(status: Status) =
    option {
        if (status == Status.COMPLIANT) {
            selected = true
        }
        value = "compliant"
        +"compliant"
    }
fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")
fun HTMLTag.hxSwapOuter() {
    attributes["hx-swap"] = "outerHMTL"
}

fun FlowContent.a11yForm(sc: SuccessCriterion) {
    form(classes = "${sc.cssClass()}") {
        span { +"${sc.successCriterionNumber} ${sc.name}" }
        select {
            hxPost("/submit")
            hxTarget(".${sc.cssClass()}")
            attributes["name"] = "status"
            attributes["hx-vals"] = """{"id": "${sc.successCriterionNumber}"}"""
            a11yOption(sc.status)


            option {
                if (sc.status == Status.NON_COMPLIANT) {
                    selected = true
                }
                value = "non-compliant"
                +"non compliant"
            }

            option {
                if (sc.status == Status.NOT_TESTED) {
                    selected = true
                }
                value = "not-tested"
                +"not tested"
            }

            option {
                if (sc.status == Status.NOT_APPLICABLE) {
                    selected = true
                }
                value = "not-applicable"
                +"not applicable"
            }
        }

        span { +"${sc.status}" }
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
        get("/styles.css") {
            call.respondCss {
                body {
                }
                rule("h1.page-title") {

                }
                main {
                    display = Display.grid
                    gridTemplateColumns = GridTemplateColumns(1.fr)
                    gap = 20.px
                }
                form {
                    display = Display.flex
                    flex = Flex.GROW

                }
            }
        }

        post("/submit") {
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val report = Version1Report.successCriteriaV1.find { it.number == 1 }
            report?.let { foundReport ->
                fun response() = createHTML().main {
                    a11yForm(foundReport)
                }
                call.respondText(contentType = ContentType.Text.Html,
                    HttpStatusCode.OK, ::response)
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
                        rel = "preload"
                        href = "/styles.css"
                        attributes["as"] = "style"
                    }
                }
                body {
                    main {
                        Version1Report.successCriteriaV1.map { a11yForm(it) }
                    }
                }
            }
        }
    }
}
