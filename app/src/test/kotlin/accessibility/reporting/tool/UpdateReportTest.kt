package accessibility.reporting.tool

import accessibility.reporting.tool.wcag.*
import io.ktor.client.request.*
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import accessibility.reporting.tool.rest.ResourceNotFoundException
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.perceivable
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-3 Mulig å tilpasse`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.devTools


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateReportTest : TestApi() {
    private val testOrg = createTestOrg(
        name = "DummyOrg",
        email = "test@nav.no",
    )
    private val testOrg2 = createTestOrg(
        name = "Team-dolly",
        email = "teamdolly@test.com",
    )
    private val testUser = TestUser(email = "testuser@nav.no", name = "Test User")
    private val dummyreport = dummyReportV4(orgUnit = testOrg)

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
        reportRepository.upsertReport(dummyreport)
    }

    @Test
    fun `partial updates metadata`() = withTestApi {
        val newDescriptiveName = "Updated Report Title"
        val updateDescriptiveName = """
        {
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "${newDescriptiveName}"
        }
    """.trimIndent()

        val descriptiveNamePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }
        descriptiveNamePatchRequest.status shouldBe HttpStatusCode.OK

        val descriptiveNameGetRequest = client.get("api/reports/${dummyreport.reportId}")
        descriptiveNameGetRequest.status shouldBe HttpStatusCode.OK
        val descriptiveNameUpdate = testApiObjectmapper.readTree(descriptiveNameGetRequest.bodyAsText())

        descriptiveNameUpdate["descriptiveName"].asText() shouldBe "Updated Report Title"

        descriptiveNameUpdate["team"]["name"].asText() shouldBe dummyreport.organizationUnit!!.name
        descriptiveNameUpdate["author"]["email"].asText() shouldBe dummyreport.author.email
        descriptiveNameUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size

        val newTeamId = testOrg2.id
        val newTeamName = testOrg2.name
        val newTeamEmail = testOrg2.email
        val updateTeam = """
        {
            "reportId": "${dummyreport.reportId}",
            "team": {
                "id": "${newTeamId}",
                "name": "${newTeamName}",
                "email": "${newTeamEmail}"
            },
            "isPartOfNavNo":"true"
        }
    """.trimIndent()

        val teamUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(updateTeam)
            contentType(ContentType.Application.Json)
        }
        teamUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        val teamUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        teamUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val teamUpdate = testApiObjectmapper.readTree(teamUpdateGetRequest.bodyAsText())

        teamUpdate["team"]["name"].asText() shouldBe "Team-dolly"
        teamUpdate["team"]["email"].asText() shouldBe "teamdolly@test.com"

        teamUpdate["descriptiveName"].asText() shouldBe newDescriptiveName
        teamUpdate["author"]["email"].asText() shouldBe dummyreport.author.email
        teamUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
        teamUpdate["isPartOfNavNo"].asBoolean() shouldBe true


        val newAuthorEmail = "author@test.com"
        val updateAuthor = """
        {
            "reportId": "${dummyreport.reportId}",
            "author": {
                "email": "${newAuthorEmail}",
                "oid": "123"
            }
        }
    """.trimIndent()

        val authorUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(updateAuthor)
            contentType(ContentType.Application.Json)
        }
        authorUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        val authorUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        authorUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val authorUpdate = testApiObjectmapper.readTree(authorUpdateGetRequest.bodyAsText())

        authorUpdate["author"]["email"].asText() shouldBe "author@test.com"

        authorUpdate["descriptiveName"].asText() shouldBe newDescriptiveName
        authorUpdate["team"]["name"].asText() shouldBe newTeamName
        authorUpdate["team"]["email"].asText() shouldBe newTeamEmail
        authorUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
        /*updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged*/

        val metadataTest = """{
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "${newDescriptiveName}",
            "team": {
                "id": "${dummyreport.organizationUnit!!.id}",
                "name": "${newTeamName}",
                "email": "${newTeamEmail}"
            },
            "author": {
                "email": "${newAuthorEmail}",
                "oid": "123"
            },
            "created": "${LocalDateTime.now()}",
            "lastChanged": "${LocalDateTime.now()}"
                }""".trimMargin()

        val metadataUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(metadataTest)
            contentType(ContentType.Application.Json)
        }
        metadataUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        val metadataUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        metadataUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val metadataUpdate = testApiObjectmapper.readTree(metadataUpdateGetRequest.bodyAsText())

        metadataUpdate["descriptiveName"].asText() shouldBe "Updated Report Title"
        metadataUpdate["team"]["name"].asText() shouldBe "Team-dolly"
        metadataUpdate["team"]["email"].asText() shouldBe "teamdolly@test.com"
        metadataUpdate["author"]["email"].asText() shouldBe "author@test.com"
        metadataUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
    }

    @Test
    fun `partial updates singleCriterion`() = withTestApi {

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

        val singleCriterionUpdateRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(singleCriterionUpdate)
            contentType(ContentType.Application.Json)
        }
        singleCriterionUpdateRequest.status shouldBe HttpStatusCode.OK

        val singleCriterionGetRequest = client.get("api/reports/${dummyreport.reportId}")
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
    fun `partial updates multipleCriteria`() = withTestApi {

        val originalCriterion1 = 1.perceivable("1.3.1", "Informasjon og relasjoner") {
            description = "Ting skal være kodet som det ser ut som."
            guideline = `1-3 Mulig å tilpasse`
            tools = "$devTools/headingsMap"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships"
        }.levelA()

        val originalCriterion2 = 1.perceivable("1.3.2", "Meningsbærende rekkefølge") {
            description = "Presenter innhold i en meningsfull rekkefølge."
            guideline = `1-3 Mulig å tilpasse`
            tools = "disableHTML"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/meaningful-sequence"
        }.levelA()

        val updatedMultipleCriteria = """ 
        {
            "reportId": "${dummyreport.reportId}",
            "successCriteria": [{
                "name": "multiple updated criteria",
                "description": "multiple updated description",
                "principle": "multiple updated principle",
                "guideline": "multiple updated guideline",
                "tools": "multiple updated tools",
                "number": "1.3.1",
                "breakingTheLaw": "ja",
                "lawDoesNotApply": "nei",
                "tooHardToComply": "nei",
                "contentGroup": "Group 1",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            },{
            "name": "flere updated criteria",
                "description": "flere updated description",
                "principle": "flere updated principle",
                "guideline": "flere updated guideline",
                "tools": "flere updated tools",
                "number": "1.3.2",
                "breakingTheLaw": "nei",
                "lawDoesNotApply": "nei",
                "tooHardToComply": "ja",
                "contentGroup": "Group 1",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            }]
        }
    """.trimIndent()

        val multipleCriteriaUpdateRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(updatedMultipleCriteria)
            contentType(ContentType.Application.Json)
        }
        multipleCriteriaUpdateRequest.status shouldBe HttpStatusCode.OK

        val multipleCriteriaUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        multipleCriteriaUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val multipleCriteriaUpdate = multipleCriteriaUpdateGetRequest.bodyAsText()
        val multipleCriteriaJsonResponse = testApiObjectmapper.readTree(multipleCriteriaUpdate)

        val criteriaList = multipleCriteriaJsonResponse["successCriteria"].toList()
        criteriaList.size shouldBe 49
        val updatedCriterion1 = criteriaList.find { it["number"].asText() == "1.3.1" }!!
        val updatedCriterion2 = criteriaList.find { it["number"].asText() == "1.3.2" }!!

        updatedCriterion1["name"].asText() shouldBe originalCriterion1.name
        updatedCriterion1["description"].asText() shouldBe originalCriterion1.description
        updatedCriterion1["principle"].asText() shouldBe originalCriterion1.principle
        updatedCriterion1["guideline"].asText() shouldBe originalCriterion1.guideline
        updatedCriterion1["tools"].asText() shouldBe originalCriterion1.tools
        updatedCriterion1["number"].asText() shouldBe originalCriterion1.number
        updatedCriterion1["breakingTheLaw"].asText() shouldBe "ja"
        updatedCriterion1["lawDoesNotApply"].asText() shouldBe "nei"
        updatedCriterion1["tooHardToComply"].asText() shouldBe "nei"
        updatedCriterion1["contentGroup"].asText() shouldBe "Group 1"
        updatedCriterion1["status"].asText() shouldBe "COMPLIANT"
        updatedCriterion1["wcagLevel"].asText() shouldBe originalCriterion1.wcagLevel.name

        updatedCriterion2["name"].asText() shouldBe originalCriterion2.name
        updatedCriterion2["description"].asText() shouldBe originalCriterion2.description
        updatedCriterion2["principle"].asText() shouldBe originalCriterion2.principle
        updatedCriterion2["guideline"].asText() shouldBe originalCriterion2.guideline
        updatedCriterion2["tools"].asText() shouldBe originalCriterion2.tools
        updatedCriterion2["number"].asText() shouldBe originalCriterion2.number
        updatedCriterion2["breakingTheLaw"].asText() shouldBe "nei"
        updatedCriterion2["lawDoesNotApply"].asText() shouldBe "nei"
        updatedCriterion2["tooHardToComply"].asText() shouldBe "ja"
        updatedCriterion2["contentGroup"].asText() shouldBe "Group 1"
        updatedCriterion2["status"].asText() shouldBe "COMPLIANT"
        updatedCriterion2["wcagLevel"].asText() shouldBe originalCriterion2.wcagLevel.name

        val otherCriteria = criteriaList.filter {
            it["number"].asText() != "1.3.1"
                    && it["number"].asText() != "1.3.2"
        }
        otherCriteria.isNotEmpty() shouldBe true
    }

    @Test
    fun `updates notes`() = withTestApi {
        val newNotes = "Here are some notes that are noted"

        client.get("api/reports/${dummyreport.reportId}").let {
            testApiObjectmapper.readTree(it.bodyAsText())
        }["notes"].asText() shouldBe ""

        client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}") {
            setBody(
                """{"notes": "$newNotes" }""".trimIndent()
            )
            contentType(ContentType.Application.Json)
        }.status shouldBe HttpStatusCode.OK

        val updatedReportRequest = client.get("api/reports/${dummyreport.reportId}")
        updatedReportRequest.status shouldBe HttpStatusCode.OK
        val updatedReport =
            testApiObjectmapper.readTree(updatedReportRequest.bodyAsText())
        updatedReport["notes"].asText() shouldBe newNotes
    }
}