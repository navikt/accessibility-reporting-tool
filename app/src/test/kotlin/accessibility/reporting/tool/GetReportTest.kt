package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.toStringList
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReportTest : TestApi() {

    private val testUser = TestUser(
        email = "tadda@test.tadda", name = "Tadda Taddasen",
    )
    private val teamMember = TestUser(
        email = "teammember@test.an",
        name = "Teamy Tems",
    )
    private val testOrg = createTestOrg(
        name = "DummyOrg",
        email = "test@nav.no",
        teamMember.email.str()
    )

    private val dummyReport = dummyReportV4(orgUnit = testOrg, user = testUser)

    private val testUserAdmin = TestUser.createAdminUser(
        email = "admin@test.an",
        name = "Admin adminson",
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
        reportRepository.upsertReport(dummyReport)
    }

    @Test
    fun `get Report`() = withTestApi {
        val responseForAuthor = client.getWithJwtUser(testUser, "api/reports/${dummyReport.reportId}")
        dummyReport.assertListItemExists(responseForAuthor, testUser.original, true)

        val updateDescriptiveName = """
            {
                "reportId": "${dummyReport.reportId}",
                "descriptiveName": "newName"
            }
        """.trimIndent()
        val updatedReport = dummyReport.copy(
            descriptiveName = "newName",
            lastUpdatedBy = Author(email = testUserAdmin.email.str(), oid = testUserAdmin.oid.str())
        )

        client.patchWithJwtUser(testUserAdmin.original, "api/reports/${dummyReport.reportId}/update") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }.also { require(it.status == HttpStatusCode.OK) }

        val responseForAuthorCapitalised =
            client.getWithJwtUser(testUser.capitalized, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForAuthorCapitalised, testUser.capitalized, true)

        val responseForAdmin = client.getWithJwtUser(testUserAdmin.original, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForAdmin, testUserAdmin.original, true)
        val responseForAdminCapitalized =
            client.getWithJwtUser(testUserAdmin.capitalized, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForAdminCapitalized, testUserAdmin.capitalized, true)

        val responseForTeamMember = client.getWithJwtUser(teamMember.original, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForTeamMember, teamMember.original, true)
        val responseForTeamMemberCapitalized =
            client.getWithJwtUser(teamMember.capitalized, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForTeamMemberCapitalized, teamMember.capitalized, true)

        val responseForNonTeamMember =
            client.getWithJwtUser(notTeamMember.original, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForNonTeamMember, notTeamMember.original, false)
        val responseForNonTeamMemberCapitalized =
            client.getWithJwtUser(notTeamMember.capitalized, "api/reports/${dummyReport.reportId}")
        updatedReport.assertListItemExists(responseForNonTeamMemberCapitalized, notTeamMember.capitalized, false)

    }

    private suspend fun Report.assertListItemExists(
        response: HttpResponse,
        user: User,
        shouldHaveWriteAccess: Boolean,
    ) {
        response.status shouldBe HttpStatusCode.OK
        val jsonNode = testApiObjectmapper.readTree(response.bodyAsText())
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
        jsonNode["isPartOfNavNo"].asBoolean() shouldBe true
        jsonNode["notes"].asText() shouldBe this.notes
    }
}

private infix fun LocalDateTime.shouldWithinTheSameMinuteAs(date: LocalDateTime) {
    val dateformat = "yyyy-MM-dd HH:mm"
    dateformat.datestr(this) shouldBe dateformat.datestr(date)
}

private fun JsonNode.toLocalDateTime() =
    LocalDateTime.parse(asText(), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))

