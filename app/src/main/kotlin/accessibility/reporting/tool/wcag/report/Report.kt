package accessibility.reporting.tool.wcag.report

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.Author
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.criteria.Status
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion
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

interface PersistableReport : ReportContent {
    val lastUpdatedBy: Author?
    val lastChanged: LocalDateTime
    val created: LocalDateTime
    fun toJson(): String
}

open class Report(
    override val reportId: String,
    override val url: String,
    override val descriptiveName: String?,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val author: Author,
    val successCriteria: List<SuccessCriterion>,
    override val created: LocalDateTime,
    override val lastChanged: LocalDateTime,
    val contributors: MutableList<Author> = mutableListOf(),
    override val lastUpdatedBy: Author?,
    val reportType: ReportType,
) : PersistableReport {
    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        }
        val currentVersion = Version.V3
    }

    fun copy(
        reportId: String? = null,
        url: String? = null,
        descriptiveName: String? = null,
        organizationUnit: OrganizationUnit? = null,
        version: Version? = null,
        author: Author? = null,
        successCriteria: List<SuccessCriterion>? = null,
        created: LocalDateTime? = null,
        lastChanged: LocalDateTime? = null,
        contributors: MutableList<Author>? = null,
        lastUpdatedBy: Author? = null,
        reportType: ReportType? = null,
    ) = Report(
        reportId = reportId ?: this.reportId,
        url = url ?: this.url,
        descriptiveName = descriptiveName ?: this.descriptiveName,
        organizationUnit = organizationUnit ?: this.organizationUnit,
        version = version ?: this.version,
        author = author ?: this.author,
        successCriteria = successCriteria ?: this.successCriteria,
        created = created ?: this.created,
        lastChanged = lastChanged ?: this.lastChanged,
        contributors = contributors ?: this.contributors,
        lastUpdatedBy = lastUpdatedBy ?: this.lastUpdatedBy,
        reportType = reportType ?: this.reportType
    )

    fun updateCriteria(criteria: List<SuccessCriterion>, updateBy: User): Report = copy(
        successCriteria = criteria,
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        lastUpdatedBy = updateBy.toAuthor()
    ).apply {
        if (!isOwner(updateBy)) contributors.add(updateBy.toAuthor())
    }

    override fun toJson(): String =
        objectMapper.writeValueAsString(this)

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
        if (!isOwner(updateBy)) contributors.add(updateBy.toAuthor())
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
    ).apply { if (!isOwner(updateBy)) contributors.add(updateBy.toAuthor()) }

    fun isOwner(callUser: User): Boolean =
        author.oid == callUser.oid.str()

    fun writeAccess(user: User?): Boolean = when {
        user == null -> false
        Admins.isAdmin(user) -> true
        isOwner(user) -> true
        organizationUnit?.isMember(user) == true -> true
        else -> false
    }
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