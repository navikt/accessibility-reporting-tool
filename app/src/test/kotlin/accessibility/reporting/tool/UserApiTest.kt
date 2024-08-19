package accessibility.reporting.tool

import assert
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class UserApiTest: TestApi() {

    private val testOrg = createTestOrg(
        name = "DummyOrg",
        email = "test@nav.no"
    )

    private val testOrg2 = createTestOrg(
        name = "DummyOrg1",
        email = "test@nav1.no",
    )

    private val testUser = TestUser(
        email =  "test@test.nav",
        name = "Test Testlin",
    )

    private val testReport = dummyReportV4(orgUnit = testOrg, user = testUser, descriptiveName = "report1")
    private val initialReports =
        listOf(
            testReport,
            dummyReportV4(orgUnit = testOrg2),
            dummyReportV4(orgUnit = testOrg)
        )

    @BeforeEach
    fun populateDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }
        database.update { queryOf("delete from organization_unit") }
        organizationRepository.upsertOrganizationUnit(testOrg)
        organizationRepository.upsertOrganizationUnit(testOrg2)
        testOrg2.addMember(testUser.email)
        organizationRepository.upsertOrganizationUnit(testOrg2)
        initialReports.forEach { report ->
            reportRepository.upsertReport(report)
        }
    }


    @Test
    fun `Hent User Summary`() = withTestApi {
        client.getWithJwtUser(testUser, "api/users/details").assert {
            status shouldBe HttpStatusCode.OK
            val responseBody = bodyAsText()
            val jsonResponse = testApiObjectmapper.readTree(responseBody)
            println("Response JSON: $jsonResponse")
            jsonResponse["email"].asText() shouldBe testUser.email.str()
            jsonResponse["reports"].toList().assert {
                this.size shouldBe 1
                val report = find { jsonNode -> jsonNode["title"].asText() == "report1" }
                require(report != null)
                report["title"].asText() shouldBe testReport.descriptiveName
                report["id"].asText() shouldBe testReport.reportId
                report["date"].asText() shouldBe "yyyy-MM-dd".today()
                report["teamId"].asText() shouldBe testReport.organizationUnit!!.id
            }
            jsonResponse["teams"].toList().assert {
                this.size shouldBe 1
                first().assert {
                    this["name"].asText() shouldBe testOrg2.name
                    this["id"].asText() shouldBe testOrg2.id
                    this["email"].asText() shouldBe testOrg2.email
                }
            }
            jsonResponse["name"].asText() shouldBe testUser.name
        }
    }
}

private fun String.today() = let {
    val formatter = DateTimeFormatter.ofPattern(this)
    LocalDateTime.now().format(formatter)
}
