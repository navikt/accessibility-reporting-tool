package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportContent
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AdminAggregatedReportTest : TestApi() {
    private val adminRoute = "api/admin"
    private val testOrg = OrganizationUnit(
        id = "1234567",
        name = "Testorganisation",
        email = "testorganisation@nav.no"
    )

    private val testUser = TestUser(name = "Samesame Tester").original
    private val adminUser = TestUser.createAdminUser(name = "Admin adminson").original

    private val testOrg2 = createTestOrg(
        name = "Testorganization",
        email = "testorganization@nav.no"
    )
    private val admintestorg = createTestOrg(name = "Admin org", adminUser.email.str())
    private val testAggregatedReports = listOf(
        dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport1"),
        dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport2")
    )

    private val testReports =
        listOf(
            dummyReportV4(orgUnit = testOrg, user = testUser, descriptiveName = "dummyReport1"),
            dummyReportV4(orgUnit = testOrg, descriptiveName = "dummyRepoort2"),
            dummyReportV4(orgUnit = testOrg2, descriptiveName = "dummyreport3"),

            )


    @BeforeEach()
    fun populateDb() {
        cleanDb()
        organizationRepository.upsertOrganizationUnit(testOrg)
        organizationRepository.upsertOrganizationUnit(testOrg2)
        testReports.forEach { report ->
            reportRepository.upsertReport(report)
        }
        testAggregatedReports.forEach { aggregatedReport ->
            reportRepository.upsertReport(aggregatedReport)
        }
    }

    @Test
    fun `returns all aggregated reports`() = withTestApi {
        client.getWithJwtUser(adminUser, "api/reports/aggregated").assert {
            json().toList().assert {
                size shouldBe 2
                map { it["title"].asText() } shouldContainAll testAggregatedReports.map { it.descriptiveName }
            }

        }

    }

    @Test
    fun `creates a new aggregated report`() = withTestApi {
        val response = postWithAdminAssertion("$adminRoute/reports/aggregated/new", HttpStatusCode.Created) {
            setBody(
                """
                {
                   "title": "Some title",
                   "url":"Some url", 
                   "notes": "Here is a noooote",
                   "reports": ${testReports.jsonList()}
                }
            """.trimIndent()
            )
        }
        val id = response.json()["id"].asText()
        client.getWithJwtUser(testUser, "api/reports/aggregated/$id").assert {
            status shouldBe HttpStatusCode.OK
            val body = json()
            body["fromTeams"].toList().assert {
                size shouldBe 2
                map { it["name"].asText() } shouldContainAll listOf(testOrg.name, testOrg2.name)
                map { it["id"].asText() } shouldContainAll listOf(testOrg.id, testOrg2.id)
            }
            body["fromReports"].toList().assert {
                size shouldBe 3
                map { it["title"].asText() } shouldContainAll testReports.map { it.descriptiveName }
                map { it["reportId"].asText() } shouldContainAll testReports.map { it.reportId }
            }
            body["notes"].asText() shouldBe "Here is a noooote"

        }
        //bad request if one of the reports is an aggregated report
    }

    @Nested
    inner class UpdateAggregatedReport {
        @Disabled("todo")
        @Test
        fun `updates metadata of an aggregated report`() = withTestApi {
            //bad request if attempting to update an aggregated report as a single report
            //    assertPatchWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")
        }


        @Disabled("todo")
        @Test
        fun `updates a single successcriterion in an aggregated report`() = withTestApi {
            // assertPatchWithAdminIsOk("reports/${testReport.reportId}")
        }

    }

    @Disabled("todo")
    @Test
    fun `deletes an aggregated report`() = withTestApi {
        //  assertDeleteWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")

    }


    private suspend fun ApplicationTestBuilder.getWithAdminAccessChek(
        url: String,
        expectedAdminStatusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpResponse {
        client.getWithJwtUser(testUser, url).status shouldBe HttpStatusCode.Forbidden
        client.get(url).status shouldBe HttpStatusCode.Unauthorized
        val adminResponse = client.getWithJwtUser(adminUser, url)
        adminResponse.status shouldBe expectedAdminStatusCode
        return adminResponse
    }

    private suspend fun ApplicationTestBuilder.assertPatchWithAdminIsOk(
        url: String,
        expectedAdminStatusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpResponse {
        client.patchWithJwtUser(testUser, url).assert {
            withClue("user without admin check fails for $url") { status shouldBe HttpStatusCode.Forbidden }
        }
        client.patch(url).assert {
            withClue("unauthenicated user check fails for $url") { status shouldBe HttpStatusCode.Unauthorized }
        }
        val adminResponse = client.patchWithJwtUser(adminUser, url)
        withClue("admin user check fails for $url") { adminResponse.status shouldBe expectedAdminStatusCode }
        return adminResponse
    }

    private suspend fun ApplicationTestBuilder.postWithAdminAssertion(
        url: String,
        expectedAdminStatusCode: HttpStatusCode = HttpStatusCode.OK,
        block: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        client.postWithJwtUser(testUser, url, block).assert {
            withClue("user without admin check fails for $url") { status shouldBe HttpStatusCode.Forbidden }
        }
        client.post(url).assert {
            withClue("unauthenicated user check fails for $url") { status shouldBe HttpStatusCode.Unauthorized }
        }
        val adminResponse = client.postWithJwtUser(adminUser, url, block)
        withClue("admin user check fails for $url") { adminResponse.status shouldBe expectedAdminStatusCode }
        return adminResponse
    }

    private suspend fun ApplicationTestBuilder.assertDeleteWithAdminIsOk(
        url: String,
        expectedAdminStatusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpResponse {
        client.deleteWithJwtUser(testUser, url).assert {
            withClue("user without admin check fails for $url") { status shouldBe HttpStatusCode.Forbidden }
        }
        client.delete(url).assert {
            withClue("unauthenicated user check fails for $url") { status shouldBe HttpStatusCode.Unauthorized }
        }
        val adminResponse = client.deleteWithJwtUser(adminUser, url)
        withClue("admin user check fails for $url") { adminResponse.status shouldBe expectedAdminStatusCode }
        return adminResponse
    }

}


private fun List<ReportContent>.jsonList() =
    joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${it.reportId}\"" }