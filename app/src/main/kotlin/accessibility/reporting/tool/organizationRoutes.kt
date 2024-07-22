package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML


fun Route.organizationUnits(organizationRepository: OrganizationRepository) {
    route("orgunit") {

        get {
            call.respondHtmlContent("Organisasjonsenheter", NavBarItem.ORG_ENHETER) {
                h1 { +"Organisasjonsenheter" }
                a(classes = "cta") {
                    href = "orgunit/new"
                    +"Legg til organisajonsenhet"
                }
                ul {
                    id = "orgunit-list"
                    organizationRepository.getAllOrganizationUnits().forEach { orgUnit ->
                        li {
                            a {
                                href = "orgunit/${orgUnit.id}"
                                +orgUnit.name
                            }
                            if (Admins.isAdmin(call.user)) {
                                button {
                                    hxDelete("orgunit/${orgUnit.id}")
                                    hxTarget("#orgunit-list")
                                    hxSwapOuter()
                                    hxConfirm("Vil du slette ${orgUnit.name}?")
                                    +"Slett organisasjonsenhet"
                                }
                            }
                        }
                    }
                }
            }
        }

        get("{id}") {

            call.parameters["id"]!!.let { unitId ->
                val (org, reports) = organizationRepository.getReportForOrganizationUnit<Report>(unitId)

                org?.let { orgUnit ->
                    call.respondHtmlContent(orgUnit.name, NavBarItem.ORG_ENHETER) {
                        h1 { +orgUnit.name }
                        div { ownerInfo(orgUnit, call.user) }
                        h2 { +"Medlemmer" }
                        div {
                            orgUnitMembersSection(orgUnit, call.user)
                        }

                        if (reports.isNotEmpty()) {
                            h2 { +"Tilgjengelighetserklæringer" }
                            ul { reports.forEach { report -> reportListItem(report) } }
                        } else p { +"${orgUnit.name} har ingen tilgjengelighetserklæringer enda" }
                    }
                } ?: run { call.respond(HttpStatusCode.NotFound) }
            }
        }

        delete("{id}") {
            val newOrgList =
                organizationRepository.deleteOrgUnit(call.parameters["id"] ?: throw IllegalArgumentException("orgid mangler"))

            fun response() = createHTML().ul {
                id = "orgunit-list"
                newOrgList.forEach { orgUnit ->
                    li {
                        a {
                            href = "orgunit/${orgUnit.id}"
                            +orgUnit.name
                        }
                        if (Admins.isAdmin(call.user))
                            button {
                                hxDelete("orgunit/${orgUnit.id}")
                                hxTarget("#orgunit-list")
                                hxSwapOuter()
                                hxConfirm("Vil du slette ${orgUnit.name}?")
                                +"Slett organisasjonsenhet"
                            }
                    }
                }
            }
            call.respondText(contentType = ContentType.Text.Html, HttpStatusCode.OK, ::response)
        }

        post("{id}/owner") {
            val params = call.receiveParameters()
            val orgUnit = call.parameters["id"]?.let {
                organizationRepository.getOrganizationUnit(it)
            } ?: throw IllegalArgumentException("Ukjent organisasjonenhetsid")
            val newOwnerEmail =
                params["orgunit-email"] ?: throw IllegalArgumentException("Mangler email til ny eier")
            organizationRepository.upsertOrganizationUnit(orgUnit.copy(email = newOwnerEmail))

            call.respondText(status = HttpStatusCode.OK) {
                createHTML().div {
                    ownerInfo(orgUnit.copy(email = newOwnerEmail), call.user)
                }
            }

        }

        route("member") {
            post() {
                val formParameters = call.receiveParameters()
                val orgUnit = organizationRepository
                    .getOrganizationUnit(
                        formParameters["orgunit"] ?: throw IllegalArgumentException("organisasjonsenhet-id mangler")
                    )
                    ?.apply {
                        addMember(formParameters["member"].toEmail())
                        organizationRepository.upsertOrganizationUnit(this)
                    }
                    ?: throw IllegalArgumentException("organisasjonsenhet finnes ikke")

                call.respondText(
                    contentType = ContentType.Text.Html,
                    status = HttpStatusCode.OK
                ) { createHTML().div { orgUnitMembersSection(orgUnit, call.user) } }
            }
            delete {
                val organizationUnit = organizationRepository.getOrganizationUnit(
                    call.parameters["orgunit"] ?: throw IllegalArgumentException("Mangler organisasjonsenhet-id")
                )
                    ?.apply {
                        removeMember(
                            call.parameters["email"] ?: throw IllegalArgumentException("Mangler brukers email")
                        )
                        organizationRepository.upsertOrganizationUnit(this)
                    }
                    ?: throw IllegalArgumentException("Fant ikke organisasjon")

                call.respondText(status = HttpStatusCode.OK, contentType = ContentType.Text.Html) {
                    createHTML().div {
                        orgUnitMembersSection(organizationUnit, call.user)
                    }
                }
            }
        }

        route("new") {
            get {
                call.respondHtmlContent("Legg til organisasjonsenhet", NavBarItem.ORG_ENHETER) {
                    h1 { +"Legg til organisasjonsenhet" }
                    form {
                        hxPost("/orgunit/new")
                        label {
                            htmlFor = "text-input-name"
                            +"Navn"
                        }
                        input {
                            id = "text-input-name"
                            name = "unit-name"
                            type = InputType.text
                            required = true
                        }
                        label {
                            htmlFor = "input-email"
                            +"email"
                        }
                        input {
                            id = "input-email"
                            name = "unit-email"
                            type = InputType.email
                            required = true
                        }

                        button {
                            +"opprett enhet"
                        }
                    }
                }
            }

            post {
                val params = call.receiveParameters()
                val email = params["unit-email"] ?: throw IllegalArgumentException("Organisasjonsenhet må ha en email")
                val name = params["unit-name"] ?: throw IllegalArgumentException("Organisasjonsenhet må ha ett navn")

                organizationRepository.upsertOrganizationUnit(
                    OrganizationUnit.createNew(
                        name = name,
                        email = email
                    )
                )

                call.response.headers.append("HX-Redirect", "/orgunit")
                call.respond(HttpStatusCode.Created)
            }
        }
    }
}

fun String?.toEmail(): User.Email =
    this?.let { User.Email(this) } ?: throw IllegalArgumentException("mailadresse kan ikke være null")

private fun DIV.orgUnitMembersSection(orgUnit: OrganizationUnit, user: User) {
    id = "member-list-container"
    if (orgUnit.members.isNotEmpty()) {
        ul {
            id = "member-list"
            orgUnit.members.forEach {
                li {
                    +it
                    if (orgUnit.hasWriteAccess(user)) {
                        button {
                            hxTarget("#member-list-container")
                            hxDelete("/orgunit/member?email=$it&orgunit=${orgUnit.id}")
                            hxTrigger("click")
                            +"Fjern fra organisasjon"
                        }
                    }
                }
            }
        }
    } else {
        p {
            +"Denne enheten har ingen medlemmer"
        }
    }
    if (orgUnit.hasWriteAccess(user)) {
        form {
            hxPost("/orgunit/member")
            hxTarget("#member-list-container")
            label {
                htmlFor = "new-member-input"
                +"Legg til medlem"
            }
            input {
                id = "new-member-input"
                name = "member"
                type = InputType.email
                placeholder = "xxxx@nav.no"
                required = true
            }
            input {
                type = InputType.hidden
                name = "orgunit"
                value = orgUnit.id
            }

            button {
                type = ButtonType.submit
                +"Legg til medlem"
            }
        }
    }
}

fun DIV.ownerInfo(orgUnit: OrganizationUnit, user: User) {
    id = "orgunit-owner"
    p {
        +"epost: ${orgUnit.email}"
    }
    if (orgUnit.hasWriteAccess(user)) {
        form {
            input {
                type = InputType.text
                required = true
                placeholder = "new owner"
                name = "orgunit-email"
            }
            button {
                hxPost("${orgUnit.id}/owner")
                hxTarget("#orgunit-owner")
                +"bytt eier"
            }
        }
    }
}