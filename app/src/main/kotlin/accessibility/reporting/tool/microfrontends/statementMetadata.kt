package accessibility.reporting.tool.microfrontends

import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import kotlinx.html.*
import kotlinx.html.stream.createHTML


class StatementMetadata(
    val label: String,
    val value: String?,
    private val ddProducer: (DL.(String) -> Unit)? = null,
    val hxId: String? = null,
    val hxUpdateName: String? = null
) {
    fun definitionItem(dl: DL, reportId: String) = when {
        ddProducer != null -> ddProducer.let { dl.it(reportId) }
        hxUpdateName != null -> dl.definitionInput(
            text = value!!,
            hxUpdateName = hxUpdateName,
            reportId = reportId,
            hxId = hxId
        )

        else -> dl.definitionItem(value!!, hxId)
    }
}


fun DIV.statementMetadataDl(reportId: String, statuses: List<StatementMetadata>) {
    dl {
        statuses.forEach { metadata ->
            dt { +metadata.label }
            metadata.definitionItem(this, reportId)
        }
    }
}

fun DL.definitionInput(text: String, hxUpdateName: String, reportId: String, hxId: String?) {
    dd(classes = "editable-definition") {
        hxId?.let { hxId ->
            id = hxId
            hxOOB("true")
        }
        input {
            hxTrigger("change")
            hxPost("/reports/metadata/$reportId")
            type = InputType.text
            name = hxUpdateName
            value = text
        }
    }
}

fun DL.definitionItem(text: String, hxId: String?) {
    dd {
        hxId?.let { hxId ->
            id = hxId
            hxOOB("true")
        }
        +text
    }
}

fun updatedMetadataStatus(report: Report): String = """
    ${
    createHTML().dd {
        id = "metadata-status"
        hxOOB("true")
        +"${report.statusString()}"
    }
}    
    
    ${
    createHTML().dd {
        id = "metadata-oppdatert"
        hxOOB("true")
        +report.lastChanged.displayFormat()
    }
}
    
    ${
    createHTML().dd {
        id = "metadata-oppdatert-av"
        hxOOB("true")
        +(report.lastUpdatedBy ?: report.user).email
    }
}""".trimMargin()

fun SELECT.orgSelector(organizations: List<OrganizationUnit>, report: Report) {
    name = "org-selector"
    hxTrigger("change")
    hxPost("/reports/organization/${report.reportId}")
    hxSwapOuter()
    organizations
        .filter { it.id != report.organizationUnit?.id }
        .forEach {
            option {
                selected = false
                value = it.id
                text(it.name)
            }
        }
    report.organizationUnit?.let {
        option {
            value = it.id
            selected = true
            text(it.name)
        }
    }
        ?: option {
            disabled = true
            selected = true
            text("Ingen organisasjonsenhet valgt")
        }
}