package accessibility.reporting.tool.wcag


class Version1Report(
    url: String,
    organizationUnit: OrganizationUnit,
    version: Version,
    testUrl: String? = null,
    successCriteria: List<SuccessCriterion>,
    testpersonIdent: String? = null
) : BaseReport(url, organizationUnit, version, testUrl, successCriteria, testpersonIdent) {

    companion object {
        val textAlternatives = Guideline(name = "Text Alternatives", section = 1, principle = Principle.PERCEIVABLE)
        val timebasedMedia = Guideline(name = "Timebased media", section = 2, principle = Principle.PERCEIVABLE)
        val adaptable = Guideline(name = "Adatptable", section = 3, principle = Principle.PERCEIVABLE)
        val distinguishable = Guideline(name = "Distinguishable", section = 3, principle = Principle.PERCEIVABLE)
        val successCriteriaV1 = listOf(
            SuccessCriterion.createEmpty(name = "Non-text content", guideline = textAlternatives, number = 1, "https://www.w3.org/TR/WCAG21/#non-text-content")
        )
    }
}

