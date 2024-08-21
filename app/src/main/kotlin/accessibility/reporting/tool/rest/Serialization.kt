package accessibility.reporting.tool.rest

import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
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