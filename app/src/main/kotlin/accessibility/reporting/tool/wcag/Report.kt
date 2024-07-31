package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.rest.NewTeam
import accessibility.reporting.tool.rest.TeamUpdate
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

open class Report(
    override val reportId: String,
    override val url: String,
    override val descriptiveName: String?,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val author: Author,
    val successCriteria: List<SuccessCriterion>,
    val created: LocalDateTime,
    val lastChanged: LocalDateTime,
    val contributors: MutableList<Author> = mutableListOf(),
    val lastUpdatedBy: Author?,
    val reportType: ReportType,
) : ReportContent {
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

    open fun toJson(): String =
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

data class OrganizationUnit(
    val id: String,
    val name: String,
    val email: String,
    val members: MutableSet<String> = mutableSetOf()
) {
    fun isMember(user: User) = members.any { it == user.email.str().comparable() }
    fun addMember(userEmail: User.Email) = members.add(userEmail.str().comparable())
    fun removeMember(userEmail: String) = members.removeIf { userEmail.comparable() == it.comparable() }
    fun update(updateValues: TeamUpdate) = copy(
        name = updateValues.name ?: name,
        email = updateValues.email ?: email,
        members = updateValues.members ?: members
    )

    companion object {
        fun createNew(name: String, email: String, shortName: String? = null) = OrganizationUnit(
            id = shortName?.toOrgUnitId() ?: name.toOrgUnitId(),
            name = name,
            email = email,
            members = mutableSetOf()
        )

        fun createNew(newTeam: NewTeam) = OrganizationUnit(
            id = newTeam.name.toOrgUnitId(),
            name = newTeam.name,
            email = newTeam.email,
            members = newTeam.members.toMutableSet()
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
    fun hasWriteAccess(user: User): Boolean =
        user.email.str() == email || Admins.isAdmin(user) || members.any { it == user.email.str() }
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

class Author(val email: String, val oid: String) {
    companion object {
        fun fromJsonOrNull(jsonNode: JsonNode?, fieldName: String): Author? =
            jsonNode?.get(fieldName)?.takeIf { !it.isNull && !it.isEmpty }
                ?.let { Author(email = it["email"].asText(), oid = it["oid"].asText()) }

        fun fromJson(jsonNode: JsonNode, fieldName: String): Author =
            jsonNode[fieldName].let { Author(email = it["email"].asText(), oid = it["oid"].asText()) }
    }
}