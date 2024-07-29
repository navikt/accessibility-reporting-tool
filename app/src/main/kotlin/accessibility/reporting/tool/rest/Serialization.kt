package accessibility.reporting.tool.rest

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.wcag.Author
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.criteria.SuccessCriterion
import accessibility.reporting.tool.wcag.report.*
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDateTime

class ReportListItem(
    @JsonProperty("id")
    override val reportId: String,
    @JsonProperty("title")
    override val descriptiveName: String?,
    override val url: String,
    val teamId: String,
    val teamName:String,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("date")
    val lastChanged: LocalDateTime
) : ReportContent {

    companion object {
        fun fromJson(jsonNode: JsonNode) = ReportListItem(
            reportId = jsonNode.reportId,
            descriptiveName = jsonNode.descriptiveName,
            url = jsonNode.url,
            teamId = jsonNode.organizationUnit()?.id ?: "",
            teamName = jsonNode.organizationUnit()?.name ?: "",
            lastChanged = jsonNode.lastChangedOrDefault()
        )
    }
}

class FullReportWithAccessPolicy(
    override val reportId: String,
    override val descriptiveName: String?,
    override val url: String,
    val team: OrganizationUnit?,
    val author: Author,
    val successCriteria: List<SuccessCriterion>,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val created: LocalDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy.MM.dd HH:mm:ss")
    val lastChanged: LocalDateTime,
    val hasWriteAccess: Boolean
) : ReportContent

fun Report.toFullReportWithAccessPolicy(user: User?): FullReportWithAccessPolicy {
    return FullReportWithAccessPolicy(
        reportId = this.reportId,
        descriptiveName = this.descriptiveName,
        url = this.url,
        team = this.organizationUnit,
        author = this.author,
        successCriteria = this.successCriteria,
        created = this.created,
        lastChanged = this.lastChanged,
        hasWriteAccess = this.writeAccess(user)
    )
}