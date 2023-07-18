package accessibility.reporting.tool

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



fun HTMLTag.hxSwapOuter() {
    attributes["hx-swap"] = "outerHMTL"
}


fun FlowContent.a11yForm(status: String, section: String) {
    form(classes = section) {


        span { +"number" }
        span { +"Criterion" }

        select {
            hxPost("/submit")
            hxTarget(".$section")
            attributes["name"] = "status"

            option {
                if (status == "non-compliant") {
                    selected = true
                }
                value = "compliant"
                +"compliant"

            }
            option {
                if (status == "non-compliant") {
                    selected = true
                }
                value = "non-compliant"
                +"non compliant"
            }
            option {
                if (status == "not-tested") {
                    selected = true
                }
                value = "not-tested"
                +"not tested"
            }
        }

        span { +status }
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
                    backgroundColor = Color.darkBlue

                }
                rule("h1.page-title") {
                    color = Color.white
                }
                main {
                    backgroundColor = Color.red
                }
                form {
                    display =Display.flex

                }
        }
        }

        post("/submit") {
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()

            fun response() = createHTML().main {
                a11yForm(status, "cool-section")

            }

            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)

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
                }
                body {
                    main {
                        a11yForm("very good", "cool-section")
                    }
                }
            }
        }
    }
}
