package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.ReportType.AGGREGATED
import accessibility.reporting.tool.wcag.Status.*
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.aggregate
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
    lastUpdatedBy = null,
    reportType = AGGREGATED
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

data class FromReport(val reportId: String, val descriptiveName: String)
data class FromOrganizationUnit(val id: String, val name: String)