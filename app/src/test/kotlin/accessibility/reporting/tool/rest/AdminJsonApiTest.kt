package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class AdminJsonApiTest {
    private val adminRoute = "api/admin"
    private val database = LocalPostgresDatabase.cleanDb()
    private val reportRepository = ReportRepository(database)
    private val organizationRepository = OrganizationRepository(database)
    private val testOrg = OrganizationUnit(
        id = "1234567",
        name = "Testorganisation",
        email = "testorganisation@nav.no"
    )

    private val testUser = TestUser(name = "Samesame Tester").original
    private val adminUser = TestUser.createAdminUser(name = "Admin adminson").original
    val testReport = dummyReportV2(orgUnit = testOrg, user = testUser, descriptiveName = "dummyReport1")
    val testAggregatedReport = dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport2")

    private val testOrg2 = OrganizationUnit(
        id = "1234568",
        name = "Testorganization",
        email = "testorganization@nav.no"
    )
    private val initialReports =
        listOf(
            testReport,
            dummyReportV2(orgUnit = testOrg, descriptiveName = "dummyRepoort2"),
            dummyReportV2(orgUnit = testOrg, descriptiveName = "dummyreport3"),
            dummyAggregatedReportV2(user = adminUser, descriptiveName = "dummyAggregatedReport1"),
            testAggregatedReport,
        )


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
    fun `returns all reports grouped by type`() = testApi {
        assertGetWithAdminIsOk("$adminRoute/reports")
    }

    @Test
    fun `creates a new aggregated report`() = testApi {
        assertGetWithAdminIsOk("$adminRoute/reports/aggregated/new")
        assertPostWithAdminIsOk("$adminRoute/reports/aggregated/new")
    }

    @Test
    fun `updates metadata of an aggregated report`() = testApi {
        assertPatchWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")
    }

    @Disabled("todo")
    @Test
    fun `updates metadata of a report`() = testApi {
    }

    @Test
    fun `updates a successcriterion in an aggregated report`() = testApi {
        assertPatchWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")
    }

    @Disabled("todo")
    @Test
    fun `updates a single successcriterion in a report`() = testApi {
        assertPatchWithAdminIsOk("reports/${testReport.reportId}")
    }

    @Test
    fun `deletes an aggregated report`() = testApi {
        assertDeleteWithAdminIsOk("$adminRoute/reports/aggregated/${testAggregatedReport.reportId}")

    }

    @Test
    fun `deletes a report`() = testApi {
        assertDeleteWithAdminIsOk("reports/${testReport.reportId}")
    }


    private suspend fun ApplicationTestBuilder.assertGetWithAdminIsOk(
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
            withClue("unauthenicated user check fails for $url"){ status shouldBe HttpStatusCode.Unauthorized}
        }
        val adminResponse = client.patchWithJwtUser(adminUser, url)
        withClue("admin user check fails for $url"){ adminResponse.status shouldBe expectedAdminStatusCode}
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
            withClue("unauthenicated user check fails for $url"){ status shouldBe HttpStatusCode.Unauthorized}
        }
        val adminResponse = client.postWithJwtUser(adminUser, url)
        withClue("admin user check fails for $url"){ adminResponse.status shouldBe expectedAdminStatusCode}
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
            withClue("unauthenicated user check fails for $url"){ status shouldBe HttpStatusCode.Unauthorized}
        }
        val adminResponse = client.deleteWithJwtUser(adminUser, url)
        withClue("admin user check fails for $url"){ adminResponse.status shouldBe expectedAdminStatusCode}
        return adminResponse
    }

    private fun testApi(block: suspend ApplicationTestBuilder.() -> Unit) =
        setupTestApi(
            database = database,
            reportRepository = reportRepository,
            orgRepository = organizationRepository,
            block = block
        )
}
