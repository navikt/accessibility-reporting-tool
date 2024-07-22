package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import accessibility.reporting.tool.wcag.Report.Companion.currentVersion
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime


object ReportVersions {
    /**
     * Changelog: v1 -> V2 field[user -> author] V2 -> V3 field
     * deleted[filters] field deleted[testData] (fields not in use) Assumptions
     * v3: fields descriptiveName, created and author is always present
     */
    fun migrateFromJsonVersion1(jsonNode: JsonNode) = deserialize(
        jsonNode = jsonNode,
        descriptiveName = jsonNode.descriptiveNameOrDefault,
        author = Author.fromJsonOrNull(jsonNode, "user")!!,
        lastChanged = jsonNode.lastChangedOrDefault(),
        created = jsonNode.createdOrDefault(),
    )

    fun migrateFromJsonVersion2(jsonNode: JsonNode): Report = deserialize(
        jsonNode = jsonNode,
        lastChanged = jsonNode.lastChangedOrDefault(),
        descriptiveName = jsonNode.descriptiveNameOrDefault,
        created = jsonNode.createdOrDefault(),
        author = Author.fromJsonOrNull(jsonNode, "author")!!
    )

    fun fromJsonVersion3(jsonNode: JsonNode) = deserialize(
        jsonNode = jsonNode,
        descriptiveName = jsonNode.descriptiveName,
        author = Author.fromJson(jsonNode, "author"),
        created = jsonNode["created"].toLocalDateTime(),
        lastChanged = jsonNode.lastChangedOrDefault(),
    )


    private fun deserialize(
        jsonNode: JsonNode,
        lastChanged: LocalDateTime,
        descriptiveName: String,
        created: LocalDateTime,
        author: Author
    ) = Report(
        reportId = jsonNode.reportId,
        url = jsonNode.url,
        descriptiveName = descriptiveName,
        organizationUnit = jsonNode.organizationUnit(),
        version = currentVersion,
        author = author,
        successCriteria = jsonNode.mapCriteria(lastChanged, currentVersion),
        lastChanged = lastChanged,
        created = created,
        lastUpdatedBy = Author.fromJsonOrNull(jsonNode, "lastUpdatedBy"),
        reportType = ReportType.valueFromJson(jsonNode)
    )

}

enum class Version(
    val deserialize: (JsonNode) -> Report,
    val criteria: List<SuccessCriterion>,
    val updateCriteria: (SuccessCriterion) -> SuccessCriterion
) {
    V1(ReportVersions::migrateFromJsonVersion1, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion),
    V2(ReportVersions::migrateFromJsonVersion2, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion),
    V3(ReportVersions::fromJsonVersion3, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion);
}