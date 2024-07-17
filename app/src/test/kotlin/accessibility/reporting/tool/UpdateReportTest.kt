package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateReportTest {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testUser =
        User(User.Email("testuser@nav.no"), "Test User", User.Oid(UUID.randomUUID().toString()), groups = listOf())
    private val dummyreport = dummyReportV2(orgUnit = testOrg)
    private val objectmapper = jacksonObjectMapper().apply{
        registerModule(JavaTimeModule())
    }

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name, :email) 
                """.trimMargin(),
                mapOf(
                    "id" to testOrg.id,
                    "name" to testOrg.name,
                    "email" to testOrg.email
                )
            )
        }
    }

    @BeforeEach
    fun populateDb() {
        repository.upsertReport(dummyreport)
    }

    @Test
    fun `update success criteria`() = setupTestApi(database) {
        val newCriteria = listOf(
            SuccessCriterion(
                name ="tull",
                description ="tull",
                principle ="tull",
                guideline ="tull",
                tools ="tull",
                number ="tull",
                breakingTheLaw ="tull",
                lawDoesNotApply ="tull",
                tooHardToComply ="tull",
                contentGroup ="tull",
                status =Status.COMPLIANT
            ).apply {
                wcagLevel = WcagLevel.AA
            }
        )

        val updatedReport = dummyreport.updateCriteria(newCriteria, testUser)


        val response = client.put("api/reports/${dummyreport.reportId}/update") {
            setBody(
                objectmapper.writeValueAsString(updatedReport)
            )
            contentType(
                ContentType.Application.Json
            )
            // legg til header ContentType -> application/json
        }

        response.status shouldBe HttpStatusCode.OK
        val responseBody = response.bodyAsText()
        val jsonResponse = objectmapper.readTree(responseBody)
        jsonResponse["id"].asText() shouldBe dummyreport.reportId

        val updatedResponse = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse.status shouldBe HttpStatusCode.OK
        val updatedResponseBody = updatedResponse.bodyAsText()
        val updatedJsonResponse = objectmapper.readTree(updatedResponseBody)
        dummyreport.assertExists(updatedJsonResponse)
        assertAll(

            { assertEquals(newCriteria, updatedReport.successCriteria, "Success criteria not updated correctly") },
            { assertNotNull(updatedReport.lastChanged, "Last changed time not updated") },

            )

    }
    private fun Report.assertExists(jsonNode: JsonNode) {
        jsonNode["reportId"].asText() shouldBe this.reportId
        jsonNode["url"].asText() shouldBe this.url
        jsonNode["descriptiveName"].asText() shouldBe this.descriptiveName
        jsonNode["team"].let {
            it["id"].asText() shouldBe this.organizationUnit?.id
            it["name"].asText() shouldBe this.organizationUnit?.name
            it["email"].asText() shouldBe this.organizationUnit?.email
        }
        jsonNode["author"].let {
            it["email"].asText() shouldBe this.author.email
            it["oid"].asText() shouldBe this.author.oid
        }
    }

}