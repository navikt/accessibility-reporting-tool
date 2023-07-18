package accessibility.reporting.tool.wcag

import java.time.LocalDate


abstract class BaseReport(
    val url: String,
    val team: Team,
    val version: Version,
    val testUrl: String? = null,
    val successCriteria: List<SuccessCriterion>,
    val testpersonIdent: String? = null,
)

class Team(val name: String, teamOrganization: TeamOrganization? = null)
class TeamOrganization(val name: String)

enum class Version() { ONE }

enum class Status() {
    COMPLIANT, NON_COMPLIANT, NOT_APPLICABLE, NOT_TESTED
}

class Guideline(val name: String, val section: Int, val principle: Principle)

enum class Principle(val number: Int, description: String) {
    PERCEIVABLE(
        1,
        "Information and user interface components must be presentable to users in ways they can perceive."
    ),
    OPERABLE(2, "User interface components and navigation must be operable"),
    UNDERSTANDABLE(3, "Information and the operation of user interface must be understandable"),
    ROBUST(
        4,
        "Content must be robust enough that it can be interpreted by by a wide variety of user agents, including assistive technologies"
    )
}

class SuccessCriterion(
    val name: String,
    val guideline: Guideline,
    val number: Int,
    var status: Status,
    wcagUrl: String,
    helpUrl: String,
    val deviations: List<Deviation>? = null
) {
    val successCriterionNumber = "${guideline.principle.number}.${guideline.section}.${this.number}"

    companion object {
        fun createEmpty(
            name: String,
            guideline: Guideline,
            number: Int,
            wcagUrl: String,
            helpUrl: String = "https://aksel.nav.no/god-praksis/universell-utforming"
        ): SuccessCriterion =
            SuccessCriterion(name, guideline, number, Status.NOT_TESTED, wcagUrl, helpUrl)
    }
}

class Deviation(
    val dateIndentified: LocalDate,
    val description: String,
    val correctedDate: LocalDate? = null,
)