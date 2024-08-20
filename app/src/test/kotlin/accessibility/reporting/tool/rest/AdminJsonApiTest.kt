package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.wcag.OrganizationUnit
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
import org.junit.jupiter.api.Test

class AdminJsonApiTest : TestApi() {
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
    private val testAggregatedReports = listOf(
        dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport1"),
        dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport2")
    )

    private val testReports =
        listOf(
            dummyReportV4(orgUnit = testOrg, user = testUser, descriptiveName = "dummyReport1"),
            dummyReportV4(orgUnit = testOrg, descriptiveName = "dummyRepoort2"),
            dummyReportV4(orgUnit = testOrg, descriptiveName = "dummyreport3"),

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
    fun `returns all reports grouped by type`() = withTestApi {
        getWithAdminAccessChek("$adminRoute/reports").assert {
          val body = testApiObjectmapper.readTree(bodyAsText())
            body["reports"].toList().assert {
                size shouldBe 3
                map { it["title"].asText() } shouldContainAll  testReports.map { it.descriptiveName }
            }
            body["aggregatedReports"].toList().assert {
                size shouldBe 2
                map { it["title"].asText() } shouldContainAll  testAggregatedReports.map { it.descriptiveName }
            }

        }

    }

    @Test
    fun `creates a new aggregated report`() = withTestApi {
        getWithAdminAccessChek("$adminRoute/reports/aggregated/new")
        assertPostWithAdminIsOk("$adminRoute/reports/aggregated/new", HttpStatusCode.Created)
    }

    @Test
    fun `updates metadata of an aggregated report`() = withTestApi {
    //    assertPatchWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")
    }

    @Disabled("todo")
    @Test
    fun `updates metadata of a report`() = withTestApi {
    }

    @Test
    fun `updates a successcriterion in an aggregated report`() = withTestApi {
     //   assertPatchWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")
    }

    @Disabled("todo")
    @Test
    fun `updates a single successcriterion in a report`() = withTestApi {
       // assertPatchWithAdminIsOk("reports/${testReport.reportId}")
    }

    @Test
    fun `deletes an aggregated report`() = withTestApi {
      //  assertDeleteWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")

    }

    @Test
    fun `deletes a report`() = withTestApi {
       // assertDeleteWithAdminIsOk("reports/${testReport.reportId}")
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

    private suspend fun ApplicationTestBuilder.assertPostWithAdminIsOk(
        url: String,
        expectedAdminStatusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpResponse {
        client.postWithJwtUser(testUser, url).assert {
            withClue("user without admin check fails for $url") { status shouldBe HttpStatusCode.Forbidden }
        }
        client.post(url).assert {
            withClue("unauthenicated user check fails for $url") { status shouldBe HttpStatusCode.Unauthorized }
        }
        val adminResponse = client.postWithJwtUser(adminUser, url)
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
