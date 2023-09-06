package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import accessibility.reporting.tool.wcag.Status.*
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.disputedDeviationCount
import accessibility.reporting.tool.wcag.Version.V1
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.IllegalArgumentException


open class Report(
    val reportId: String,
    val url: String,
    val descriptiveName: String?,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val testData: TestData?,
    val user: User,
    val successCriteria: List<SuccessCriterion>,
    val filters: MutableList<String> = mutableListOf(),
    val created: LocalDateTime,
    val lastChanged: LocalDateTime,
    val contributers: MutableList<User> = mutableListOf(),
    val lastUpdatedBy: User?,
    val reportType: ReportType,
) {
    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        }

        fun fromJsonVersion1(jsonNode: JsonNode): Report =
            (jsonNode["lastChanged"].toLocalDateTime()
                ?: LocalDateTimeHelper.nowAtUtc().also {
                    log.warn { "Fant ikke lastChanged-dato for rapport med id ${jsonNode["reportId"].asText()}, bruker default" }
                })
                .let { lastChanged ->
                    Report(
                        reportId = jsonNode["reportId"].asText(),
                        url = jsonNode["url"].asText(),
                        descriptiveName = jsonNode["descriptiveName"]?.takeIf { !it.isNull }?.asText(),
                        organizationUnit = jsonNode["organizationUnit"].takeIf { !it.isEmpty }
                            ?.let { organizationJson ->
                                OrganizationUnit(
                                    id = organizationJson["id"].asText(),
                                    name = organizationJson["name"].asText(),
                                    email = organizationJson["email"].asText()
                                )
                            },
                        version = V1,
                        reportType = ReportType.valueOf(jsonNode["reportType"].let {
                            if (it == null)
                                "SINGLE"
                            else it.asText()
                        }),
                        testData = jsonNode["testData"].takeIf { !it.isEmpty }?.let { testDataJson ->
                            TestData(ident = testDataJson["ident"].asText(), url = testDataJson["url"].asText())
                        },
                        user = User.fromJson(jsonNode["user"])!!,
                        successCriteria = jsonNode["successCriteria"].map {
                            SuccessCriterion.fromJson(
                                it,
                                V1,
                                lastChanged.isBefore(Version1.lastTextUpdate)
                            )
                        },
                        filters = jsonNode["filters"].map { it.asText() }.toMutableList(),
                        lastChanged = lastChanged,
                        created = jsonNode["created"].toLocalDateTime() ?: LocalDateTimeHelper.nowAtUtc().also {
                            log.error { "Fant ikke created-dato for rapport med id ${jsonNode["reportId"].asText()}, bruker default" }
                        },
                        lastUpdatedBy = User.fromJson(jsonNode["lastUpdatedBy"])
                    )
                }
    }


    fun toJson(): String =
        objectMapper.writeValueAsString(this)

    fun statusString(): String = when {
        successCriteria.any { it.status == NOT_TESTED } -> "Ikke ferdig"
        successCriteria.deviationCount() != 0 ->
            "${successCriteria.deviationCount()} avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

        successCriteria.deviationCount() == 0 ->
            "Ingen avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

        else -> "Ukjent"
    }

    fun updateCriterion(
        criterionNumber: String,
        statusString: String,
        breakingTheLaw: String?,
        lawDoesNotApply: String?,
        tooHardToComply: String?
    ) = findCriterion(criterionNumber).let { criteria ->
        criteria.copy(
            status = Status.undisplay(statusString),
            breakingTheLaw = breakingTheLaw ?: criteria.breakingTheLaw,
            lawDoesNotApply = lawDoesNotApply ?: criteria.lawDoesNotApply,
            tooHardToComply = tooHardToComply ?: criteria.tooHardToComply
        ).apply { wcagLevel = criteria.wcagLevel }
    }

    fun findCriterion(criterionNumber: String) =
        successCriteria.find { it.number == criterionNumber }
            ?: throw IllegalArgumentException("Criteria with number $criterionNumber does not exists")

    fun withUpdatedCriterion(criterion: SuccessCriterion, updateBy: User): Report = Report(
        organizationUnit = organizationUnit,
        reportId = reportId,
        successCriteria = successCriteria.map { if (it.number == criterion.number) criterion else it },
        testData = testData,
        url = url,
        user = if (userIsOwner(updateBy)) updateBy else user,
        version = version,
        created = created,
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        lastUpdatedBy = updateBy,
        descriptiveName = descriptiveName,
        reportType = reportType
    ).apply { if (!userIsOwner(updateBy)) contributers.add(updateBy) }

    fun withUpdatedMetadata(title: String?, pageUrl: String?, organizationUnit: OrganizationUnit?, updateBy: User) =
        Report(
            reportId = reportId,
            url = pageUrl ?: url,
            descriptiveName = title ?: descriptiveName,
            organizationUnit = organizationUnit ?: this.organizationUnit,
            version = version,
            testData = testData,
            user = user,
            successCriteria = successCriteria,
            filters = filters,
            created = created,
            lastChanged = LocalDateTimeHelper.nowAtUtc(),
            lastUpdatedBy = updateBy,
            reportType = reportType
        ).apply { if (!userIsOwner(updateBy)) contributers.add(updateBy) }

    fun userIsOwner(callUser: User): Boolean =
        user.oid == callUser.oid || user.email == callUser.oid//TODO: fjern sammenligning av oid på email

}

private val Int.punkter: String
    get() = if (this == 1) {
        "1 punkt"
    } else "$this punkter"

class TestData(val ident: String, val url: String)
class OrganizationUnit(
    val id: String, val name: String, val email: String, val shortName: String? = null
) {

    companion object {
        fun createNew(name: String, email: String, shortName: String? = null) = OrganizationUnit(
            id = shortName?.toOrgUnitId() ?: name.toOrgUnitId(),
            name = name,
            email = email,
            shortName = shortName
        )

        private fun String.toOrgUnitId() = trimMargin()
            .lowercase()
            .replace(" ", "-")
    }
}


enum class Version(
    val deserialize: (JsonNode) -> Report,
    val criteria: List<SuccessCriterion>,
    val updateCriteria: (SuccessCriterion) -> SuccessCriterion
) {
    V1(Report::fromJsonVersion1, Version1.criteriaTemplate, Version1::updateCriterion);
}

enum class ReportType {
    AGGREGATED, SINGLE
}

