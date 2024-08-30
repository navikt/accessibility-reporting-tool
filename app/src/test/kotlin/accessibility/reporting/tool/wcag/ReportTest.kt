package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.html.toEmail
import assert
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test

import java.util.UUID

class ReportTest {
    private val testOrg = OrganizationUnit.createNew(name = "Test organisasjonsenhet", email = "test@nav.no")
    private val testUser =
        User(User.Email("testuser@nav.no"), "Test User", User.Oid(UUID.randomUUID().toString()), groups = listOf())

    @Test
    fun `Finner kriterie basert på nummer`() {
        val testReport = SucessCriteriaV1.newReport(
            organizationUnit = testOrg,
            reportId = UUID.randomUUID().toString(),
            url = "https://test.nav.no",
            user = testUser,
            descriptiveName = "Some name",
            isPartOfNavNo = false
        )

        testReport.findCriterion("1.1.1") shouldBe SucessCriteriaV1.criteriaTemplate.first()
        shouldThrow<IllegalArgumentException> {
            testReport.findCriterion("3.4.4")
        }
    }

    @Test
    fun `lager kopi av rapport med oppdatert sukksesskriterie`() {
        val testReport = SucessCriteriaV1.newReport(
            organizationUnit = testOrg,
            reportId = UUID.randomUUID().toString(),
            url = "https://test.nav.no",
            user = testUser,
            descriptiveName = "some name 2",
            isPartOfNavNo = false
        )

        val testUpdatedCriterion = SucessCriteriaV1.criteriaTemplate.find { it.number == "1.3.2" }!!.copy(
            status = Status.NON_COMPLIANT,
            breakingTheLaw = "not cool"
        )

        val updatedReport = testReport.withUpdatedCriterion(testUpdatedCriterion, testUser)
        updatedReport.assert {
            reportId shouldBe testReport.reportId
            author shouldBe testReport.author
            organizationUnit shouldBe testReport.organizationUnit
            url shouldBe testReport.url
            lastChanged shouldBeAfter testReport.lastChanged

            successCriteria.forEach { newReportCriterion ->
                if (newReportCriterion.number != testUpdatedCriterion.number) {
                    newReportCriterion shouldBe SucessCriteriaV1.criteriaTemplate.find { it.number == newReportCriterion.number }
                } else {
                    newReportCriterion shouldNotBe SucessCriteriaV1.criteriaTemplate.find { it.number == "1.3.2" }
                    newReportCriterion.status shouldBe Status.NON_COMPLIANT
                    newReportCriterion.breakingTheLaw shouldBe "not cool"
                }
            }
        }
        val testUpdatedCriterion2 = SucessCriteriaV1.criteriaTemplate.find { it.number == "4.1.1" }!!.copy(
            status = Status.COMPLIANT
        )

        val contributor =
            User(
                "other.user@test.ja".toEmail(),
                "Contributor Contributerson",
                User.Oid(UUID.randomUUID().toString()),
                groups = listOf()
            )
        updatedReport.withUpdatedCriterion(testUpdatedCriterion2, contributor).assert {
            successCriteria.count { it.status != Status.NOT_TESTED } shouldBe 2
            contributors.size shouldBe 1
            contributors.first().assert {
                oid shouldBe contributor.oid.str()
                email shouldBe contributor.email.str()
            }
            author.email shouldBe testReport.author.email
            author.oid shouldBe testReport.author.oid
            lastUpdatedBy.assert {
                require(this != null)
                email shouldBe contributor.email.str()
                oid shouldBe contributor.oid.str()
            }
        }
    }

    @Test
    fun tilgangsjekk() {
        mockkStatic(Admins::class)

        val memberUser = User(
            email = User.Email("member@member.test"),
            name = "Member Membersen",
            oid = User.Oid("member-oid"),
            groups = listOf()
        )

        val testReport = SucessCriteriaV1.newReport(
            organizationUnit = testOrg,
            reportId = UUID.randomUUID().toString(),
            url = "https://test.nav.no",
            user = testUser,
            descriptiveName = "Some name",
            isPartOfNavNo = true
        )
        testReport.writeAccess(testUser) shouldBe true
        testReport.writeAccess(memberUser) shouldBe false

        testOrg.addMember(memberUser.email)
        val updatedReport = testReport.withUpdatedMetadata(
            organizationUnit = testOrg,
            updateBy = memberUser,
        )
        updatedReport.writeAccess(memberUser) shouldBe true

        updatedReport.writeAccess(
            User(
                email = User.Email("Member@Member.test"),
                name = "Member Membersen",
                oid = User.Oid("member-oid"),
                groups = listOf()
            )
        ) shouldBe true
    }
}
