package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.toStringList
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import io.kotest.assertions.withClue
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.databind.JsonNode

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReportTest: TestApi() {

    private val testUser = TestUser(
        email = "tadda@test.tadda", name = "Tadda Taddasen",
    )
    private val teamMember = TestUser(
        email = "teammember@test.an",
        name = "Teamy Tems",
    )
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no"
    ).apply {
        addMember(teamMember.original.email)
    }

    private val dummyreport = dummyReportV2(orgUnit = testOrg, user = testUser.original)

    private val testUserAdmin = TestUser(
        email = "admin@test.an",
        name = "Admin adminson",
        groups = listOf(System.getenv("ADMIN_GROUP"))
    )


    private val notTeamMember = TestUser(
        email = "notteammember@test.an",
        name = "Noteamy Tems",
    )

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email, member) 
                    VALUES (:id,:name,:email, :members)
                """.trimMargin(),
                mapOf(
                    "id" to testOrg.id,
                    "name" to testOrg.name,
                    "email" to testOrg.email,
                    "members" to testOrg.members.toStringList()
                )
            )
        }
        reportRepository.upsertReport(dummyreport)
    }

    @Test
    fun `get Report`() = withTestApi{
        val responseForAuthor = client.getWithJwtUser(testUser.original, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForAuthor, testUser.original, true)

        val updateDescriptiveName = """
            {
                "reportId": "${dummyreport.reportId}",
                "descriptiveName": "newName"
            }
        """.trimIndent()
        val updatedReport = dummyreport.copy(
            descriptiveName = "newName",
            lastUpdatedBy = Author(email = testUserAdmin.original.email.str(), oid = testUserAdmin.original.oid.str())
        )

        client.patchWithJwtUser(testUserAdmin.original, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }.also { require(it.status == HttpStatusCode.OK) }

        val responseForAuthorCapitalised =
            client.getWithJwtUser(testUser.capitalized, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForAuthorCapitalised, testUser.capitalized, true)

        val responseForAdmin = client.getWithJwtUser(testUserAdmin.original, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForAdmin, testUserAdmin.original, true)
        val responseForAdminCapitalized =
            client.getWithJwtUser(testUserAdmin.capitalized, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForAdminCapitalized, testUserAdmin.capitalized, true)

        val responseForTeamMember = client.getWithJwtUser(teamMember.original, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForTeamMember, teamMember.original, true)
        val responseForTeamMemberCapitalized =
            client.getWithJwtUser(teamMember.capitalized, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForTeamMemberCapitalized, teamMember.capitalized, true)

        val responseForNonTeamMember =
            client.getWithJwtUser(notTeamMember.original, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForNonTeamMember, notTeamMember.original, false)
        val responseForNonTeamMemberCapitalized =
            client.getWithJwtUser(notTeamMember.capitalized, "api/reports/${dummyreport.reportId}")
        updatedReport.assertListItemExists(responseForNonTeamMemberCapitalized, notTeamMember.capitalized, false)

    }
}

private suspend fun Report.assertListItemExists(
    response: HttpResponse,
    user: User,
    shouldHaveWriteAccess: Boolean,
) {
    response.status shouldBe HttpStatusCode.OK
    val jsonNode = objectmapper.readTree(response.bodyAsText())
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
    withClue("${user.email.str()} has incorrect permissions on report") {
        jsonNode["hasWriteAccess"].asBoolean() shouldBe shouldHaveWriteAccess
    }
    jsonNode["created"].toLocalDateTime() shouldWithinTheSameMinuteAs this.created
    jsonNode["lastChanged"].toLocalDateTime() shouldWithinTheSameMinuteAs this.lastChanged
    jsonNode["lastUpdatedBy"].asText() shouldBe (lastUpdatedBy?.email ?: author.email)
}

private infix fun LocalDateTime.shouldWithinTheSameMinuteAs(date: LocalDateTime) {
    val dateformat = "yyyy-MM-dd HH:mm"
    dateformat.datestr(this) shouldBe dateformat.datestr(date)
}

private fun JsonNode.toLocalDateTime() =
    LocalDateTime.parse(asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

