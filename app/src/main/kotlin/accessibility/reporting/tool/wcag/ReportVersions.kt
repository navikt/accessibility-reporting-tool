package accessibility.reporting.tool.wcag

import com.fasterxml.jackson.databind.JsonNode


object ReportVersions {
    /**
     * Changelog:
     * v1 -> V2
     * field[user -> author]
     * V2 -> V3 field
     * deleted[filters] field deleted[testData] (fields not in use)
     * Assumptions v3: fields descriptiveName, and created always present
     */
    fun migrateFromJsonVersion1(jsonNode: JsonNode): Report {
        val lastChanged = jsonNode.lastChangedOrDefault()
        return Report(
            reportId = jsonNode.reportId,
            url = jsonNode.url,
            descriptiveName = jsonNode.descriptiveNameOrDefault,
            organizationUnit = jsonNode.organizationUnit(),
            version = Version.V2,
            reportType = jsonNode.reportType,
            author = Author.fromJson(jsonNode, "user")!!,
            successCriteria = jsonNode.mapCriteria(lastChanged),
            lastChanged = lastChanged,
            created = jsonNode.createdOrDefault(),
            lastUpdatedBy = jsonNode.lastUpdatedBy
        )
    }

    fun fromJsonVersion2(jsonNode: JsonNode): Report {
        val lastChanged = jsonNode.lastChangedOrDefault()
        return Report(
            reportId = jsonNode.reportId,
            url = jsonNode.url,
            descriptiveName = jsonNode.descriptiveNameOrDefault,
            organizationUnit = jsonNode.organizationUnit(),
            version = Version.V2,
            author = Author.fromJson(jsonNode, "author")!!,
            successCriteria = jsonNode.mapCriteria(lastChanged),
            lastChanged = lastChanged,
            created = jsonNode.createdOrDefault(),
            lastUpdatedBy = jsonNode.lastUpdatedBy,
            reportType = jsonNode.reportType
        )
    }
    fun fromJsonVersion3(jsonNode: JsonNode): Report {
        val lastChanged = jsonNode.lastChangedOrDefault()
        return Report(
            reportId = jsonNode.reportId,
            url = jsonNode.url,
            descriptiveName = jsonNode.descriptiveName,
            organizationUnit = jsonNode.organizationUnit(),
            version = Version.V2,
            author = Author.fromJson(jsonNode, "author")!!,
            successCriteria = jsonNode.mapCriteria(lastChanged),
            lastChanged = lastChanged,
            created = jsonNode.created,
            lastUpdatedBy = jsonNode.lastUpdatedBy,
            reportType = jsonNode.reportType
        )
    }
}

enum class Version(
    val deserialize: (JsonNode) -> Report,
    val criteria: List<SuccessCriterion>,
    val updateCriteria: (SuccessCriterion) -> SuccessCriterion
) {
    V1(ReportVersions::migrateFromJsonVersion1, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion),
    V2(ReportVersions::fromJsonVersion2, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion);

}