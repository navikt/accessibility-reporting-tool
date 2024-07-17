package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun JsonNode.reportLastChanged() = (this["lastChanged"].toLocalDateTime()
    ?: LocalDateTimeHelper.nowAtUtc().also {
        log.warn { "Fant ikke lastChanged-dato for rapport med id ${this["reportId"].asText()}, bruker default" }
    })

val JsonNode.reportId: String
    get() = this["reportId"].asText()
val JsonNode.url: String
    get() = this["url"].asText()

val JsonNode.descriptiveName: String?
    get() = this["descriptiveName"]?.takeIf { !it.isNull }?.asText()

fun JsonNode.orgnaizationUnit() = this["organizationUnit"].takeIf { !it.isEmpty }
    ?.let { organizationJson ->
        OrganizationUnit.fromJson(organizationJson)
    }

fun String.datestr(date: LocalDateTime) = let {
    val formatter = DateTimeFormatter.ofPattern(this)
    date.format(formatter)
}