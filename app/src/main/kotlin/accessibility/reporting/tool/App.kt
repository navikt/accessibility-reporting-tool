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


private var HTMLTag.hxPost(): String
    get() {this.}
    set() {}

fun main() {
        embeddedServer(Netty, port = 8080, module = Application::api).start(wait = true)
}

suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
fun Application.api(){
    routing {
        get("/isAlive"){
            call.respond(HttpStatusCode.OK)
        }
        get("/isReady"){
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

        get ("/index.html") {
            // attrs["hx-$method"] ="first"
            val html = createHTML()
            html.head {
                title{}
                script {}
            }
            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    meta { charset = "UTF-8" }
                    style {

                    }
                    title {}
                    script { src = "https://unpkg.com/htmx.org/dist/htmx.js" }
                }
                body {
                    main {
                        form {
                            //hxPost = "/submit"

                            attributes["hx-post"] ="/sumbit"
                            attributes["hx-target"] = "#formsList"
                            span { "Criterion number"}
                            span { "Criterion" }
                            select {
                                id = "status" name = "status" required
                                option value ="compliant">Compliant</option>
                                <option value ="non-compliant">Non-compliant</option>
                                <option value ="not-tested">Not tested</option>
                            }
                            <button type ="submit">Submit</button>
                        }
                    }
                }

            }


        }
    }
}
