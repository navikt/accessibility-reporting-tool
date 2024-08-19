package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTimeOrNull
import accessibility.reporting.tool.wcag.ReportType.AGGREGATED
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.aggregate
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.UUID

class AggregatedReport : Report {

    var fromReports: List<ReportShortSummary>
    var fromOrganizations: List<OrganizationUnitShortSummary>

    constructor(
        url: String,
        descriptiveName: String,
        user: User,
        organizationUnit: OrganizationUnit?,
        reports: List<Report>,
        isPartOfNavNo: Boolean
    ) : super(
        reportId = UUID.randomUUID().toString(),
        url = url,
        descriptiveName = descriptiveName,
        organizationUnit = organizationUnit,
        version = Version.V4,
        author = user.toAuthor(),
        successCriteria = reports.map { report ->
            report.successCriteria.map { criterion ->
                SuccessCriterionSummary(
                    reportTitle = report.descriptiveName ?: url,
                    contactPerson = report.author.email,
                    content = criterion
                )
            }

        }.flatten().aggregate(),
        created = LocalDateTimeHelper.nowAtUtc(),
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        contributors = reports.map { it.contributors }.flatten().toMutableList(),
        lastUpdatedBy = null,
        reportType = AGGREGATED,
        isPartOfNavNo = isPartOfNavNo
    ) {
        fromReports =
            reports.map { ReportShortSummary(it.reportId, it.descriptiveName, it.url, it.reportType, it.lastChanged) }
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
        author = report.author,
        successCriteria = report.successCriteria,
        created = report.created,
        lastChanged = report.lastChanged,
        contributors = report.contributors,
        lastUpdatedBy = report.lastUpdatedBy,
        reportType = AGGREGATED,
        isPartOfNavNo = report.isPartOfNavNo
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

    fun updateWithDataFromSource(srcReport: Report?): AggregatedReport {
        if (srcReport == null) throw IllegalArgumentException("Kilderapport finnes ikke")
        val updatedFromReports = fromReports
            .filter { it.reportId != srcReport.reportId }
            .toMutableList()
            .apply {
                add(
                    ReportShortSummary(
                        reportId = srcReport.reportId,
                        descriptiveName = srcReport.descriptiveName,
                        url = srcReport.url,
                        reportType = srcReport.reportType,
                        lastChanged = srcReport.lastChanged
                    )
                )
            }
        return AggregatedReport(
            this.copy(
                reportId = reportId,
                successCriteria = (successCriteria + srcReport.successCriteria).map {
                    SuccessCriterionSummary(
                        reportTitle = srcReport.descriptiveName ?: srcReport.url,
                        contactPerson = srcReport.author.email,
                        content = it
                    )
                }.aggregate()
            ),
            updatedFromReports,
            fromOrganizations
        )
    }

    override fun toJson(): String =
        objectMapper.writeValueAsString(this).also {
            require(objectMapper.readTree(it)["fromReports"] != null)
            require(objectMapper.readTree(it)["fromOrganizations"] != null)
        }

    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        }

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
    val reportType: ReportType,
    val lastChanged: LocalDateTime
) : ReportContent {
    val title = descriptiveName ?: url
    fun hasUpdates(srcReports: List<ReportShortSummary>): Boolean =
        !wasDeleted(srcReports) &&
                srcReports.any { srcReport -> srcReport.reportId == this.reportId && srcReport.lastChanged.isAfter(this.lastChanged) }

    fun wasDeleted(srcReports: List<ReportShortSummary>) = srcReports.none { it.reportId == this.reportId }

    companion object {
        fun fromJson(jsonNode: JsonNode) = ReportShortSummary(
            jsonNode["reportId"].asText(),
            jsonNode["descriptiveName"].asText("Ikke tilgjengelig") ?: jsonNode["url"].asText(),
            jsonNode["url"].asText(),
            ReportType.valueFromJson(jsonNode),
            jsonNode["lastChanged"].takeIf { it != null }?.toLocalDateTimeOrNull() ?: LocalDateTimeHelper.nowAtUtc()
                .minusDays(30),

            )
    }
}

data class OrganizationUnitShortSummary(val id: String, val name: String)
