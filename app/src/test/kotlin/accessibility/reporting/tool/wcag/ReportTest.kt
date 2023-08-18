package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import assert
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test

import java.util.UUID

class ReportTest {
    private val testOrg = OrganizationUnit.createNew(name = "Test organisasjonsenhet", email = "test@nav.no")
    private val testUser = User("testuser@nav.no", "Test User")

    @Test
    fun `Finner kriterie basert på nummer`() {
        val testReport = Version1.newReport(
            organizationUnit = testOrg,
            reportId = UUID.randomUUID().toString(),
            url = "https://test.nav.no",
            user = testUser
        )

        testReport.findCriterion("1.1.1") shouldBe Version1.criteriaTemplate.first()
        shouldThrow<IllegalArgumentException> {
            testReport.findCriterion("3.4.4")
        }
    }

    @Test
    fun `lager kopi av rapport med oppdatert sukksesskriterie`() {
        val testReport = Version1.newReport(
            organizationUnit = testOrg,
            reportId = UUID.randomUUID().toString(),
            url = "https://test.nav.no",
            user = testUser
        )

        val testUpdatedCriterion = Version1.criteriaTemplate.find { it.number == "1.3.2" }!!.copy(
            status = Status.NON_COMPLIANT,
            breakingTheLaw = "not cool"
        )

        val updatedReport = testReport.withUpdatedCriterion(testUpdatedCriterion)
        updatedReport.assert {
            reportId shouldBe testReport.reportId
            user shouldBe testReport.user
            organizationUnit shouldBe testReport.organizationUnit
            url shouldBe testReport.url
            lastChanged shouldBeAfter testReport.lastChanged

            successCriteria.forEach {newReportCriterion ->
                if (newReportCriterion.number != testUpdatedCriterion.number) {
                    newReportCriterion shouldBe  Version1.criteriaTemplate.find { it.number == newReportCriterion.number }
                }
                else {
                    newReportCriterion shouldNotBe Version1.criteriaTemplate.find { it.number == "1.3.2" }
                    newReportCriterion.status shouldBe Status.NON_COMPLIANT
                    newReportCriterion.breakingTheLaw shouldBe "not cool"
                }
            }
        }
        val testUpdatedCriterion2 = Version1.criteriaTemplate.find { it.number == "4.1.1" }!!.copy(
            status = Status.COMPLIANT
        )

        updatedReport.withUpdatedCriterion(testUpdatedCriterion2).assert{
            successCriteria.count { it.status != Status.NOT_TESTED } shouldBe 2
        }

    }
}