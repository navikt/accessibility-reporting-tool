package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.microfrontends.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML


fun Route.organizationUnits(repository: ReportRepository) {
    route("orgunit") {

        get {
            call.respondHtmlContent("Organisasjonsenheter", NavBarItem.ORG_ENHETER) {
                h1 { +"Organisasjonsenheter" }
                a(classes = "cta") {
                    href = "orgunit/new"
                    +"Legg til organisajonsenhet"
                }
                ul {
                    id="orgunit-list"
                    repository.getAllOrganizationUnits().forEach { orgUnit ->
                        li {
                            a {
                                href = "orgunit/${orgUnit.id}"
                                +orgUnit.name
                            }
                            if(Admins.isAdmin(call.user))
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

        get("{id}") {

            call.parameters["id"]!!.let { unitId ->
                val (org, reports) = repository.getReportForOrganizationUnit(unitId)

                org?.let { orgUnit ->
                    call.respondHtmlContent(orgUnit.name, NavBarItem.ORG_ENHETER) {
                        h1 { +orgUnit.name }
                        p {
                            +"epost: ${orgUnit.email}"
                        }

                        h2 { +"Medlemmer" }
                        div {
                            orgUnitMembersSection(orgUnit)
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
            val newOrgList = repository.deleteOrgUnit(call.parameters["id"]?:throw IllegalArgumentException("orgid mangler"))

            fun response() = createHTML().ul {
                id="orgunit-list"
                newOrgList.forEach { orgUnit ->
                    li {
                        a {
                            href = "orgunit/${orgUnit.id}"
                            +orgUnit.name
                        }
                        if(Admins.isAdmin(call.user))
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

        route("member") {
            post() {
                val formParameters = call.receiveParameters()
                val orgUnit = repository
                    .getOrganizationUnit(
                        formParameters["orgunit"] ?: throw IllegalArgumentException("organisasjonsenhet-id mangler")
                    )
                    ?.apply {
                        addMember(formParameters["member"].toString())
                        repository.upsertOrganizationUnit(this)
                    }
                    ?: throw IllegalArgumentException("organisasjonsenhet finnes ikke")

                call.respondText(
                    contentType = ContentType.Text.Html,
                    status = HttpStatusCode.OK
                ) { createHTML().div { orgUnitMembersSection(orgUnit) } }
            }
            delete {
                val organizationUnit = repository.getOrganizationUnit(
                    call.parameters["orgunit"] ?: throw IllegalArgumentException("Mangler organisasjonsenhet-id")
                )
                    ?.apply {
                        removeMember(
                            call.parameters["email"] ?: throw IllegalArgumentException("Mangler brukers email")
                        )
                        repository.upsertOrganizationUnit(this)
                    }
                    ?: throw IllegalArgumentException("Fant ikke organisasjon")

                call.respondText(status = HttpStatusCode.OK, contentType = ContentType.Text.Html) {
                    createHTML().div {
                        orgUnitMembersSection(organizationUnit)
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

                repository.upsertOrganizationUnit(
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

private fun DIV.orgUnitMembersSection(orgUnit: OrganizationUnit) {
    id = "member-list-container"
    if (orgUnit.members.isNotEmpty()) {
        ul {
            id = "member-list"
            orgUnit.members.forEach {
                li {
                    +it
                    button {
                        hxTarget("#member-list-container")
                        hxDelete("/orgunit/member?email=$it&orgunit=${orgUnit.id}")
                        hxTrigger("click")
                        +"Fjern fra organisasjon"
                    }
                }
            }
        }
    } else {
        p {
            +"Denne enheten har ingen medlemmer"
        }
    }
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

