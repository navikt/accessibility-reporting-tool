package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportContent
import accessibility.reporting.tool.wcag.Status
import assert
import com.fasterxml.jackson.databind.JsonNode
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
                   "descriptiveName": "Some title",
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
                   "descriptiveName": "Some title",
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
                   "descriptiveName": "Some title",
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
                   "descriptiveName": "Some title",
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
            client.patchWithJwtUser(
                testUser,
                "$adminRoute/reports/aggregated/${testAggregatedReports.first().reportId}"
            ) {
                setBodyWithJsonFields("descriptiveName" to "newName")
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.Forbidden
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

            val updatedReportRequest =
                client.getWithJwtUser(adminUser, "api/reports/aggregated/${dummyreport.reportId}")
            updatedReportRequest.status shouldBe HttpStatusCode.OK
            val updatedReport =
                testApiObjectmapper.readTree(updatedReportRequest.bodyAsText())
            updatedReport["notes"].asText() shouldBe newNotes
        }

        @ParameterizedTest
        @ValueSource(strings = ["COMPLIANT", "NON_COMPLIANT", "NOT_APPLICABLE", "NOT_TESTED"])
        fun `updates a single success criterion in an aggregated report`(expectedStatusStr: String) = withTestApi {
            val expectedStatus = Status.valueOf(expectedStatusStr)
            val testReport = testAggregatedReports.last()
            val updateCriteriaBody = """
                {
                   "successCriteria": [
                    ${
                updatedCriterionJsonBody(
                    number = "1.3.1",
                    status = expectedStatus
                )
            }
                   ]
                }
            """.trimIndent()

            client.patchWithJwtUser(adminUser, "$adminRoute/reports/aggregated/${testReport.reportId}") {
                setBody(updateCriteriaBody)
                contentType(ContentType.Application.Json)
            }.status shouldBe HttpStatusCode.OK

            client.getWithJwtUser(adminUser, "api/reports/aggregated/${testReport.reportId}").assert {
                status shouldBe HttpStatusCode.OK
                json()["successCriteria"].toList().assert {
                    size shouldBe testReport.successCriteria.size
                    assertCriterion(
                        criterionNode = find { it["number"].asText() == "1.3.1" },
                        expectedStatus = expectedStatus
                    )
                    filterNot { it["number"].asText() == "1.3.1" }.forEach {
                        assertCriterion(
                            criterionNode = it,
                            expectedStatus = Status.NOT_TESTED
                        )
                    }
                }
            }

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

private fun updatedCriterionJsonBody(
    number: String,
    status: Status,
    breakingTheLaw: String? = "",
    lawDoesNotApply: String? = "",
    tooHardToComply: String? = "",
) =
    """ 
    {
       "number": "$number",
       "breakingTheLaw": "$breakingTheLaw",
       "lawDoesNotApply": "$lawDoesNotApply",
       "tooHardToComply": "$tooHardToComply",
       "status": "${status.name}"
    
    }
    """.trimIndent()


private fun assertCriterion(
    criterionNode: JsonNode?,
    expectedStatus: Status,
    expectedBreakingTheLaw: String? = "",
    expectedLawDoesNotApply: String? = "",
    expectedTooHardToComply: String? = "",
) {
    require(criterionNode != null) { "Criterionnode is null" }
    criterionNode["status"].asNonEmptyText().trimIndent() shouldBe expectedStatus.name
    criterionNode["breakingTheLaw"].asNonEmptyText() shouldBe expectedBreakingTheLaw
    criterionNode["lawDoesNotApply"].asNonEmptyText() shouldBe expectedLawDoesNotApply
    criterionNode["tooHardToComply"].asNonEmptyText() shouldBe expectedTooHardToComply
}

private fun JsonNode.asNonEmptyText() = asText().replace("\n", "").trimIndent()