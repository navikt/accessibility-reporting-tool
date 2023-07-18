package accessibility.reporting.tool.wcag


class Version1Report(
    url: String,
    team: Team,
    version: Version,
    testUrl: String? = null,
    successCriteria: List<SuccessCriterion>,
    testpersonIdent: String? = null
) : BaseReport(url, team, version, testUrl, successCriteria, testpersonIdent) {

    companion object {
        val textAlternatives = Guideline(name = "Text Alternatives", section = 1, principle = Principle.PERCEIVABLE)
        val timebasedMedia = Guideline(name = "Timebased media", section = 2, principle = Principle.PERCEIVABLE)
        val adaptable = Guideline(name = "Adatptable", section = 3, principle = Principle.PERCEIVABLE)
        val distinguishable = Guideline(name = "Distinguishable", section = 3, principle = Principle.PERCEIVABLE)
        val successCriteriaV1 = listOf(
            SuccessCriterion.createEmpty(name = "Non-text content", guideline = textAlternatives, number = 1, "https://www.w3.org/TR/WCAG21/#non-text-content"),
            SuccessCriterion.createEmpty(name = "Non-text content", guideline = timebasedMedia, number = 2, "https://www.w3.org/TR/WCAG21/#non-text-content"),
            SuccessCriterion.createEmpty(name = "Non-text content", guideline = adaptable, number = 3, "https://www.w3.org/TR/WCAG21/#non-text-content"),
           SuccessCriterion.createEmpty(name = "Non-text content", guideline = distinguishable, number = 4, "https://www.w3.org/TR/WCAG21/#non-text-content")
        )
    }
}

