package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.util.UUID

class AggregatedReport private constructor(
    reportId: String,
    url: String,
    descriptiveName: String?,
    organizationUnit: OrganizationUnit?,
    version: Version,
    testData: TestData?,
    user: User,
    successCriteria: List<SuccessCriterion>,
    filters: MutableList<String> = mutableListOf(),
    created: LocalDateTime,
    lastChanged: LocalDateTime,
    contributers: MutableList<User> = mutableListOf(),
    lastUpdatedBy: User?
) : Report(
    reportId,
    url,
    descriptiveName,
    organizationUnit,
    version,
    testData,
    user,
    successCriteria,
    filters,
    created,
    lastChanged,
    contributers,
    lastUpdatedBy,
    ReportType.AGGREGATED
) {
    lateinit var fromReportIds: List<String>
    lateinit var fromOrganizationUnits: List<String>

    companion object {
        fun aggregate(reports: List<Report>, user: User, descriptiveName: String) = AggregatedReport(
            reportId = UUID.randomUUID().toString(),
            url = reports.joinToString(separator = ",") { it.url },
            descriptiveName = descriptiveName,
            organizationUnit = null,
            version = Version.V1,
            testData = null,
            user = user,
            successCriteria = reports.aggregateCriteria(),
            filters = mutableListOf(),
            created = LocalDateTimeHelper.nowAtUtc(),
            lastChanged = LocalDateTimeHelper.nowAtUtc(),
            contributers = reports.map { it.contributers }.flatten().toSet().toMutableList(),
            lastUpdatedBy = null,
        ).apply {
            fromReportIds = reports.map { it.reportId }
            fromOrganizationUnits = reports.mapNotNull { it.organizationUnit?.id }
        }

        fun fromReport(report: Report, fromReportIds: List<String>, fromOrganizationUnits: List<String>) =
            AggregatedReport(
                reportId = report.reportId,
                url = report.url,
                descriptiveName = report.descriptiveName,
                organizationUnit = report.organizationUnit,
                version = report.version,
                testData = report.testData,
                user = report.user,
                successCriteria = report.successCriteria,
                filters = report.filters,
                created = report.created,
                lastChanged = report.lastChanged,
                contributers = report.contributers,
                lastUpdatedBy = report.lastUpdatedBy
            ).apply {
                this.fromOrganizationUnits = fromOrganizationUnits
                this.fromReportIds = fromReportIds
            }
    }
}

private fun List<Report>.aggregateCriteria(): List<SuccessCriterion> =
    this
        .map { it.successCriteria }
        .flatten()
        .groupBy { it.number }
        .values
        .map { successcriteriaGroup ->
            val templateCriterion = successcriteriaGroup.first()
            SuccessCriterion(
                name = templateCriterion.name,
                description = templateCriterion.number,
                principle = templateCriterion.principle,
                guideline = templateCriterion.guideline,
                tools = templateCriterion.tools,
                number = templateCriterion.number,
                breakingTheLaw = successcriteriaGroup
                    .map { if (it.breakingTheLaw.isNotEmpty()) it.breakingTheLaw }
                    .joinToString("\n"),
                lawDoesNotApply = successcriteriaGroup
                    .map { if (it.lawDoesNotApply.isNotEmpty()) it.lawDoesNotApply }
                    .joinToString("\n"),
                tooHardToComply = successcriteriaGroup
                    .map { if (it.tooHardToComply.isNotEmpty()) it.tooHardToComply }
                    .joinToString("\n"),
                contentGroup = templateCriterion.contentGroup,
                status = Status.resolveStatus(successcriteriaGroup),
                wcagUrl = templateCriterion.wcagUrl,
                helpUrl = templateCriterion.helpUrl,
                wcagVersion = templateCriterion.wcagVersion
            ).apply { wcagLevel = templateCriterion.wcagLevel }
        }

