package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.user
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.Status.*
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.disputedDeviationCount
import accessibility.reporting.tool.wcag.Version.V1
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.server.application.*
import java.lang.IllegalArgumentException
import java.time.LocalDateTime


class Report(
    val reportId: String,
    val url: String,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val testData: TestData?,
    val user: User,
    val successCriteria: List<SuccessCriterion>,
    val filters: MutableList<String> = mutableListOf(),
    val created: LocalDateTime,
    val lastChanged: LocalDateTime
) {
    fun findCriterion(index: String) =
        successCriteria.find { it.number == index } ?: throw java.lang.IllegalArgumentException("no such criteria")

    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        }

        fun fromJsonVersion1(rawJson: String, created: LocalDateTime, lastChanged: LocalDateTime): Report {
            val successCriterionIsStale = lastChanged.isBefore(Version1.lastTextUpdate)
            return jacksonObjectMapper().readTree(rawJson).let { jsonNode ->
                Report(
                    reportId = jsonNode["reportId"].asText(),
                    url = jsonNode["url"].asText(),
                    organizationUnit = jsonNode["organizationUnit"].takeIf { !it.isEmpty }?.let { organizationJson ->
                        OrganizationUnit(
                            id = organizationJson["id"].asText(),
                            name = organizationJson["name"].asText(),
                            email = organizationJson["email"].asText()
                        )
                    },
                    version = V1,
                    testData = jsonNode["testData"].takeIf { !it.isEmpty }?.let { testDataJson ->
                        TestData(ident = testDataJson["ident"].asText(), url = testDataJson["url"].asText())
                    },
                    user = User(email = jsonNode["user"]["email"].asText(), jsonNode["user"]["name"].asText()),
                    successCriteria = jsonNode["successCriteria"].map {
                        SuccessCriterion.fromJson(
                            it,
                            V1,
                            successCriterionIsStale
                        )
                    },
                    filters = jsonNode["filters"].map { it.asText() }.toMutableList(),
                    lastChanged = lastChanged,
                    created = created
                )
            }
        }
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)
    fun status(): String = when {
        successCriteria.any { it.status == NOT_TESTED } -> "Ikke ferdig"
        successCriteria.deviationCount() != 0 ->
            "${successCriteria.deviationCount()} avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

        successCriteria.deviationCount() == 0 ->
            "Ingen avvik, ${successCriteria.disputedDeviationCount().punkter} med merknad"

        else -> "Ukjent"
    }

    fun updateCriteria(
        criterionNumber: String,
        statusString: String,
        breakingTheLaw: String?,
        lawDoesNotApply: String?,
        tooHardToComply: String?
    ): SuccessCriterion =
        successCriteria.find { it.successCriterionNumber == criterionNumber }.let { criteria ->
            criteria?.copy(
                status = Status.undisplay(statusString),
                breakingTheLaw = breakingTheLaw ?: criteria.breakingTheLaw,
                lawDoesNotApply = lawDoesNotApply ?: criteria.lawDoesNotApply,
                tooHardToComply = tooHardToComply ?: criteria.tooHardToComply
            )?.apply { wcagLevel = criteria.wcagLevel }
        } ?: throw IllegalArgumentException("ukjent successkriterie")

    fun withUpdatedCriterion(criterion: SuccessCriterion, user: User): Report = Report(
        organizationUnit = organizationUnit,
        reportId = reportId,
        successCriteria = successCriteria.map { if (it.number == criterion.number) criterion else it },
        testData = testData,
        url = url,
        user = user,
        version = version,
        created = created,
        lastChanged = LocalDateTimeHelper.nowAtUtc()
    )

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
    val deserialize: (String, LocalDateTime, LocalDateTime) -> Report,
    val criteria: List<SuccessCriterion>,
    val updateCriteria: (SuccessCriterion) -> SuccessCriterion
) {
    V1(Report::fromJsonVersion1, Version1.criteria, Version1::updateCriterion);

}
