package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import assert
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class UserSummaryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val organizationRepository = OrganizationRepository(database)
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    /*private val testOrg2 = OrganizationUnit(
        id = "1234568",
        name = "Testorganization",
        email = "testorganization@nav.no"
    )*/

    private val testOrg2 = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg1",
        email = "test@nav1.no",
        members = mutableSetOf()
    )

    private val testUser = User(
        email = User.Email(s = "test@test.nav"),
        name = "Test Testlin",
        oid = User.Oid(s = "1234568"),
        groups = listOf()
    )
    private val initialReports =
        listOf(
            dummyReportV2(orgUnit = testOrg, user = testUser, descriptiveName = "report1"),
            dummyReportV2(orgUnit = testOrg2),
            dummyReportV2(orgUnit = testOrg)
        )

    @BeforeEach()
    fun populateDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }
        database.update { queryOf("delete from organization_unit") }
        repository.upsertOrganizationUnit(testOrg)
        repository.upsertOrganizationUnit(testOrg2)
            testOrg2.addMember(testUser.email)
        repository.upsertOrganizationUnit(testOrg2)
        initialReports.forEach { report ->
            repository.upsertReport(report)
        }
    }


    @Test
    fun `Hent User Summary`() = setupTestApi(database) {
        client.getWithJwtUser(testUser, "api/users/details").assert {
            status shouldBe HttpStatusCode.OK
            val responseBody = bodyAsText()
            val jsonResponse = objectmapper.readTree(responseBody)
            println("Response JSON: $jsonResponse")
            jsonResponse["email"].asText() shouldBe testUser.email.str()
            jsonResponse["reports"].toList().assert {
                this.size shouldBe 1
                val report = find { jsonNode -> jsonNode["descriptiveName"].asText() == "report1" }
                require(report != null)
            }
            jsonResponse["teams"].toList().assert {
                this.size shouldBe 1
            }
        }
    }
}
/*private fun Report.assertExists(jsonList: List<JsonNode>) {
    val result = jsonList.find { jsonNode ->
        val nameNode = jsonNode.get("name")
        nameNode?.asText() == descriptiveName
    }
    require(result != null) { "Result is null! Available nodes: ${jsonList.map { it.toString() }}" }
    val urlNode = result?.get("url")
    require(urlNode != null) { "URL node is missing in the result: $result" }
    urlNode.asText() shouldBe this.url
}*/

