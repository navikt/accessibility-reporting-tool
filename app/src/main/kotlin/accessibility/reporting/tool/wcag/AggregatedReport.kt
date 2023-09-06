package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.ReportType.AGGREGATED
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
    var fromReports: List<ReportShortSummary>
    var fromOrganizations: List<OrganizationUnitShortSummary>


    init {
        fromReports = reports.map { ReportShortSummary(reportId, descriptiveName) }
        fromOrganizations = reports
            .mapNotNull { it.organizationUnit?.let { org -> OrganizationUnitShortSummary(org.id, org.name) } }
    }

    companion object {
        fun deserialize(version: Version, jsonData: JsonNode) =
            (version.deserialize(jsonData) as AggregatedReport).apply {
                fromReports = jsonData["fromReports"].toList()
                    .map { ReportShortSummary(it["reportId"].asText(), it["descriptiveName"].asText()) }
                fromOrganizations = jsonData["fromOrganizationUnits"].toList()
                    .map { OrganizationUnitShortSummary(it["id"].asText(), it["name"].asText()) }
            }
    }
}

data class ReportShortSummary(val reportId: String, val descriptiveName: String)
data class OrganizationUnitShortSummary(val id: String, val name: String)