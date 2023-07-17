package accessibility.reporting.tool.wcag

import java.time.LocalDate

abstract class Report(
    val url: String,
    val team: Team,
    val version: Version,
    val testUrl: String? = null,
    val successCriteria: List<SuccessCriterion>,
    val testpersonIdent: String? = null,
)

class SuccessCriterion(
    val name: String,
    val guideline: Guideline,
    val number: Int,
    val deviations: List<Deviation>? = null
) {
    val status = when {
        deviations == null -> Status.NOT_TESTED
        deviations.isEmpty() -> Status.COMPLIANT
        deviations.all { it.correctedDate != null } -> Status.COMPLIANT
        deviations.any { it.correctedDate == null } -> Status.NON_COMPLIANT
        else -> throw IllegalStateException("This should not happen")
    }
}

class Deviation(
    val dateIndentified: LocalDate,
    val description: String,
    val correctedDate: LocalDate? = null,
)


class Team(val name: String, teamOrganization: TeamOrganization? = null)
class TeamOrganization(val name: String)

enum class Version() {
    ONE
}

enum class Status() {
    COMPLIANT, NON_COMPLIANT, NOT_TESTED
}

abstract class Principle(val name: String, val number: Int)
abstract class Guideline(val name: String, val number: Int, val principle: Principle)