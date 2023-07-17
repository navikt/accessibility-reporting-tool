package accessibility.reporting.tool

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.css.*
import java.text.Normalizer.Form


fun HTMLTag.hxPost(url: String) {
    attributes["hx-post"] = url
}

fun HTMLTag.hxTarget(url: String) {
    attributes["hx-target"] = url
}

fun FlowContent.a11yForm(status: String) {
    form {
        id = "formsList"
        hxPost("/submit")
        hxTarget("#formsList")
        span { +"ALL GOOD number" }
        span { +"Criterion" }
        select {
            id = "status"
            attributes["name"] = "status"
            +"required"
            option {
                value = "compliant"
                +"compliant"
            }
            option {
                value = "non-compliant"
                +"non compliant"
            }
            option {
                value = "not-tested"
                +"not tested"
            }
        }
        button {
            type = ButtonType.submit
            +"Submit"
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

fun runHtml(html: HTML): String = html.toString()

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
            }
        }

        post("/submit") {

            fun response() = createHTML().main {
                a11yForm("very bad")

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
                }
                body {
                    main {
                        a11yForm("very good")
                    }
                }
            }
        }
    }
}
