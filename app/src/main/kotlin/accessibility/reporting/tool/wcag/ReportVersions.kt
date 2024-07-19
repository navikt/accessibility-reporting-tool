package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import com.fasterxml.jackson.databind.JsonNode


object ReportVersions {
    fun migrateFromJsonVersion1(jsonNode: JsonNode): Report {
        val lastChanged = jsonNode.lastChangedOrDefault()
        return Report(
            reportId = jsonNode["reportId"].asText(),
            url = jsonNode["url"].asText(),
            descriptiveName = jsonNode["descriptiveName"]?.takeIf { !it.isNull }?.asText(),
            organizationUnit = jsonNode["organizationUnit"].takeIf { !it.isEmpty }
                ?.let { organizationJson ->
                    OrganizationUnit.fromJson(organizationJson)
                },
            version = Version.V2,
            reportType = ReportType.valueFromJson(jsonNode),
            testData = jsonNode["testData"].takeIf { !it.isEmpty }?.let { testDataJson ->
                TestData(ident = testDataJson["ident"].asText(), url = testDataJson["url"].asText())
            },
            author = Author.fromJson(jsonNode, "user")!!,
            successCriteria = jsonNode["successCriteria"].map {
                SuccessCriterion.fromJson(
                    it,
                    Version.V2,
                    lastChanged.isBefore(SucessCriteriaV1.lastTextUpdate)
                )
            },
            filters = jsonNode["filters"].map { it.asText() }.toMutableList(),
            lastChanged = lastChanged,
            created = jsonNode["created"].toLocalDateTime() ?: LocalDateTimeHelper.nowAtUtc().also {
                log.error { "Fant ikke created-dato for rapport med id ${jsonNode["reportId"].asText()}, bruker default" }
            },
            lastUpdatedBy = Author.fromJson(jsonNode, "lastUpdatedBy")
        )
    }

    fun fromJsonVersion2(jsonNode: JsonNode): Report {
        val lastChanged = jsonNode.lastChangedOrDefault()
        return Report(
            reportId = jsonNode["reportId"].asText(),
            url = jsonNode["url"].asText(),
            descriptiveName = jsonNode["descriptiveName"]?.takeIf { !it.isNull }?.asText(),
            organizationUnit = jsonNode["organizationUnit"].takeIf { !it.isEmpty }
                ?.let { organizationJson ->
                    OrganizationUnit.fromJson(organizationJson)
                },
            version = Version.V2,
            testData = null,
            author = Author.fromJson(jsonNode, "author")!!,
            successCriteria = jsonNode["successCriteria"].map {
                SuccessCriterion.fromJson(
                    it,
                    Version.V2,
                    lastChanged.isBefore(SucessCriteriaV1.lastTextUpdate)
                )
            },
            filters = jsonNode["filters"].map { it.asText() }.toMutableList(),
            lastChanged = lastChanged,
            created = jsonNode["created"].toLocalDateTime() ?: LocalDateTimeHelper.nowAtUtc().also {
                log.error { "Fant ikke created-dato for rapport med id ${jsonNode["reportId"].asText()}, bruker default" }
            },
            lastUpdatedBy = Author.fromJson(jsonNode, "lastUpdatedBy"),
            reportType = ReportType.valueFromJson(jsonNode)
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