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
    val hxUpdateName: String? = null,
    private val updatePath: String? = null
) {

    fun definitionItem(dl: DL, reportId: String) = when {
        ddProducer != null -> ddProducer.let { dl.it(reportId) }
        hxUpdateName != null -> {
            require(updatePath != null) { "updatepath må være satt for inputelement $hxUpdateName" }
            require(value != null) { "updatepath må være satt for inputelement $hxUpdateName" }
            dl.definitionInput(
                text = value,
                hxUpdateName = hxUpdateName,
                reportId = reportId,
                hxId = hxId,
                updatePath = updatePath,
                label = label
            )

        }

        else -> dl.definitionItem(value!!, hxId)
    }
}


fun DIV.statementMetadataDl(readOnly: Boolean,reportId: String, metadata: List<StatementMetadata>) {
    if (readOnly) readOnlyMetadata(metadata)
    else
        dl {
            metadata.forEach { metadata ->
                dt { +metadata.label }
                metadata.definitionItem(this, reportId)
            }
        }
}

fun DIV.readOnlyMetadata(metadata: List<StatementMetadata>) {
    dl {
        metadata.forEach { metadata ->
                dt { +metadata.label }
                dl { +(metadata.value?:"Ikke satt") }
        }
    }
}


fun DL.definitionInput(text: String, hxUpdateName: String, reportId: String, hxId: String?, updatePath: String, label: String) {
    dd(classes = "editable-definition") {
        hxId?.let { hxId ->
            id = hxId
            hxOOB("true")
        }
        input {
            attributes["aria-label"] = label
            hxTrigger("change")
            hxPost("$updatePath/${reportId}")
            type = InputType.text
            name = hxUpdateName
            id = hxUpdateName
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
    createHTML().span {
        id = "metadata-status"
        hxOOB("true")
        +report.statusString()
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

fun SELECT.orgSelector(organizations: List<OrganizationUnit>, report: Report, updateUrl: String) {
    name = "org-selector"
    attributes["aria-label"] = "Organisasjonsenhet"
    hxTrigger("change")
    hxPost("$updateUrl/${report.reportId}")
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