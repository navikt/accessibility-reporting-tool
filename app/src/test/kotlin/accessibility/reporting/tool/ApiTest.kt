package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import assert
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest {

    val objectmapper = jacksonObjectMapper()


    private val repository = ReportRepository(LocalPostgresDatabase.cleanDb())
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
        listOf(dummyReportV2(orgUnit = testOrg), dummyReportV2(orgUnit = testOrg), dummyReportV2(orgUnit = testOrg))


    @BeforeAll()
    fun populateDb() {
        repository.upsertOrganizationUnit(testOrg)
        repository.upsertOrganizationUnit(testOrg2)
        initialReports.forEach { report ->
            repository.upsertReport(report)
        }
    }

    @Test
    fun `Returns a summary of of all reports`() = testApplication {
        application {
            api(repository = repository, corsAllowedOrigins = "*", corsAllowedSchemes = listOf("http","https")) { mockEmptyAuth() }
        }

        client.get("api/reports/list").assert {
            status shouldBe HttpStatusCode.OK
            objectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 3
                initialReports.forEach {
                    it.assertExists(this)
                }
            }
        }
    }

    @Test
    fun `Returns a summary of of all teams`() = testApplication {
        application {
            api(repository = repository, corsAllowedOrigins = "*", corsAllowedSchemes = listOf("http","https")) { mockEmptyAuth() }
        }

        client.get("api/teams").assert {
            status shouldBe HttpStatusCode.OK
            objectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 2
                val org = this.find {it["id"].asText() == "1234567"}
                require(org!=null) {"org is null"}
                org["name"].asText() shouldBe "Testorganisation"
                org["email"].asText() shouldBe "testorganisation@nav.no"
                val org2 = this.find {it["id"].asText() == "1234568"}
                require(org2!=null) {"org is null"}
                org2["name"].asText() shouldBe "Testorganization"
                org2["email"].asText() shouldBe "testorganization@nav.no"

            }
        }
    }

}



private fun Report.assertExists(jsonList: List<JsonNode>) {
    val result = jsonList.find { jsonNode -> jsonNode["navn"].asText() == descriptiveName }
    require(result != null)
    result["url"].asText() shouldBe this.url
}

private fun Application.mockEmptyAuth() = authentication {
    jwt {
        skipWhen { true }
    }
}