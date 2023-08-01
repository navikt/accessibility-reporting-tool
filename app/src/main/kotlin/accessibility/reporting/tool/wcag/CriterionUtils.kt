package accessibility.reporting.tool.wcag

class SuccessCriterionInfo {
    lateinit var name: String
    lateinit var description: String
    lateinit var principle: String
    lateinit var guideline: String
    lateinit var tools: String
    lateinit var number: String
    var contentGroup: String? = null
    lateinit var wcagUrl: String
    var helpUrl: String? = null

    fun buildCriterion() =
        SuccessCriterion(
            name = name,
            description = description,
            principle = principle,
            guideline = guideline,
            tools = tools,
            number = number,
            breakingTheLaw = "",
            lawDoesNotApply = "",
            tooHardToComply = "",
            contentGroup = contentGroup ?: "",
            status = Status.NOT_TESTED,
            wcagUrl = wcagUrl,
            helpUrl = helpUrl,
        )


    companion object {

        private fun initCriteria(number: String, name: String, principle: String) =
            SuccessCriterionInfo().apply {
                this.principle = principle
                this.number = number
                this.name = name
            }

        fun Int.perceivable(
            number: String,
            name: String,
            updateInfo: SuccessCriterionInfo.() -> Unit
        ): SuccessCriterion {
            if (this != 1) { throw IllegalArgumentException("$this is not the number of principle 1.Perceivable") }
            return initCriteria(number, name, "1.  Mulig å oppfatte").apply(updateInfo).buildCriterion()
        }

        fun Int.operable(number: String, name: String, updateInfo: SuccessCriterionInfo.() -> Unit): SuccessCriterion {
            if (this != 2) { throw IllegalArgumentException("$this is not the number of principle 2.Operable") }
            return initCriteria(number, name, "2. Mulig å betjene").apply(updateInfo).buildCriterion()
        }

        fun Int.understandable(
            number: String,
            name: String,
            updateInfo: SuccessCriterionInfo.() -> Unit
        ): SuccessCriterion {
            if (this != 3) { throw IllegalArgumentException("$this is not the number of principle 3.Understandabble") }
            return initCriteria(number, name, "3. Forståelig").apply(updateInfo).buildCriterion()
        }

        fun Int.robust(number: String, name: String, updateInfo: SuccessCriterionInfo.() -> Unit): SuccessCriterion {
            if (this != 4) { throw IllegalArgumentException("$this is not the number of principle 4.Understandabble") }
            return initCriteria(number, name, "4. Robust").apply(updateInfo).buildCriterion()
        }
    }
}