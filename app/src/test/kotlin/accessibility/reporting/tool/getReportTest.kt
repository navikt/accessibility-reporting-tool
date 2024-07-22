package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportType
import accessibility.reporting.tool.wcag.Version

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.css.Contain
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class getReportTest {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testUserEmail = Email("tadda@test.tadda")
    private val testUserName = "Tadda Taddasen"
    private val testUserOid = User.Oid(UUID.randomUUID().toString())
    private val dummyreport = dummyReportV2(orgUnit = testOrg)
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

    /*@BeforeEach
    fun cleanDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }

    }*/

    @BeforeEach
    fun populateDb() {
        repository.upsertReport(dummyreport)
    }

    @Test
    fun `get Report`() = setupTestApi(database) {
        val response=client.get("api/reports/${dummyreport.reportId}")
            response.status shouldBe HttpStatusCode.OK
            val responseBody = response.bodyAsText()
            val jsonResponse = objectmapper.readTree(responseBody)
            println("Response JSON: $jsonResponse")
        dummyreport.assertExists(jsonResponse)

    }

    private fun dummyReportV2(
        id: String =UUID.randomUUID().toString(),
        tittel: String = "Example Report",
        url: String ="https://www.example.com",
        user: User = User(email = testUserEmail, name = testUserName, oid = testUserOid, groups = listOf()),
        orgUnit: OrganizationUnit? = null,
        reportType: ReportType = ReportType.SINGLE,
    )= Report(
        reportId = id,
        url = url,
        organizationUnit = orgUnit,
        version = Version.V2,
        author = user.toAuthor(),
        successCriteria = Version.V2.criteria,
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        created = LocalDateTimeHelper.nowAtUtc(),
        lastUpdatedBy = null,
        descriptiveName = tittel,
        reportType = reportType
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

