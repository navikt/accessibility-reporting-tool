package accessibility.reporting.tool.wcag.report

import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTimeFromArray
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTimeFromArrayOrNull
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion
import accessibility.reporting.tool.wcag.criteria.SucessCriteriaV1
import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import java.time.LocalDateTime


private val log = KotlinLogging.logger {  }

fun JsonNode.lastChangedOrDefault() = this["lastChanged"]
    ?.toLocalDateTimeFromArrayOrNull()
    ?: LocalDateTimeHelper.nowAtUtc().also {
        log.warn { "Fant ikke lastChanged-dato for rapport med id ${this["reportId"].asText()}, bruker default" }
    }

fun JsonNode.lastChanged() = this["lastChanged"].toLocalDateTimeFromArray()


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

fun JsonNode.createdOrDefault() =
    this["created"].toLocalDateTimeFromArrayOrNull() ?: LocalDateTimeHelper.nowAtUtc().also {
        log.error { "Fant ikke created-dato for rapport med id ${this.reportId}, bruker default" }
    }

fun JsonNode.mapCriteria(lastChanged: LocalDateTime, reportVersion: Version) = this["successCriteria"].map {
    SuccessCriterion.fromJson(
        it,
        reportVersion,
        lastChanged.isBefore(SucessCriteriaV1.lastTextUpdate)
    )
}

