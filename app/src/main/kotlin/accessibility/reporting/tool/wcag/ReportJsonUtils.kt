package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun JsonNode.lastChangedOrDefault() = (this["lastChanged"].toLocalDateTime()
    ?: LocalDateTimeHelper.nowAtUtc().also {
        log.warn { "Fant ikke lastChanged-dato for rapport med id ${this["reportId"].asText()}, bruker default" }
    })

val JsonNode.reportId: String
    get() = this["reportId"].asText()
val JsonNode.url: String
    get() = this["url"].asText()

val JsonNode.descriptiveNameOrDefault: String
    get() = this["descriptiveName"]?.takeIf { !it.isNull }?.asText() ?: this.url

val JsonNode.descriptiveName: String //Skal brukes i V3
    get() = this["descriptiveName"].asText()

fun JsonNode.organizationUnit() = this["organizationUnit"].takeIf { !it.isEmpty }
    ?.let { organizationJson ->
        OrganizationUnit.fromJson(organizationJson)
    }

val JsonNode.reportType
    get() = ReportType.valueFromJson(this)

fun JsonNode.createdOrDefault() = this["created"].toLocalDateTime() ?: LocalDateTimeHelper.nowAtUtc().also {
    log.error { "Fant ikke created-dato for rapport med id ${this.reportId}, bruker default" }
}
val JsonNode.created
    get() = this["created"].toLocalDateTime()!!
val JsonNode.lastUpdatedBy
    get() = Author.fromJson(this, "lastUpdatedBy")
fun JsonNode.mapCriteria(lastChanged: LocalDateTime) = this["successCriteria"].map {
    SuccessCriterion.fromJson(
        it,
        Version.V2,
        lastChanged.isBefore(SucessCriteriaV1.lastTextUpdate)
    )
}

fun String.datestr(date: LocalDateTime) = let {
    val formatter = DateTimeFormatter.ofPattern(this)
    date.format(formatter)
}

