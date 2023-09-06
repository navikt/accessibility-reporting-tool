package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.Status.*
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

class AggregatedReport(
    url: String,
    descriptiveName: String,
    user: User,
    organizationUnit: OrganizationUnit?,
    reports: List<Report>
) : Report(
    reportId = UUID.randomUUID().toString(),
    url = url,
    descriptiveName = descriptiveName,
    organizationUnit = organizationUnit,
    version = Version.V1,
    testData = null,
    user = user,
    successCriteria = reports.map { it.successCriteria }.flatten().aggregate(),
    filters = mutableListOf(),
    created = LocalDateTimeHelper.nowAtUtc(),
    lastChanged = LocalDateTimeHelper.nowAtUtc(),
    contributers = reports.map { it.contributers }.flatten().toMutableList(),
    lastUpdatedBy = null
) {
    var fromReports: List<FromReport>
    var fromOrganizationUnits: List<FromOrganizationUnit>


    init {
        fromReports = reports.map { FromReport(reportId, descriptiveName) }
        fromOrganizationUnits = reports
            .mapNotNull { it.organizationUnit?.let { org -> FromOrganizationUnit(org.id, org.name) } }
    }

    companion object {
        fun deserialize(version: Version, jsonData: JsonNode) =
            (version.deserialize(jsonData) as AggregatedReport).apply {
                fromReports = jsonData["fromReports"].toList()
                    .map { FromReport(it["reportId"].asText(), it["descriptiveName"].asText()) }
                fromOrganizationUnits = jsonData["fromOrganizationUnits"].toList()
                    .map { FromOrganizationUnit(it["id"].asText(), it["name"].asText()) }
            }
    }
}

private fun List<SuccessCriterion>.aggregate(): List<SuccessCriterion> =
    groupBy { it.number }
        .map { it.value.combine() }


private fun List<SuccessCriterion>.combine(): SuccessCriterion =
    first().let { template ->
        SuccessCriterion(
            name = template.name,
            description = template.description,
            principle = template.principle,
            guideline = template.guideline,
            tools = template.tools,
            number = template.number,
            breakingTheLaw = mapNotNull { it.breakingTheLaw.ifBlank { null } }.joinToString("\n"),
            lawDoesNotApply = mapNotNull { it.lawDoesNotApply.ifBlank { null } }.joinToString("\n"),
            tooHardToComply = mapNotNull { it.tooHardToComply.ifBlank { null } }.joinToString("\n"),
            contentGroup = template.contentGroup,
            status = resolveStatus(),
            wcagUrl = template.wcagUrl,
            helpUrl = template.helpUrl,
            wcagVersion = template.wcagVersion
        )
    }


private fun List<SuccessCriterion>.resolveStatus(): Status =
    when {
        all { it.status == NOT_TESTED } -> NOT_TESTED
        any { it.status == NON_COMPLIANT } -> NON_COMPLIANT
        all { it.status == NOT_APPLICABLE } -> NOT_APPLICABLE
        all { it.status == COMPLIANT } -> COMPLIANT
        else -> {
            log.warn { "Could not resolve status for successcriterium ${first().number}" }
            NOT_TESTED
        }
    }


data class FromReport(val reportId: String, val descriptiveName: String)
data class FromOrganizationUnit(val id: String, val name: String)