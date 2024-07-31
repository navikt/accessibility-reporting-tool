package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.request.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.time.LocalDateTime
import java.util.*
import accessibility.reporting.tool.rest.ResourceNotFoundException
import accessibility.reporting.tool.wcag.SuccessCriterionInfo.Companion.perceivable
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-3 Mulig å tilpasse`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.devTools


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateReportTest {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val testOrg2 = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "Team-dolly",
        email = "teamdolly@test.com",
        members = mutableSetOf()
    )
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testUser =
        User(User.Email("testuser@nav.no"), "Test User", User.Oid(UUID.randomUUID().toString()), groups = listOf())
    private val dummyreport = dummyReportV2(orgUnit = testOrg)
    private val objectmapper = jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
    }

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
        repository.upsertReport(dummyreport)
    }

    @Test
    fun `partial updates metadata`() = setupTestApi(database) {
        val newDescriptiveName = "Updated Report Title"
        val updateDescriptiveName = """
        {
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "${newDescriptiveName}"
        }
    """.trimIndent()

        val descriptiveNamePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }
        descriptiveNamePatchRequest.status shouldBe HttpStatusCode.OK

        val descriptiveNameGetRequest = client.get("api/reports/${dummyreport.reportId}")
        descriptiveNameGetRequest.status shouldBe HttpStatusCode.OK
        val descriptiveNameUpdate = objectmapper.readTree(descriptiveNameGetRequest.bodyAsText())

        descriptiveNameUpdate["descriptiveName"].asText() shouldBe "Updated Report Title"

        descriptiveNameUpdate["team"]["name"].asText() shouldBe dummyreport.organizationUnit!!.name
        descriptiveNameUpdate["author"]["email"].asText() shouldBe dummyreport.author.email
        descriptiveNameUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
        /*updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged*/

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
            }
        }
    """.trimIndent()

        val teamUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateTeam)
            contentType(ContentType.Application.Json)
        }
        teamUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name, :email) 
                """.trimMargin(),
                mapOf(
                    "id" to newTeamId,
                    "name" to newTeamName,
                    "email" to newTeamEmail
                )
            )
        }

        val teamUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        teamUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val teamUpdate = objectmapper.readTree(teamUpdateGetRequest.bodyAsText())

        teamUpdate["team"]["name"].asText() shouldBe "Team-dolly"
        teamUpdate["team"]["email"].asText() shouldBe "teamdolly@test.com"

        teamUpdate["descriptiveName"].asText() shouldBe newDescriptiveName
        teamUpdate["author"]["email"].asText() shouldBe dummyreport.author.email
        teamUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
        /*updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged*/


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

        val authorUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateAuthor)
            contentType(ContentType.Application.Json)
        }
        authorUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        val authorUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        authorUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val authorUpdate = objectmapper.readTree(authorUpdateGetRequest.bodyAsText())

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

        val metadataUpdatePatchRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(metadataTest)
            contentType(ContentType.Application.Json)
        }
        metadataUpdatePatchRequest.status shouldBe HttpStatusCode.OK

        val metadataUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        metadataUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val metadataUpdate = objectmapper.readTree(metadataUpdateGetRequest.bodyAsText())

        metadataUpdate["descriptiveName"].asText() shouldBe "Updated Report Title"
        metadataUpdate["team"]["name"].asText() shouldBe "Team-dolly"
        metadataUpdate["team"]["email"].asText() shouldBe "teamdolly@test.com"
        metadataUpdate["author"]["email"].asText() shouldBe "author@test.com"
        metadataUpdate["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
    }

    @Test
    fun `partial updates singleCriterion`() = setupTestApi(database) {

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

        val singleCriterionUpdateRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(singleCriterionUpdate)
            contentType(ContentType.Application.Json)
        }
        singleCriterionUpdateRequest.status shouldBe HttpStatusCode.OK

        val singleCriterionGetRequest = client.get("api/reports/${dummyreport.reportId}")
        singleCriterionGetRequest.status shouldBe HttpStatusCode.OK
        val singleCriterionUpdateJsonResponse = objectmapper.readTree(singleCriterionGetRequest.bodyAsText())


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
    fun `partial updates multipleCriteria`() = setupTestApi(database) {

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

        val multipleCriteriaUpdateRequest = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updatedMultipleCriteria)
            contentType(ContentType.Application.Json)
        }
        multipleCriteriaUpdateRequest.status shouldBe HttpStatusCode.OK

        val multipleCriteriaUpdateGetRequest = client.get("api/reports/${dummyreport.reportId}")
        multipleCriteriaUpdateGetRequest.status shouldBe HttpStatusCode.OK
        val multipleCriteriaUpdate = multipleCriteriaUpdateGetRequest.bodyAsText()
        val multipleCriteriaJsonResponse = objectmapper.readTree(multipleCriteriaUpdate)

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
}