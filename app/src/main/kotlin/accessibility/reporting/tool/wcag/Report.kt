package accessibility.reporting.tool.wcag

import java.time.LocalDate
import java.time.LocalDateTime


abstract class Report(
    val reportId: String = "foo",
    val url: String,
    val organizationUnit: OrganizationUnit,
    val version: Version,
    val testUrl: String? = null,
    val successCriteria: List<SuccessCriterion>,
    val testpersonIdent: String? = null,
    val filters: MutableList<String> = mutableListOf()
    // kontaktperson/ansvarsperson
) {
    abstract fun toJson(): String
    companion object {
                fun createLatest(url: String, organizationUnit: OrganizationUnit, testUrl: String?, testpersonIdent: String?) =
            ReportV1.createEmpty(url, organizationUnit, testUrl, testpersonIdent)
        }
    }


class OrganizationUnit(val id: String, val name: String, parent: OrganizationUnit? = null, val email:String)


enum class Version(val deserialize: (String) -> Report) {
    ONE(deserialize = ReportV1::deserialize)

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
    val description: String,
    val principle: String,
    val guildeline_: String,
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
            SuccessCriterion(name, description, principle, guildeline_,tools, number, contentGroup, Status.NON_COMPLIANT, wcagUrl, helpUrl)
    }
}

class Deviation(
    val dateIndentified: LocalDateTime,
    val description: String,
    val correctedDate: LocalDate? = null,
)
