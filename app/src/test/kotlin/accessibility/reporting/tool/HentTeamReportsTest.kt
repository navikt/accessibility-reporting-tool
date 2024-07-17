package accessibility.reporting.tool

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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)

class HentTeamReportsTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testOrg = OrganizationUnit(
        id = "1234567",
        name = "Testorganisation",
        email = "testorganisation@nav.no"
    )
    private val testOrg2 = OrganizationUnit(
        id = "1234568",
        name = "Testorganization",
        email = "testorganization@nav.no"
    )
    private val initialReports =
        listOf(dummyReportV2(orgUnit = testOrg), dummyReportV2(orgUnit = testOrg2), dummyReportV2(orgUnit = testOrg))

    @BeforeEach()
    fun populateDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }
        database.update { queryOf("delete from organization_unit") }
        repository.upsertOrganizationUnit(testOrg)
        repository.upsertOrganizationUnit(testOrg2)
        initialReports.forEach { report ->
            repository.upsertReport(report)
        }
    }

    @Test
    fun `Hent Team reports`() = setupTestApi(database) {
        client.get("api/teams/${testOrg.id}/reports").assert {
            status shouldBe HttpStatusCode.OK
            /*objectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 2
                initialReports.forEach {
                    it.assertExists(this)*/
            val responseBody = bodyAsText()
            val jsonResponse = objectmapper.readTree(responseBody)
            println("Response JSON: $jsonResponse")
            jsonResponse.toList().assert {
                this.size shouldBe 2
                initialReports.forEach {
                    it.assertExists(this)
                }
            }
        }
    }
}
private fun Report.assertExists(jsonList: List<JsonNode>) {
    val result = jsonList.find { jsonNode ->
        val nameNode = jsonNode.get("name")
        nameNode?.asText() == descriptiveName
    }
    require(result != null) { "Result is null! Available nodes: ${jsonList.map { it.toString() }}" }
    val urlNode = result?.get("url")
    require(urlNode != null) { "URL node is missing in the result: $result" }
    urlNode.asText() shouldBe this.url
}