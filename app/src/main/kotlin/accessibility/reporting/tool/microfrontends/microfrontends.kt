package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.microfrontends.NavBarItem.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*
import java.time.LocalDateTime

suspend fun ApplicationCall.respondHtmlContent(
    title: String,
    navBarItem: NavBarItem,
    classes: String = "default-body-class",
    contentbuilder: MAIN.() -> Unit
) {
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
            link {
                rel = "icon"
                type = "image/x-icon"
                href = "/static/a11y-cat-round.png"
            }
        }

        body(classes) {
            navbar(navBarItem, user)
            main {
                contentbuilder()
            }
            div(classes="grey-box") {
                div{ +"Card"}
                div{ +"Card"}
                div{ +"Card"}
                div{ +"Card"}
                div{ +"Card"}
            }
            footer {
                +"very footer"
            }
        }

    }
}


fun BODY.navbar(currentItem: NavBarItem, user: User? = null) {
    nav {
        id = "hovedmeny"
        attributes["aria-label"] = "Hovedmeny"
        ul {
            FORSIDE.li(currentItem, this)
            ORG_ENHETER.li(currentItem, this)
            BRUKER.li(currentItem, this)
            if (user != null && Admins.isAdmin(user))
                ADMIN.li(currentItem, this)
            FAQ.li(currentItem, this)
            LOGG_UT.li(currentItem, this)
        }
        +"Inlogget som ${user?.email?.str()}"
    }
}

enum class NavBarItem(val itemHref: String, val itemText: String) {
    FORSIDE("/", "Forside"),
    ORG_ENHETER("/orgunit", "Organisasjonsenheter"),
    BRUKER("/user", "Dine erklæringer"),
    LOGG_UT("/oauth2/logout", "Logg ut"),
    ADMIN("/admin", "Admin"),
    FAQ("/faq", "FAQ"),
    NONE("", "");

    fun li(navBarItem: NavBarItem, ul: UL) =
        if (navBarItem == this@NavBarItem)
            ul.li { span { +itemText } }
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

fun LocalDateTime.displayFormat(): String = "$dayOfMonth.$monthValue.$year"

fun Route.faqRoute() {
    get("faq") {
        call.respondHtmlContent("FAQ", FAQ) {
            h1 { +"FAQ" }
            h2 { +"Hva er en organisasjonsenhet?" }
            p {
                +"""En organisasjonsenhet er teamet ditt eller evt annen del av organisasjonen du tilhører om du ikke er en del av ett team."""

            }
            p {
                +""" Det er ingen predefinerte organisasjonsenheter, så det må teamet/enheten lage selv.
                |Den som oppretter enheten kan legge inn sin egen e-mail og vil da få mulighet til å legge til andre.
                |Alle som tilhører en organisasjonsenhet kan redigere rapporter som tilhører enheten.""".trimMargin()
            }
            h2 { +"Kan man lagre rapporten og komme tilbake seinere?" }
            p {
                +"Ja. Rapporten lagres automatisk og det er ikke noe problem å begynne på en rapport og fortsette på den senere."
                +"Du kan også fortsette å redigere på rapporten selv om den er ferdigutfylt, f.eks hvis du har rettet opp en feil"
            }
            h2 { +"Kan flere jobbe på samme rapport?" }
            p { +"Ja, alle som er medlemmer av en organisasjonsenhet kan redigere rapporter som tilhører enheten." }
            p { +"Samtidighet er ikke på plass enda, så det vil si at det er viktig at den kun utfylles på 1 skjerm av gangen, hvis ikke risikerer en at ting bli overskrevet." }
            h2 { +"Må jeg sende rapporten noe sted?" }
            p { +"Nei! UU-teamet samler inn rapporten direkte, så det er ikke nødvendig å gjøre noe mer enn å fylle ut." }
            h2 { +"Hvor skal jeg si ifra dersom jeg har oppdaget ett problem?" }
            p {
                +"Legg inn et issue i "
                a {
                    href = "https://github.com/navikt/accessibility-reporting-tool/issues"
                    +"github-repoet"
                }
            }
            h2 { +"Andre spørsmål" }
            p { +"Om du har andre spørsmål kan du stille de i nav-uu kanalen på slack eller sende en mail til universel.utformning@nav.no" }
        }
    }
}
