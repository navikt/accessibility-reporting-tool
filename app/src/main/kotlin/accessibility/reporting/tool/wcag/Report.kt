package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.LocalDateTimeHelper.toLocalDateTime
import accessibility.reporting.tool.wcag.Status.*
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.deviationCount
import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.disputedDeviationCount
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import kotlin.IllegalArgumentException

interface ReportContent {
    val reportId: String
    val descriptiveName: String?
    val url: String
}

class Author(val email: String, val oid: String) {
    companion object {
        fun fromJson(jsonNode: JsonNode?, fieldName: String): Author? =
            jsonNode?.get(fieldName)?.takeIf { !it.isNull && !it.isEmpty }
                ?.let { Author(email = it["email"].asText(), oid = it["oid"].asText()) }
    }
}

open class Report(
    override val reportId: String,
    override val url: String,
    override val descriptiveName: String?,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val testData: TestData?,
    val author: Author,
    val successCriteria: List<SuccessCriterion>,
    val filters: MutableList<String> = mutableListOf(),
    val created: LocalDateTime,
    val lastChanged: LocalDateTime,
    val contributers: MutableList<Author> = mutableListOf(),
    val lastUpdatedBy: Author?,
    val reportType: ReportType,
) : ReportContent {
    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        }

        fun migrateFromJsonVersion1(jsonNode: JsonNode): Report =
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

        fun fromJsonVersion2(jsonNode: JsonNode) =
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

        fun copy(
            reportId: String? = null,
            url: String? = null,
            descriptiveName: String? = null,
            organizationUnit: OrganizationUnit? = null,
            version: Version? = null,
            author: Author? = null,
            successCriteria: List<SuccessCriterion>? = null,
            filters: MutableList<String>? = null,
            created: LocalDateTime? = null,
            lastChanged: LocalDateTime? = null,
            contributers: MutableList<Author>? = null,
            lastUpdatedBy: Author? = null,
            reportType: ReportType? = null,
        ) = Report(
            reportId = reportId ?: this.reportId,
            url = url ?: this.url,
            descriptiveName = descriptiveName ?: this.descriptiveName,
            organizationUnit = organizationUnit ?: this.organizationUnit,
            version = version ?: this.version,
            testData = null,
            author = author ?: this.author,
            successCriteria = successCriteria ?: this.successCriteria,
            filters = filters ?: this.filters,
            created = created ?: this.created,
            lastChanged = lastChanged ?: this.lastChanged,
            contributers = contributers ?: this.contributers,
            lastUpdatedBy = lastUpdatedBy ?: this.lastUpdatedBy,
            reportType = reportType ?: this.reportType
        )

        open fun toJson(): String =
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

        open fun withUpdatedCriterion(criterion: SuccessCriterion, updateBy: User): Report = copy(
            successCriteria = successCriteria.map { if (it.number == criterion.number) criterion else it },
            lastChanged = LocalDateTimeHelper.nowAtUtc(),
            lastUpdatedBy = updateBy.toAuthor()
        ).apply {
            if (!isOwner(updateBy)) contributers.add(updateBy.toAuthor())
        }

        open fun withUpdatedMetadata(
            title: String? = null,
            pageUrl: String? = null,
            organizationUnit: OrganizationUnit?,
            updateBy: User
        ) = copy(
            url = pageUrl ?: url,
            descriptiveName = title ?: descriptiveName,
            organizationUnit = organizationUnit ?: this.organizationUnit,
            lastChanged = LocalDateTimeHelper.nowAtUtc(),
            lastUpdatedBy = updateBy.toAuthor(),
        ).apply { if (!isOwner(updateBy)) contributers.add(updateBy.toAuthor()) }

        fun isOwner(callUser: User): Boolean =
            author.oid == callUser.oid

        fun h1() = when (reportType) {
            ReportType.AGGREGATED -> "Tilgjengelighetserklæring (Samlerapport)"
            ReportType.SINGLE -> "Tilgjengelighetserklæring"
        }

        fun writeAccess(user: User?): Boolean = when {
            user == null -> false
            Admins.isAdmin(user) -> true
            isOwner(user) -> true
            organizationUnit?.isMember(user) == true -> true
            else -> false
        }

    }

    private val Int.punkter: String
        get() = if (this == 1) {
            "1 punkt"
        } else "$this punkter"

    class TestData(val ident: String, val url: String)
    class OrganizationUnit(
        val id: String,
        val name: String,
        val email: String,
        val members: MutableSet<String> = mutableSetOf()
    ) {
        fun isMember(user: User) = members.any { it == user.email.comparable() }
        fun addMember(userEmail: String) = members.add(userEmail.comparable())
        fun removeMember(userEmail: String) = members.removeIf { userEmail.comparable() == it.comparable() }

        companion object {
            fun createNew(name: String, email: String, shortName: String? = null) = OrganizationUnit(
                id = shortName?.toOrgUnitId() ?: name.toOrgUnitId(),
                name = name,
                email = email,
                members = mutableSetOf()
            )

            private fun String.toOrgUnitId() = trimMargin()
                .lowercase()
                .replace(" ", "-")

            fun fromJson(organizationJson: JsonNode): OrganizationUnit = OrganizationUnit(
                id = organizationJson["id"].asText(),
                name = organizationJson["name"].asText(),
                email = organizationJson["email"].asText(),
                members = organizationJson["members"].takeIf { it != null }?.toList()?.map { it.asText() }
                    ?.toMutableSet()
                    ?: mutableSetOf()
            )
        }

        private fun String.comparable(): String = trimIndent().lowercase()
    }


    enum class Version(
        val deserialize: (JsonNode) -> Report,
        val criteria: List<SuccessCriterion>,
        val updateCriteria: (SuccessCriterion) -> SuccessCriterion
    ) {
        V1(Report::migrateFromJsonVersion1, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion),
        V2(Report::fromJsonVersion2, SucessCriteriaV1.criteriaTemplate, SucessCriteriaV1::updateCriterion);

    }

    enum class ReportType {
        AGGREGATED, SINGLE;

        companion object {
            fun valueFromJson(reportRoot: JsonNode): ReportType =
                reportRoot["reportType"].let {
                    when (it) {
                        null -> SINGLE
                        else -> valueOf(it.asText("SINGLE"))
                    }
                }
        }
    }
