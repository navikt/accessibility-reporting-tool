package accessibility.reporting.tool.wcag

import com.fasterxml.jackson.databind.JsonNode

fun List<SuccessCriterion>.aggregateBreakingTheLaw(): String =
    filter { it.status == Status.NON_COMPLIANT && it.breakingTheLaw.isNotBlank() }.joinToString("\n") { it.breakingTheLaw }

private fun List<SuccessCriterion>.aggregateLawDoesNotApply(): String =
    filter { it.status == Status.NON_COMPLIANT && it.lawDoesNotApply.isNotBlank() }.joinToString("\n") { it.lawDoesNotApply }

private fun List<SuccessCriterion>.aggregateTooHardToComply(): String =
    filter { it.status == Status.NON_COMPLIANT && it.tooHardToComply.isNotBlank() }.joinToString("\n") { it.tooHardToComply }


data class SuccessCriterion(
    val name: String,
    val description: String,
    val principle: String,
    val guideline: String,
    val tools: String,
    val number: String,
    val breakingTheLaw: String, //kan endres
    val lawDoesNotApply: String, //kan endres
    val tooHardToComply: String, // kan endres
    val contentGroup: String,
    var status: Status, //kan endres
    val wcagUrl: String? = null,
    val helpUrl: String? = null,
    val wcagVersion: String = "2.1"
) {
    lateinit var wcagLevel: WcagLevel //kan ikke endres
    val successCriterionNumber = number

    fun devationIsDesputed() =
        breakingTheLaw.isEmpty() && (lawDoesNotApply.isNotEmpty() || tooHardToComply.isNotEmpty())

    fun describeFindings(): String =
        when {
            status == Status.COMPLIANT -> ""
            status == Status.NOT_TESTED -> ""
            status == Status.NOT_APPLICABLE -> ""
            status == Status.NON_COMPLIANT && breakingTheLaw.isNotEmpty() -> breakingTheLaw
            status == Status.NON_COMPLIANT && lawDoesNotApply.isNotEmpty() -> lawDoesNotApply
            status == Status.NON_COMPLIANT && tooHardToComply.isNotEmpty() -> tooHardToComply
            else -> "Ukjent"
        }


    companion object {
        fun List<SuccessCriterion>.disputedDeviationCount() =
            count { it.status == Status.NON_COMPLIANT && it.devationIsDesputed() }

        fun List<SuccessCriterion>.deviationCount() =
            count { it.status == Status.NON_COMPLIANT && !it.devationIsDesputed() }

        fun List<SuccessCriterionSummary>.aggregate(): List<SuccessCriterion> =
            groupBy { it.content.number }
                .values
                .map { list ->
                    list.first().content.let { template ->
                        SuccessCriterion(
                            name = template.name,
                            description = template.description,
                            principle = template.principle,
                            guideline = template.guideline,
                            tools = template.tools,
                            number = template.number,
                            breakingTheLaw = list.joinToString("\n") { it.breakingTheLaw },
                            lawDoesNotApply = list.joinToString("\n") { it.lawDoesNotApply },
                            tooHardToComply = list.joinToString("\n") { it.tooHardToComply },
                            contentGroup = template.contentGroup,
                            status = list.map { it.content }.resolveStatus(),
                            wcagUrl = template.wcagUrl,
                            helpUrl = template.helpUrl,
                            wcagVersion = template.wcagVersion
                        ).apply {
                            wcagLevel = template.wcagLevel
                        }
                    }
                }

        private fun List<SuccessCriterion>.resolveStatus(): Status =
            when {
                any { it.status == Status.NON_COMPLIANT } -> Status.NON_COMPLIANT
                all { it.status == Status.NOT_APPLICABLE } -> Status.NOT_APPLICABLE
                all { it.status == Status.COMPLIANT || it.status == Status.NOT_APPLICABLE } -> Status.COMPLIANT
                all { it.status == Status.NOT_TESTED } -> Status.NOT_TESTED
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

data class SuccessCriterionSummary(val reportTitle: String, val contactPerson: String, val content: SuccessCriterion) {
    val breakingTheLaw: String =
        if (content.breakingTheLaw.isNotEmpty()) {
            aggregatedText(text = content.breakingTheLaw)
        } else ""

    val lawDoesNotApply: String =
        if (content.lawDoesNotApply.isNotEmpty()) {
            aggregatedText(text = content.lawDoesNotApply)
        } else ""

    val tooHardToComply: String =
        if (content.tooHardToComply.isNotEmpty()) {
            aggregatedText(text = content.tooHardToComply)
        } else ""

    private fun aggregatedText(text: String) = """
            $text
            -- $reportTitle, kontaktperson: $contactPerson
        """.trimIndent()
}

