package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.aggregate
import assert
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SuccessCriterionTest {

    @Test
    fun `aggregerer sukkesskriterer basert på nummer`() {
        (testCriterion(number = "1.1.1", status = Status.COMPLIANT) * 5)
            .plus(testCriterion("1.2.3", status = Status.NOT_TESTED))
            .aggregate()
            .assert {
                size shouldBe 2
                first().assert {
                    number shouldBe "1.1.1"
                    name shouldBe "Criterion 1.1.1"
                    description shouldBe "description 1.1.1"
                    principle shouldBe "principle 1.1.1"
                    guideline shouldBe "guideline 1.1.1"
                    tools shouldBe "tools 1.1.1"
                    number shouldBe number
                    breakingTheLaw shouldBe ""
                    lawDoesNotApply shouldBe ""
                    tooHardToComply shouldBe ""
                    contentGroup shouldBe "contentgroup 1.1.1"
                    status shouldBe Status.COMPLIANT
                    wcagUrl shouldBe "wcagUrl 1.1.1"
                    helpUrl shouldBe "helpurl 1.1.1"
                    wcagVersion shouldBe "wcagversion 1.1.1"
                }
                get(1).assert {
                    number shouldBe "1.2.3"
                    status shouldBe Status.NOT_TESTED
                }
            }


    }

    @Test
    fun `resolver riktig status`() {
        (testCriterion(status = Status.COMPLIANT) * 5)
            .plus(testCriterion(status = Status.NOT_TESTED) * 5)
            .aggregate()
            .assert {
                size shouldBe 1
                first().status shouldBe Status.NOT_TESTED
            }

        (testCriterion(status = Status.COMPLIANT) * 5)
            .plus(testCriterion(status = Status.NOT_TESTED) * 5)
            .plus(testCriterion(status = Status.NON_COMPLIANT))
            .plus(testCriterion(status = Status.NOT_APPLICABLE))
            .aggregate()
            .assert {
                size shouldBe 1
                first().status shouldBe Status.NON_COMPLIANT
            }

        listOf(testCriterion(status = Status.NOT_APPLICABLE), testCriterion(status = Status.COMPLIANT))
            .aggregate()
            .first().status shouldBe Status.COMPLIANT

        (testCriterion(status = Status.NOT_APPLICABLE) * 5)
            .aggregate()
            .first().status shouldBe Status.NOT_APPLICABLE
    }


}

private operator fun SuccessCriterion.times(i: Int) = mutableListOf<SuccessCriterion>().apply {
    for (i in 1..i)
        add(this@times)
}

private fun testCriterion(
    number: String = "1.1.1",
    status: Status,
    breakingTheLaw: String = "",
    lawDoesNotApply: String = "",
    tooHardToComply: String = "",
) =
    SuccessCriterion(
        name = "Criterion $number",
        description = "description $number",
        principle = "principle $number",
        guideline = "guideline $number",
        tools = "tools $number",
        number = number,
        breakingTheLaw = breakingTheLaw,
        lawDoesNotApply = lawDoesNotApply,
        tooHardToComply = tooHardToComply,
        contentGroup = "contentgroup $number",
        status = status,
        wcagUrl = "wcagUrl $number",
        helpUrl = "helpurl $number",
        wcagVersion = "wcagversion $number"
    )
