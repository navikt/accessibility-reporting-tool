package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportContent
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.perceivable
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-3 Mulig å tilpasse`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.devTools
import accessibility.reporting.tool.wcag.levelA
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
    val testAggregatedReports = listOf(
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

    @Nested
    inner class GetAggregatedReport {
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
        fun `bad request when attempting to get report with the wrong report type`() = withTestApi {
            client.getWithJwtUser(testUser, "api/reports/${testAggregatedReports.first().reportId}")
                .status shouldBe HttpStatusCode.BadRequest
            client.getWithJwtUser(testUser, "api/reports/aggregated/${testReports.first().reportId}")
                .status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Nested
    inner class NewAggregatedReport {
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
        }

        @Test
        fun `bad request if one of the reports is an aggregated report`() = withTestApi {
            postWithAdminAssertion("$adminRoute/reports/aggregated/new", HttpStatusCode.BadRequest) {
                setBody(
                    """
                {
                   "title": "Some title",
                   "url":"Some url", 
                   "notes": "Here is a noooote",
                   "reports": ${(testReports + testAggregatedReports.first()).jsonList()}
                }
            """.trimIndent()
                )
            }.status shouldBe HttpStatusCode.BadRequest
        }

        @Test
        fun `bad request if reports doesn't exist`() = withTestApi {
            postWithAdminAssertion("$adminRoute/reports/aggregated/new", HttpStatusCode.BadRequest) {
                setBody(
                    """
                {
                   "title": "Some title",
                   "url":"Some url", 
                   "notes": "Here is a noooote",
                   "reports": ${(testReports + dummyReportV4()).jsonList()}
                }
            """.trimIndent()
                )
            }.status shouldBe HttpStatusCode.BadRequest

            postWithAdminAssertion("$adminRoute/reports/aggregated/new", HttpStatusCode.BadRequest) {
                setBody(
                    """
                {
                   "title": "Some title",
                   "url":"Some url", 
                   "notes": "Here is a noooote",
                   "reports": ${listOf(dummyReportV4(), dummyReportV4(), dummyReportV4()).jsonList()}
                }
            """.trimIndent()
                )
            }.status shouldBe HttpStatusCode.BadRequest

        }
    }


    @Nested
    inner class UpdateAggregatedReport {

        @Test
        fun `updates metadata`() = withTestApi {
            val originalReport = testAggregatedReports.first()
            val newDescriptiveName = "Updated Report Title"

            client.patchWithJwtUser(adminUser, "$adminRoute/reports/aggregated/${originalReport.reportId}") {
                setBodyWithJsonFields("descriptiveName" to newDescriptiveName)
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.OK

            client.getWithJwtUser(adminUser, "api/reports/aggregated/${originalReport.reportId}").assert {
                status shouldBe HttpStatusCode.OK
                val descriptiveNameUpdate = testApiObjectmapper.readTree(bodyAsText())
                descriptiveNameUpdate["descriptiveName"].asText() shouldBe "Updated Report Title"
                descriptiveNameUpdate["successCriteria"].toList().size shouldBe originalReport.successCriteria.size
                descriptiveNameUpdate["url"].asText() shouldBe originalReport.url
            }


            val newUrl = "https://test.new.no"
            val descriptiveNameSecondUpdate = "tadda"
            client.patchWithJwtUser(adminUser, "$adminRoute/reports/aggregated/${originalReport.reportId}") {
                setBodyWithJsonFields(
                    "url" to newUrl,
                    "descriptiveName" to descriptiveNameSecondUpdate
                )
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.OK

            client.getWithJwtUser(adminUser, "api/reports/aggregated/${originalReport.reportId}").assert {
                status shouldBe HttpStatusCode.OK
                val urlAndTitleUpdate = testApiObjectmapper.readTree(bodyAsText())
                urlAndTitleUpdate["author"]["email"].asText() shouldBe originalReport.author.email
                urlAndTitleUpdate["descriptiveName"].asText() shouldBe descriptiveNameSecondUpdate
                urlAndTitleUpdate["url"].asText() shouldBe newUrl
                urlAndTitleUpdate["successCriteria"].toList().size shouldBe originalReport.successCriteria.size
            }

        }

        @Test
        fun `updates forbidden for non-admin users`() = withTestApi {
            client.patchWithJwtUser(testUser, "$adminRoute/reports/aggregated/${testAggregatedReports.first().reportId}") {
                setBodyWithJsonFields("descriptiveName" to "newName")
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.Forbidden
        }

        @Disabled
        @Test
        fun `partial updates singleCriterion`() = withTestApi {
            val dummyreport = testAggregatedReports.first()

            val originalCriteria = 1.perceivable("1.3.1", "Informasjon og relasjoner") {
                description = "Ting skal være kodet som det ser ut som."
                guideline = `1-3 Mulig å tilpasse`
                tools = "$devTools/headingsMap"
                wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships"
            }.levelA()

            val singleCriterionUpdate = """
        {
            "reportId": "${dummyreport.reportId}",
            
            "successCriteria": [{
                "name": "single updated criterion",
                "description": "single updated description",
                "principle": "single updated principle",
                "guideline": "single updated guideline",
                "tools": "single updated tools",
                "number": "1.3.1",
                "breakingTheLaw": "nei",
                "lawDoesNotApply": "nei",
                "tooHardToComply": "nei",
                "contentGroup": "Group 1",
                "status": "COMPLIANT",
                "wcagLevel": "A"
            }]
        }
    """.trimIndent()

            val singleCriterionUpdateRequest =
                client.patchWithJwtUser(testUser, "$adminRoute/reports/aggregated/${dummyreport.reportId}") {
                    setBody(singleCriterionUpdate)
                    contentType(ContentType.Application.Json)
                }
            singleCriterionUpdateRequest.status shouldBe HttpStatusCode.OK

            val singleCriterionGetRequest = client.get("$adminRoute/reports/aggregated/${dummyreport.reportId}")
            singleCriterionGetRequest.status shouldBe HttpStatusCode.OK
            val singleCriterionUpdateJsonResponse = testApiObjectmapper.readTree(singleCriterionGetRequest.bodyAsText())


            val criteriaList = singleCriterionUpdateJsonResponse["successCriteria"].toList()
            criteriaList.size shouldBe 49

            val specificCriterion = criteriaList.find { it["number"].asText() == "1.3.1" }
            specificCriterion?.let {
                it["name"].asText() shouldBe originalCriteria.name
                it["description"].asText() shouldBe originalCriteria.description
                it["principle"].asText() shouldBe originalCriteria.principle
                it["guideline"].asText() shouldBe originalCriteria.guideline
                it["tools"].asText() shouldBe originalCriteria.tools
                it["number"].asText() shouldBe originalCriteria.number
                it["breakingTheLaw"].asText() shouldBe "nei"
                it["lawDoesNotApply"].asText() shouldBe "nei"
                it["tooHardToComply"].asText() shouldBe "nei"
                it["contentGroup"].asText() shouldBe "Group 1"
                it["status"].asText() shouldBe "COMPLIANT"
                it["wcagLevel"].asText() shouldBe originalCriteria.wcagLevel.name
            } ?: throw ResourceNotFoundException("Criterion", "1.3.1")

            val otherCriteria = criteriaList.filter { it["number"].asText() != "1.3.1" }
            otherCriteria.isNotEmpty() shouldBe true
        }

        @Test
        fun `updates notes`() = withTestApi {
            val dummyreport = testAggregatedReports.first()

            val newNotes = "Here are some notes that are noted"

            client.patchWithJwtUser(adminUser, "$adminRoute/reports/aggregated/${dummyreport.reportId}") {
                setBodyWithJsonFields(
                    "notes" to newNotes,
                )
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.OK

            val updatedReportRequest = client.getWithJwtUser(adminUser,"api/reports/aggregated/${dummyreport.reportId}")
            updatedReportRequest.status shouldBe HttpStatusCode.OK
            val updatedReport =
                testApiObjectmapper.readTree(updatedReportRequest.bodyAsText())
            updatedReport["notes"].asText() shouldBe newNotes
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

private fun HttpRequestBuilder.setBodyWithJsonFields(vararg fields: Pair<String, String>) {
    setBody(
        fields.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ","
        ) { """ "${it.first}" : "${it.second}" """.trimIndent() })
}

private fun List<Pair<String, String>>.toJsonObject() = joinToString(
    prefix = "{",
    postfix = "}",
    separator = ","
) { """ "${it.first}" : "${it.second}" """.trimIndent() }


private fun List<ReportContent>.jsonList() =
    joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${it.reportId}\"" }
