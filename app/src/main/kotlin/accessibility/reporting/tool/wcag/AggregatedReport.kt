package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.ReportType.AGGREGATED
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.aggregate
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

class AggregatedReport : Report {

    var fromReports: List<ReportShortSummary>
    var fromOrganizations: List<OrganizationUnitShortSummary>

    constructor(
        url: String,
        descriptiveName: String,
        user: User,
        organizationUnit: OrganizationUnit?,
        reports: List<Report>
    ) : super(
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
        fromReports = reports.map { ReportShortSummary(it.reportId, it.descriptiveName, it.url, it.reportType) }
        fromOrganizations = reports
            .mapNotNull { it.organizationUnit?.let { org -> OrganizationUnitShortSummary(org.id, org.name) } }
    }

    constructor(
        report: Report,
        fromReports: List<ReportShortSummary>,
        fromOrganizations: List<OrganizationUnitShortSummary>
    ) : super(
        reportId = report.reportId,
        url = report.url,
        descriptiveName = report.descriptiveName,
        organizationUnit = report.organizationUnit,
        version = report.version,
        testData = report.testData,
        user = report.user,
        successCriteria = report.successCriteria,
        filters = mutableListOf(),
        created = report.created,
        lastChanged = report.lastChanged,
        contributers = report.contributers,
        lastUpdatedBy = report.lastUpdatedBy,
        reportType = AGGREGATED
    ) {
        this.fromReports = fromReports
        this.fromOrganizations = fromOrganizations
    }

    override fun withUpdatedCriterion(criterion: SuccessCriterion, updateBy: User): AggregatedReport =
        AggregatedReport(super.withUpdatedCriterion(criterion, updateBy), fromReports, fromOrganizations)

    override fun withUpdatedMetadata(
        title: String?,
        pageUrl: String?,
        organizationUnit: OrganizationUnit?,
        updateBy: User
    ): AggregatedReport =
        AggregatedReport(
            super.withUpdatedMetadata(title, pageUrl, organizationUnit, updateBy),
            fromReports,
            fromOrganizations
        )


    companion object {
        fun deserialize(version: Version, jsonData: JsonNode) =
            AggregatedReport(
                report = version.deserialize(jsonData)
                    .also { if (it.reportType != AGGREGATED) throw IllegalArgumentException("rapport av type ${it.reportType} kan ikke deserialiseres til AggregatedReport") },
                fromReports = jsonData["fromReports"].toList()
                    .map { ReportShortSummary.fromJson(it) },
                fromOrganizations = jsonData["fromOrganizations"].toList()
                    .map { OrganizationUnitShortSummary(it["id"].asText(), it["name"].asText()) },
            )
    }

}

class ReportShortSummary(
    override val reportId: String,
    override val descriptiveName: String?,
    override val url: String,
    val reportType: ReportType
) : ReportContent {
    companion object {
        fun fromJson(jsonNode: JsonNode) = ReportShortSummary(
            jsonNode["reportId"].asText(),
            jsonNode["descriptiveName"].asText(),
            jsonNode["url"].asText(),
            ReportType.valueFromJson(jsonNode)

        )
    }
}

data class OrganizationUnitShortSummary(val id: String, val name: String)