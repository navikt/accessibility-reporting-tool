package accessibility.reporting.tool

import accessibility.reporting.tool.database.Environment
import accessibility.reporting.tool.database.Flyway
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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
import kotliquery.param
import java.lang.IllegalArgumentException

fun SuccessCriterion.cssClass() =
    "f" + this.successCriterionNumber.replace(".", "-")

val testOrg = OrganizationUnit("carls-awesome-test-unit", "Carls awesome test unit")

fun main() {
    val environment = Environment()
    Flyway.runFlywayMigrations(Environment())
    val repository = ReportRepository(PostgresDatabase(environment)).also {reportRepository ->
        //id som kan brukes når du skal sette opp rapporter: "carls-awesome-test-unit"

        reportRepository.insertOrganizationUnit(OrganizationUnit("carls-awesome-test-unit", "Carls awesome test unit"))

    }

    embeddedServer(Netty, port = 8080, module = {this.api(repository) { installAzure() } }).start(wait = true)
}

fun Application.installAzure() {
    install(Authentication) {
        jwt {}
    }
}
suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    this.respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
}

fun Application.api(repository: ReportRepository, authInstaller: Application.() -> Unit )  {
    authInstaller()
    routing {
        get("/isAlive") {
            call.respond(HttpStatusCode.OK)
        }
        get("/isReady") {
            call.respond(HttpStatusCode.OK)
        }

        get("/reports") {}
        post("/submit/{id}") {
            // 1 is a good id?
            val id = call.parameters["id"]?: throw IllegalArgumentException()
            val formParameters = call.receiveParameters()
            val status = formParameters["status"].toString()
            val index = formParameters["index"].toString()
            val filters = listOf(formParameters["multimedia-filter"],
                formParameters["form-filter"],
                formParameters["timelimit-filter"],
                formParameters["interaction-filter"]).map { it.toString() }

            val report = ReportV1.successCriteriaV1.find { it.successCriterionNumber == index }
            report?.let { foundReport ->

                if (status == "non compliant") {
                    // .div because I cannot find a .fragment or similar.
                    // This means that you have to hx-select on the other end
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
                    headContent("Select reports")
                }
                body {
                    h1 { +"Select a report"}
                    a { href="report/foo"
                        +"there's only this one, sir"
                    }
                }
            }
        }
        get("/report/{id}") {
            val id = call.parameters["id"]?: throw IllegalArgumentException()
            val report = repository.getReport(id)?: Report.createLatest("url", testOrg, "foo", null  )

            call.respondHtml(HttpStatusCode.OK) {
                lang = "no"
                head {
                    headContent("A11y report")
                }
                body {
                    p {+"${report.organizationUnit.name}"}
                    main {
                        h1 { +"A11y report" }
                        p { +"Hvem fyller ut rapporten?"}
                        p { +"Fyller du ut rapporten på vegne av et annet team?"}
                        p { +"Kontaktperson fra det andre teamet"}
                        h2 {+"Om løsningen" }
                        p { +"Hva heter løsningen?" }
                        p { +"Løsningens base-URL" }
                        p { +"(For PoC'en) URLen som er testet"}
                        div {
                            label {
                                +"Url:"
                                input { type = InputType.text }
                            }
                            label {

                                input {
                                    type = InputType.checkBox
                                    value = "multimedia-filter"
                                    name = "multimedia-filter"
                                    attributes["data-removes"] = """ removes
                                1.2.1 Bare lyd og bare video
                                1.2.2 Teksting (forhåndsinnspilt)
                                1.2.3 Synstolking eller mediealternativ (forhåndsinnspilt)
                                1.2.5 Synstolking (forhåndsinnspilt)
                                1.4.2 Styring av lyd
                                2.3.1 Terskelverdi på maksimalt tre glimt
                                """
                                }
                                +"Multimedia: Har sidene du skal teste multimedia eller innhold som flasher, f.eks. video, lydfiler, animasjoner?"
                            }
                            label {

                                input {
                                    type = InputType.checkBox
                                    name = "form-filter"
                                    value = "form-filter"
                                    attributes["data-removes"] = """ removes

                             1.3.5 Identifiser formål med inndata
                             2.5.3 Ledetekst i navn
                             3.2.2 Inndata
                             3.3.1 Identifikasjon av feil
                             3.3.2 Ledetekster eller instruksjoner
                             3.3.3 Forslag ved feil
                             3.3.4 Forhindring av feil
                            """
                                }
                                +"Skjemaer: Har løsningen din skjemafelter (utenom i dekoratøren), eller mottar løsningen inndata fra brukeren?"
                            }

                            label {

                                input {
                                    type = InputType.checkBox
                                    name = "interaction-filter"
                                    value = "interaction-filter"
                                    attributes["data-removes"] = """ removes
                        2.1.4 Hurtigtaster som består av ett tegn
                        2.5.1 Pekerbevegelser
                        2.5.4 Bevegelsesaktivering
                        """
                                }
                                +"Interaksjonsmønstre: Har du bevegelsesaktivert innhold, hurtigtaster, eller gestures?"
                            }
                            label {

                                input {
                                    type = InputType.checkBox
                                    value = "timelimit-filter"
                                    name = "timelimit-filter"
                                    attributes["data-removes"] = """ removes
                                 2.2.1 Justerbar hastighet
                                 2.2.2 Pause, stopp, skjul
                               """
                                }
                                +"Tidsbegrensninger og innhold som oppdaterer seg automatisk: Har du innhold med tidsbegrensning? F.eks. automatisk utlogging, begrenset tid til å ta en quiz."
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
