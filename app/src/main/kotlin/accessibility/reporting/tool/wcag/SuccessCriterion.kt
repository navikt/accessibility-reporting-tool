package accessibility.reporting.tool.wcag

import com.fasterxml.jackson.databind.JsonNode

data class SuccessCriterion(
    val name: String,
    val description: String,
    val principle: String,
    val guideline: String,
    val tools: String,
    val number: String,
    val breakingTheLaw: String,
    val lawDoesNotApply: String,
    val tooHardToComply: String,
    val contentGroup: String,
    var status: Status,
    val wcagUrl: String? = null,
    val helpUrl: String? = null,
    val wcagVersion: String = "2.1"
) {
    lateinit var wcagLevel: WcagLevel
    fun devationIsDesputed() =
        breakingTheLaw.isEmpty() && (lawDoesNotApply.isNotEmpty() || tooHardToComply.isNotEmpty())

    val successCriterionNumber = number

    companion object {
        fun List<SuccessCriterion>.disputedDeviationCount() =
            count { it.status == Status.NON_COMPLIANT && it.devationIsDesputed() }

        private val criterionNumberComparator =
            Comparator { o1: SuccessCriterion, o2: SuccessCriterion ->
                val x = o1.number.split(".")
                    .let { list -> list.sumOf { i -> i.toInt() } }
                val y = o2.number.split(".")
                    .let { list -> list.sumOf { i -> i.toInt() } }
                return@Comparator x - y
            }


        fun List<SuccessCriterion>.deviationCount() =
            count { it.status == Status.NON_COMPLIANT && !it.devationIsDesputed() }

        fun List<SuccessCriterion>.aggregate(): List<SuccessCriterion> =
            groupBy { it.number }
                .map {
                    first().let { template ->
                        SuccessCriterion(
                            name = template.name,
                            description = template.description,
                            principle = template.principle,
                            guideline = template.guideline,
                            tools = template.tools,
                            number = template.number,
                            breakingTheLaw = mapNotNull { it.breakingTheLaw.ifBlank { null } }.joinToString("\n"),
                            lawDoesNotApply = mapNotNull { it.lawDoesNotApply.ifBlank { null } }.joinToString("\n"),
                            tooHardToComply = mapNotNull { it.tooHardToComply.ifBlank { null } }.joinToString("\n"),
                            contentGroup = template.contentGroup,
                            status = resolveStatus(),
                            wcagUrl = template.wcagUrl,
                            helpUrl = template.helpUrl,
                            wcagVersion = template.wcagVersion
                        )
                    }
                }
                .sortedBy { it. }

        private fun List<SuccessCriterion>.resolveStatus(): Status =
            when {
                all { it.status == Status.NOT_TESTED } -> Status.NOT_TESTED
                any { it.status == Status.NON_COMPLIANT } -> Status.NON_COMPLIANT
                all { it.status == Status.NOT_APPLICABLE } -> Status.NOT_APPLICABLE
                all { it.status == Status.COMPLIANT } -> Status.COMPLIANT
                else -> {
                    log.warn { "Could not resolve status for successcriterium ${first().number}" }
                    Status.NOT_TESTED
                }
            }


        fun fromJson(rawJson: JsonNode, version: Version, isStale: Boolean): SuccessCriterion =
            SuccessCriterion(
                name = rawJson["name"].asText(),
                description = rawJson["description"].asText(),
                principle = rawJson["principle"].asText(),
                guideline = rawJson["guideline"].asText(),
                tools = rawJson["tools"].asText(),
                number = rawJson["number"].asText(),
                breakingTheLaw = rawJson["breakingTheLaw"].asText(),
                lawDoesNotApply = rawJson["lawDoesNotApply"].asText(),
                tooHardToComply = rawJson["tooHardToComply"].asText(),
                contentGroup = rawJson["contentGroup"].asText(),
                status = Status.valueOf(rawJson["status"].asText()),
                wcagUrl = rawJson["wcagUrl"].takeIf { !it.isNull }?.asText(),
                helpUrl = rawJson["helpUrl"].takeIf { !it.isNull }?.asText()
            ).apply {
                wcagLevel =
                    rawJson["wcagLevel"]?.takeIf { !it.isNull }?.asText()
                        ?.let { WcagLevel.valueOf(it) } ?: WcagLevel.UNKNOWN
            }.let {
                if (isStale)
                    version.updateCriteria(it)
                else it
            }
    }
}

enum class WcagLevel() {
    A, AA, AAA, UNKNOWN
}

enum class Status(val display: String) {
    COMPLIANT("compliant"), NON_COMPLIANT("non-compliant"), NOT_APPLICABLE("not-applicable"), NOT_TESTED("not-tested");

    companion object {

        fun undisplay(s: String) = when (s) {
            COMPLIANT.display -> COMPLIANT
            NOT_APPLICABLE.display -> NOT_APPLICABLE
            NON_COMPLIANT.display -> NON_COMPLIANT
            NOT_TESTED.display -> NOT_TESTED
            else -> throw IllegalArgumentException()
        }
    }
}
