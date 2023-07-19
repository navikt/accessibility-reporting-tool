package accessibility.reporting.tool.wcag

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.*


class ReportV1(
    reportId: String,
    url: String,
    organizationUnit: OrganizationUnit,
    testUrl: String? = null,
    successCriteria: List<SuccessCriterion>,
    testpersonIdent: String? = null
) : Report(reportId = reportId, url, organizationUnit, Version.ONE, testUrl, successCriteria, testpersonIdent) {

    companion object {
        fun deserialize(s: String): Report = jacksonObjectMapper().readValue(s,ReportV1::class.java)

        private val objectMapper = jacksonObjectMapper().apply {

        }
        fun createEmpty(url: String, organizationUnit: OrganizationUnit, testUrl: String?, testpersonIdent: String?) =
            ReportV1(
                reportId = UUID.randomUUID().toString(),
                url = url,
                organizationUnit = organizationUnit,
                testUrl = url,
                testpersonIdent = testpersonIdent,
                successCriteria = successCriteriaV1
            )

        val textAlternatives = Guideline(name = "Text Alternatives", section = 1, principle = Principle.PERCEIVABLE)
        val timebasedMedia = Guideline(name = "Timebased media", section = 2, principle = Principle.PERCEIVABLE)
        val adaptable = Guideline(name = "Adatptable", section = 3, principle = Principle.PERCEIVABLE)
        val distinguishable = Guideline(name = "Distinguishable", section = 3, principle = Principle.PERCEIVABLE)
        val successCriteriaV1 = listOf(
            SuccessCriterion.createEmpty(
                name = "Non-text content",
                guideline = textAlternatives,
                number = 1,
                "https://www.w3.org/TR/WCAG21/#non-text-content"
            )
        )
    }

    override fun toJson() = objectMapper.writeValueAsString(this)
}

