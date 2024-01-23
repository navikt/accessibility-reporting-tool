package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.wcag.SuccessCriterion.Companion.aggregate
import assert
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SuccessCriterionTest {

    @Test
    fun `aggregerer sukkesskriterer basert på nummer`() {

        val testcriteria = (testCriterion(
            number = "1.1.1",
            status = Status.NON_COMPLIANT,
            breakingTheLaw = "nei",
            lawDoesNotApply = "law",
            tooHardToComply = "too hard"
        ) * 5)
            .plus(testCriterion("1.2.3", status = Status.NOT_TESTED))

        testcriteria.
        mapToSummary()
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
                    breakingTheLaw shouldBe """
                        nei
                        -- Testtittel, kontaktperson: testperson@tes.no
                        nei
                        -- Testtittel, kontaktperson: testperson@tes.no
                        nei
                        -- Testtittel, kontaktperson: testperson@tes.no
                        nei
                        -- Testtittel, kontaktperson: testperson@tes.no
                        nei
                        -- Testtittel, kontaktperson: testperson@tes.no
                    """.trimIndent()
                    lawDoesNotApply shouldBe """
                        law
                        -- Testtittel, kontaktperson: testperson@tes.no
                        law
                        -- Testtittel, kontaktperson: testperson@tes.no
                        law
                        -- Testtittel, kontaktperson: testperson@tes.no
                        law
                        -- Testtittel, kontaktperson: testperson@tes.no
                        law
                        -- Testtittel, kontaktperson: testperson@tes.no
                    """.trimIndent()
                    tooHardToComply shouldBe """
                        too hard
                        -- Testtittel, kontaktperson: testperson@tes.no
                        too hard
                        -- Testtittel, kontaktperson: testperson@tes.no
                        too hard
                        -- Testtittel, kontaktperson: testperson@tes.no
                        too hard
                        -- Testtittel, kontaktperson: testperson@tes.no
                        too hard
                        -- Testtittel, kontaktperson: testperson@tes.no
                    """.trimIndent()
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
            .mapToSummary()
            .aggregate()
            .assert {
                size shouldBe 1
                first().status shouldBe Status.NOT_TESTED
            }

        (testCriterion(status = Status.COMPLIANT) * 5)
            .plus(testCriterion(status = Status.NOT_TESTED) * 5)
            .plus(testCriterion(status = Status.NON_COMPLIANT))
            .plus(testCriterion(status = Status.NOT_APPLICABLE))
            .mapToSummary()
            .aggregate()
            .assert {
                size shouldBe 1
                first().status shouldBe Status.NON_COMPLIANT
            }

        listOf(testCriterion(status = Status.NOT_APPLICABLE), testCriterion(status = Status.COMPLIANT))
            .mapToSummary()
            .aggregate()
            .first().status shouldBe Status.COMPLIANT

        (testCriterion(status = Status.NOT_APPLICABLE) * 5)
            .mapToSummary()
            .aggregate()
            .first().status shouldBe Status.NOT_APPLICABLE
    }


}

private fun List<SuccessCriterion>.mapToSummary() = map {
    SuccessCriterionSummary(
        reportTitle = "Testtittel", contactPerson = "testperson@tes.no", content = it
    )
}

private operator fun SuccessCriterion.times(amount: Int) = mutableListOf<SuccessCriterion>().apply {
    for (i in 1..amount)
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
    ).apply {
        wcagLevel = WcagLevel.A
    }
