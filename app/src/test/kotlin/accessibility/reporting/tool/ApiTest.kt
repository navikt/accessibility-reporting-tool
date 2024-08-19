package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.datestr
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
import java.time.LocalDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiTest : TestApi() {

    private val testOrg = createTestOrg(
        name = "Testorganisation",
        email = "testorganisation@nav.no"
    )

    private val testUser = TestUser(
        email = "test@test.nav",
        name = "Test Testlin",
    )

    private val testOrg2 = createTestOrg(
        name = "Testorganization2",
        email = "testorganization2@nav.no"
    )
    private val initialReports =
        listOf(dummyReportV4(orgUnit = testOrg), dummyReportV4(orgUnit = testOrg), dummyReportV4(orgUnit = testOrg))


    @BeforeEach()
    fun populateDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }
        database.update { queryOf("delete from organization_unit") }
        organizationRepository.upsertOrganizationUnit(testOrg)
        organizationRepository.upsertOrganizationUnit(testOrg2)
        initialReports.forEach { report ->
            reportRepository.upsertReport(report)
        }
    }

    @Test
    fun `Returns a summary of of all reports`() = withTestApi {
        client.get("api/reports/list").assert {
            status shouldBe HttpStatusCode.OK
            testApiObjectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 3
                initialReports.forEach {
                    it.assertListItemExists(this)
                }
            }
        }
    }

    @Test
    fun `Returns a summary of of all teams`() = withTestApi {
        client.get("api/teams").assert {
            status shouldBe HttpStatusCode.OK
            testApiObjectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 2
                val org = this.find { it["id"].asText() == testOrg.id }
                require(org != null) { "org is null" }
                org["name"].asText() shouldBe testOrg.name
                org["email"].asText() shouldBe testOrg.email
                val org2 = this.find { it["id"].asText() == testOrg2.id }
                require(org2 != null) { "org is null" }
                org2["name"].asText() shouldBe testOrg2.name
                org2["email"].asText() shouldBe testOrg2.email

            }
        }
    }

    @Test
    fun `Create a new team `() = withTestApi {

        client.postWithJwtUser(testUser, "api/teams/new") {
            setBody(
                """{
                    "name": "team 1",
                    "email": "abc@gmail.no",
                    "members": ["abc","def","ghi"]
                    }
                    
                """.trimMargin()
            )
            contentType(
                ContentType.Application.Json
            )
        }.assert {
            status shouldBe HttpStatusCode.OK
        }
        client.postWithJwtUser(testUser, "api/teams/new") {
            setBody(
                """{
                    "name": "team 2",
                    "email": "abdd@gmail.no",
                    "members": ["abc","def","ghi"]
                  }  
          """.trimMargin()
            )
            contentType(
                ContentType.Application.Json
            )
        }.assert {
            status shouldBe HttpStatusCode.OK
        }
        client.get("api/teams").assert {
            status shouldBe HttpStatusCode.OK
            testApiObjectmapper.readTree(bodyAsText()).toList().assert {
                this.size shouldBe 4
                val org = this.find { it["name"].asText() == "team 1" }
                require(org != null) { "org is null" }

                org["email"].asText() shouldBe "abc@gmail.no"
                val org2 = this.find { it["name"].asText() == "team 2" }
                require(org2 != null) { "org is null" }

                org2["email"].asText() shouldBe "abdd@gmail.no"

            }
        }
    }
}

private fun Report.assertListItemExists(jsonList: List<JsonNode>) {
    val result = jsonList.find { jsonNode -> jsonNode["id"].asText() == reportId }
    require(result != null)

    withJsonClue("title") { titleField ->
        result[titleField].asText() shouldBe descriptiveName
    }
    withJsonClue("teamName") { teamNameField ->
        result[teamNameField].asText() shouldBe this.organizationUnit!!.name
    }
    withJsonClue("teamId") { teamIdField ->
        result[teamIdField].asText() shouldBe this.organizationUnit!!.id
    }
    withJsonClue("date") { dateField ->
        result[dateField].asText() shouldBe "yyyy-MM-dd".datestr(LocalDateTime.now())
    }
}