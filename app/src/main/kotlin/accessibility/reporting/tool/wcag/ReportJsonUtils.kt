package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTimeOrNull
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun JsonNode.lastChangedOrDefault() = (this["lastChanged"].toLocalDateTimeOrNull()
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

val JsonNode.isPartOfNavNo: Boolean
    get() = this["isPartOfNavNo"]?.asBoolean() ?: false

fun JsonNode.organizationUnit() = this["organizationUnit"].takeIf { !it.isEmpty }
    ?.let { organizationJson ->
        OrganizationUnit.fromJson(organizationJson)
    }

fun JsonNode.createdOrDefault() = this["created"].toLocalDateTimeOrNull() ?: LocalDateTimeHelper.nowAtUtc().also {
    log.error { "Fant ikke created-dato for rapport med id ${this.reportId}, bruker default" }
}
fun JsonNode.mapCriteria(lastChanged: LocalDateTime, reportVersion: Version) = this["successCriteria"].map {
    SuccessCriterion.fromJson(
        it,
        reportVersion,
        lastChanged.isBefore(SucessCriteriaV1.lastTextUpdate)
    )
}

fun String.datestr(date: LocalDateTime) = let {
    val formatter = DateTimeFormatter.ofPattern(this)
    date.format(formatter)
}


