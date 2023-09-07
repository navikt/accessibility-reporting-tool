package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.microfrontends.NavBarItem.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import kotlinx.html.*
import java.time.LocalDateTime

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
            navbar(navBarItem, user)
            contenbuilder()
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
            LOGG_UT.li(currentItem, this)
        }
    }
}

enum class NavBarItem(val itemHref: String, val itemText: String) {
    FORSIDE("/", "Forside"),
    ORG_ENHETER("/orgunit", "Organisasjonsenheter"),
    BRUKER("/user", "Dine erklæringer"),
    LOGG_UT("/oauth2/logout", "Logg ut"),
    ADMIN("/admin", "Admin"),
    NONE("", "");

    fun li(navBarItem: NavBarItem, ul: UL) =
        if (navBarItem == this@NavBarItem)
            ul.li { span { +itemText }}
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
