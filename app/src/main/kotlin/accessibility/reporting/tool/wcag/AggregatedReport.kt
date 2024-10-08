package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTimeOrNull
import accessibility.reporting.tool.rest.SuccessCriterionUpdate
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
        notes: String,
    ) : super(
        reportId = UUID.randomUUID().toString(),
        url = url,
        descriptiveName = descriptiveName,
        organizationUnit = organizationUnit,
        version = Version.V5,
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
        notes = notes,
        isPartOfNavNo = false
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
        isPartOfNavNo = report.isPartOfNavNo,
        notes = report.notes
    ) {
        this.fromReports = fromReports
        this.fromOrganizations = fromOrganizations
    }

    override fun withUpdatedCriterion(criterion: SuccessCriterion, updateBy: User): AggregatedReport =
        AggregatedReport(super.withUpdatedCriterion(criterion, updateBy), fromReports, fromOrganizations)


    override fun withUpdatedMetadata(
        title: String?,
        pageUrl: String?,
        notes: String?,
        organizationUnit: OrganizationUnit?,
        updateBy: User,
    ): AggregatedReport =
        AggregatedReport(
            super.withUpdatedMetadata(
                title = title,
                pageUrl = pageUrl,
                notes = notes,
                updateBy = updateBy,
                organizationUnit = organizationUnit
            ),
            fromReports,
            fromOrganizations
        )

    fun updatedWith(
        title: String?,
        pageUrl: String?,
        notes: String?,
        updateBy: User,
        changedCriteria: List<SuccessCriterionUpdate>?
    ): AggregatedReport =
        AggregatedReport(
            Report(
                reportId = this.reportId,
                url = pageUrl ?: this.url,
                descriptiveName = title ?: this.descriptiveName,
                organizationUnit = this.organizationUnit,
                version = version,
                author = author,
                successCriteria = changedCriteria?.let { copyCriteriaList(changedCriteria) } ?: this.successCriteria,
                created = created,
                lastChanged = LocalDateTimeHelper.nowAtUtc(),
                contributors = contributors,
                lastUpdatedBy = updateBy.toAuthor(),
                isPartOfNavNo = false,
                reportType = AGGREGATED,
                notes = notes ?: this.notes
            ),
            fromReports,
            fromOrganizations
        )

    private fun copyCriteriaList(updatedValues: List<SuccessCriterionUpdate>): List<SuccessCriterion> {
        val newList = this.successCriteria.toMutableList()
        updatedValues.forEach { updatedCriterion ->
            val index = newList.indexOfFirst { it.number == updatedCriterion.number }
            if (index == -1) {
                throw IllegalArgumentException(
                    """Update criterion error: could not find criterion ${updatedCriterion.number}: 
                           in ${newList.joinToString(",") { it.number }}""".trimIndent()
                )
            } else {
                val currentCriterion = this.successCriteria[index]
                newList[index] = currentCriterion.copy(
                    status = updatedCriterion.status?.let { Status.valueOf(it) }?:currentCriterion.status,
                    breakingTheLaw = updatedCriterion.breakingTheLaw?:currentCriterion.breakingTheLaw,
                    lawDoesNotApply = updatedCriterion.lawDoesNotApply?:currentCriterion.lawDoesNotApply,
                    tooHardToComply = updatedCriterion.tooHardToComply?:currentCriterion.tooHardToComply
                ).apply { this.wcagLevel = currentCriterion.wcagLevel }
            }
        }
        return newList
    }

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
