package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.Version.V1
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime


class Report(
    val reportId: String,
    val url: String,
    val organizationUnit: OrganizationUnit?,
    val version: Version,
    val testData: TestData?,
    val user: User,
    val successCriteria: List<SuccessCriterion>,
    val filters: MutableList<String> = mutableListOf()
) {

    companion object {
        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        fun fromJsonVersion1(rawJson: String): Report = jacksonObjectMapper().readTree(rawJson).let { jsonNode ->
            Report(
                reportId = jsonNode["reportId"].asText(),
                url = jsonNode["url"].asText(),
                organizationUnit = jsonNode["organizationUnit"]
                    .takeIf { !it.isEmpty }
                    ?.let { organizationJson ->
                        OrganizationUnit(
                            id = organizationJson["id"].asText(),
                            name = organizationJson["name"].asText(),
                            parentId = organizationJson["parentId"]?.asText(),
                            email = organizationJson["email"].asText()
                        )
                    },
                version = V1,
                testData = jsonNode["testData"]
                    .takeIf { !it.isEmpty }
                    ?.let { testDataJson ->
                        TestData(ident = testDataJson["ident"].asText(), url = testDataJson["url"].asText())
                    },
                user = User(email = jsonNode["user"]["email"].asText(), jsonNode["user"]["name"].asText()),
                successCriteria = jsonNode["successCriteria"].map { SuccessCriterion.fromJson(it) },
                filters = jsonNode["filters"].map { it.asText() }.toMutableList()
            )

        }
    }

    fun toJson(): String = objectMapper.writeValueAsString(this)
}


class TestData(val ident: String, val url: String)
class OrganizationUnit(val id: String, val name: String, parentId: String? = null, val email: String)

enum class Version(val deserialize: (String) -> Report, val criteria: List<SuccessCriterion>) {
    V1(Report::fromJsonVersion1, Version1.criteria)
}

enum class Status(val display: String) {
    COMPLIANT("compliant"), NON_COMPLIANT("non compliant"), NOT_APPLICABLE("not applicable"), NOT_TESTED("not tested");

    companion object {

        fun undisplay(s: String) =
            when (s) {
                COMPLIANT.display -> COMPLIANT
                NOT_APPLICABLE.display -> NOT_APPLICABLE
                NON_COMPLIANT.display -> NON_COMPLIANT
                NOT_TESTED.display -> NOT_TESTED
                else -> throw IllegalArgumentException()
            }
    }
}


class SuccessCriterion(
    val name: String,
    val description: String,
    val principle: String,
    val guideline: String,
    val tools: String,
    val number: String,
    val contentGroup: String,
    var status: Status,
    val wcagUrl: String? = null,
    val helpUrl: String? = null,
    val deviations: MutableList<Deviation> = mutableListOf()
) {
    val successCriterionNumber = "${number}}"


    companion object {
        fun createEmpty(
            contentGroup: String,
            description: String,
            guildeline_: String,
            helpUrl: String = "https://aksel.nav.no/god-praksis/universell-utforming",
            name: String,
            number: String,
            principle: String,
            tools: String,
            wcagUrl: String? = null
        ): SuccessCriterion =
            SuccessCriterion(
                name,
                description,
                principle,
                guildeline_,
                tools,
                number,
                contentGroup,
                Status.NON_COMPLIANT,
                wcagUrl,
                helpUrl
            )

        private val objectMapper = jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
        }

        fun fromJson(rawJson: JsonNode): SuccessCriterion = SuccessCriterion(
            name = rawJson["name"].asText(),
            description = rawJson["description"].asText(),
            principle = rawJson["principle"].asText(),
            guideline = rawJson["guideline"].asText(),
            tools = rawJson["tools"].asText(),
            number = rawJson["number"].asText(),
            contentGroup = rawJson["contentGroup"].asText(),
            status = Status.valueOf(rawJson["status"].asText()),
            wcagUrl = rawJson["wcagUrl"].asText(),
            helpUrl = rawJson["helpUrl"].asText(),
            deviations = Deviation.fromJson(rawJson["deviations"])
        )
    }
}

class Deviation(
    val dateIndentified: LocalDateTime,
    val description: String,
    val correctedDate: LocalDateTime? = null,
) {
    companion object {
        fun fromJson(jsonNode: JsonNode?): MutableList<Deviation> = jsonNode?.toList()?.map {
            Deviation(dateIndentified = LocalDateTimeHelper.nowAtUtc(), description = "", correctedDate = null)
        }?.toMutableList() ?: mutableListOf()
    }
}
