package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.datestr
import assert
import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class TeamApiTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testOrg = OrganizationUnit(
        id = "1234567",
        name = "Testorganisation",
        email = "testorganisation@nav.no"
    )
    private val testOrg2 = OrganizationUnit(
        id = "one-two-three",
        name = "Testorganization",
        email = "testorganization@nav.no"
    )

    private val testorgsReports = listOf(dummyReportV2(orgUnit = testOrg), dummyReportV2(orgUnit = testOrg))

    @BeforeEach()
    fun populateDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }
        database.update { queryOf("delete from organization_unit") }
        repository.upsertOrganizationUnit(testOrg)
        repository.upsertOrganizationUnit(testOrg2)
        testorgsReports.forEach { report ->
            repository.upsertReport(report)
        }
        repository.upsertReport(dummyReportV2(orgUnit = testOrg2))
    }

    @Test
    fun `Hent Team reports`() = setupTestApi(database) {
        client.get("api/teams/${testOrg.id}/reports").assert {
            status shouldBe HttpStatusCode.OK
            val responseBody = bodyAsText()
            val jsonResponse = objectmapper.readTree(responseBody)
            jsonResponse.toList().assert {
                this.size shouldBe 2
                testorgsReports.forEach {
                    it.assertExists(this)
                }
            }
        }
    }

    @Test
    fun `Returns the details of a team`() = setupTestApi(database, true) {
        testOrg2.addMember(Email("tadda@nav.no"))
        testOrg2.addMember(Email("tadda1@nav.no"))
        testOrg2.addMember(Email("tadda2@nav.no"))
        testOrg2.addMember(Email("tadda3@nav.no"))
        repository.upsertOrganizationUnit(testOrg2)

        client.get("/api/teams/${testOrg2.id}/details").assert {
            status shouldBe OK
            objectmapper.readTree(bodyAsText()).assert {
                this["id"].asText() shouldBe testOrg2.id
                this["name"].asText() shouldBe testOrg2.name
                this["email"].asText() shouldBe testOrg2.email
                this["members"].toList().map { it.asText() } shouldContainAll testOrg2.members
            }
        }

        client.get("/api/teams/${testOrg.id}/details").assert {
            status shouldBe OK
            objectmapper.readTree(bodyAsText()).assert {
                this["id"].asText() shouldBe testOrg.id
                this["name"].asText() shouldBe testOrg.name
                this["email"].asText() shouldBe testOrg.email
                this["members"].toList().size shouldBe 0
            }
        }

        client.get("/api/teams/no-exists/details").status shouldBe NotFound
    }
}

private fun Report.assertExists(jsonList: List<JsonNode>) {
    val result = jsonList.find { jsonNode -> jsonNode["id"].asText() == reportId }
    require(result != null) { "Could not find report with id $reportId! Available nodes: ${jsonList.map { it.toString() }}" }
    result["teamId"].asText() shouldBe organizationUnit!!.id
    result["title"].asText() shouldBe descriptiveName
    result["date"].asText() shouldBe "yyyy-MM-dd".datestr(lastChanged)

}
