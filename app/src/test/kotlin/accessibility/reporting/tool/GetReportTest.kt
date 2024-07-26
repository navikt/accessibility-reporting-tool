package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.database.toStringList
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import io.kotest.assertions.withClue
import accessibility.reporting.tool.wcag.*

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetReportTest {

    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
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
        repository.upsertReport(dummyreport)
    }

    @Test
    fun `get Report`() = setupTestApi(database) {
        val responseForAuthor = client.getWithJwtUser(testUser.original, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForAuthor, testUser.original, true)
        val responseForAuthorCapitalised =
            client.getWithJwtUser(testUser.capitalized, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForAuthorCapitalised, testUser.capitalized, true)

        val responseForAdmin = client.getWithJwtUser(testUserAdmin.original, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForAdmin, testUserAdmin.original, true)
        val responseForAdminCapitalized =
            client.getWithJwtUser(testUserAdmin.capitalized, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForAdminCapitalized, testUserAdmin.capitalized, true)

        val responseForTeamMember = client.getWithJwtUser(teamMember.original, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForTeamMember, teamMember.original, true)
        val responseForTeamMemberCapitalized =
            client.getWithJwtUser(teamMember.capitalized, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForTeamMemberCapitalized, teamMember.capitalized, true)

        val responseForNonTeamMember =
            client.getWithJwtUser(notTeamMember.original, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForNonTeamMember, notTeamMember.original, false)
        val responseForNonTeamMemberCapitalized =
            client.getWithJwtUser(notTeamMember.capitalized, "api/reports/${dummyreport.reportId}")
        dummyreport.assertListItemExists(responseForNonTeamMemberCapitalized, notTeamMember.capitalized, false)

    }
}

private suspend fun Report.assertListItemExists(response: HttpResponse, user: User, shouldHaveWriteAccess: Boolean) {
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
    val dateFormat = "yyyy.MM.dd HH:mm:ss"
    jsonNode["created"].asText() shouldBe dateFormat.datestr(this.created)
    jsonNode["lastChanged"].asText() shouldBe dateFormat.datestr(this.lastChanged)
}

