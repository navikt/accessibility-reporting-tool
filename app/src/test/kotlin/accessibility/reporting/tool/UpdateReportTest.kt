package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.rest.FullReport
import accessibility.reporting.tool.wcag.*
import assert
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
import accessibility.reporting.tool.wcag.SucessCriteriaV1
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Guidelines.`1-3 Mulig å tilpasse`
import accessibility.reporting.tool.wcag.SucessCriteriaV1.Tools.devTools
import kotlinx.css.times


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateReportTest {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
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
    fun `update success criteria`() = setupTestApi(database) {
        val newCriteria =
            SuccessCriterion(
                name = "updated criterion",
                description = "updated description",
                principle = "updated principle",
                guideline = "updated guideline",
                tools = "updated tools",
                number = "updated number",
                breakingTheLaw = "updated breakingTheLaw",
                lawDoesNotApply = "updated lawDoesNotApply",
                tooHardToComply = "updated tooHardToComply",
                contentGroup = "updated contentGroup",
                status = Status.COMPLIANT
            ).apply {
                wcagLevel = WcagLevel.AA
            }


        val updatedReport = dummyreport.updateCriteria(listOf(newCriteria), testUser)
            .let {
                FullReport(
                    it.reportId, it.descriptiveName, it.url,
                    team = it.organizationUnit,
                    author = it.author,
                    successCriteria = it.successCriteria,
                    created = it.created,
                    lastChanged = it.lastChanged
                )
            }


        val response = client.putWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(
                objectmapper.writeValueAsString(updatedReport)
            )
            contentType(
                ContentType.Application.Json
            )
        }

        response.status shouldBe HttpStatusCode.OK
        val responseBody = response.bodyAsText()
        val jsonResponse = objectmapper.readTree(responseBody)
        jsonResponse["reportId"].asText() shouldBe dummyreport.reportId

        val updatedResponse = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse.status shouldBe HttpStatusCode.OK
        val updatedResponseBody = updatedResponse.bodyAsText()
        val updatedJsonResponse = objectmapper.readTree(updatedResponseBody)


        dummyreport.assertExists(updatedJsonResponse)
        updatedJsonResponse["successCriteria"].toList().assert {
            size shouldBe 1
            val criterion = this.first()
            criterion["name"].asText() shouldBe "updated criterion"
            criterion["description"].asText() shouldBe "updated description"
            criterion["principle"].asText() shouldBe "updated principle"
            criterion["guideline"].asText() shouldBe "updated guideline"
            criterion["tools"].asText() shouldBe "updated tools"
            criterion["number"].asText() shouldBe "updated number"
            criterion["breakingTheLaw"].asText() shouldBe "updated breakingTheLaw"
            criterion["lawDoesNotApply"].asText() shouldBe "updated lawDoesNotApply"
            criterion["tooHardToComply"].asText() shouldBe "updated tooHardToComply"
            criterion["contentGroup"].asText() shouldBe "updated contentGroup"
            criterion["status"].asText() shouldBe "COMPLIANT"
            criterion["wcagLevel"].asText() shouldBe "AA"

        }
    }

    //Lag en test hvor bare ett metadatafelt blir send med (f.eks team eller descriptive name)
    @Test
    fun `partial updates metadata`() = setupTestApi(database) {

        val updateDescriptiveName = """
        {
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "Updated Report Title"
        }
    """.trimIndent()

        val response5 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }
        response5.status shouldBe HttpStatusCode.OK
        //trenger ikke de 3 neste linjene (det blir testet på linje 156-161)
        /*val responseBody5 = response5.bodyAsText()
        val jsonResponse5 = objectmapper.readTree(responseBody5)
        jsonResponse5["reportId"].asText() shouldBe dummyreport.reportId*/

        val updatedResponse5 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse5.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse5 = objectmapper.readTree(updatedResponse5.bodyAsText())

        updatedJsonResponse5["descriptiveName"].asText() shouldBe "Updated Report Title"
        // test at andre metadatafelt ikke er endret
        updatedJsonResponse5["team"].asText() shouldBe dummyreport.organizationUnit
        updatedJsonResponse5["author"].asText() shouldBe dummyreport.author
        updatedJsonResponse5["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged

        val updateTeam = """
        {
            "reportId": "${dummyreport.reportId}",
            "team": {
                "id": "test-id",
                "name": "Team-dolly",
                "email": "teamdolly@test.com"
            }
        }
    """.trimIndent()

        val response6 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(updateTeam)
            contentType(ContentType.Application.Json)
        }
        response6.status shouldBe HttpStatusCode.OK

        val updatedResponse6 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse6.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse6 = objectmapper.readTree(updatedResponse6.bodyAsText())

        updatedJsonResponse6["team"]["name"].asText() shouldBe "Team-dolly"
        updatedJsonResponse6["team"]["email"].asText() shouldBe "teamdolly@test.com"

        updatedJsonResponse5["descriptiveName"].asText() shouldBe dummyreport.descriptiveName
        updatedJsonResponse5["author"].asText() shouldBe dummyreport.author
        updatedJsonResponse5["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged

        val updateAuthor = """
        {
            "reportId": "${dummyreport.reportId}",
            "author": {
                "email": "author@test.com",
                "oid": "123"
            }
        }
    """.trimIndent()

        val response7 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(updateAuthor)
            contentType(ContentType.Application.Json)
        }
        response7.status shouldBe HttpStatusCode.OK

        val updatedResponse7 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse7.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse7 = objectmapper.readTree(updatedResponse7.bodyAsText())

        updatedJsonResponse7["author"]["email"].asText() shouldBe "author@test.com"

        updatedJsonResponse5["descriptiveName"].asText() shouldBe dummyreport.descriptiveName
        updatedJsonResponse5["team"].asText() shouldBe dummyreport.organizationUnit
        updatedJsonResponse5["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse5["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse5["lastChanged"].asText() shouldBe dummyreport.lastChanged

        val metadataTest = """{
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "Updated Report Title",
            "team": {
                "id": "test-report",
                "name": "Team-UU",
                "email": "team@test.com"
            },
            "author": {
                "email": "updateduser@test.com",
                "oid": "123"
            },
            "created": "${LocalDateTime.now()}",
            "lastChanged": "${LocalDateTime.now()}"
                }""".trimMargin()

        val response = client.putWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(metadataTest)
            contentType(ContentType.Application.Json)
        }
        response.status shouldBe HttpStatusCode.OK

        val updatedResponse = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse = objectmapper.readTree(updatedResponse.bodyAsText())

        updatedJsonResponse["descriptiveName"].asText() shouldBe "Updated Report Title"
        updatedJsonResponse["team"]["name"].asText() shouldBe "Team-UU"
        updatedJsonResponse["team"]["email"].asText() shouldBe "team@test.com"
        updatedJsonResponse["author"]["email"].asText() shouldBe "updateduser@test.com"
    }

    @Test
    fun `partial updates singleCriterion`() = setupTestApi(database) {

        val originalCriteria = 1.perceivable("1.3.1", "Informasjon og relasjoner") {
            description = "Ting skal være kodet som det ser ut som."
            guideline = `1-3 Mulig å tilpasse`
            tools = "$devTools/headingsMap"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships"
        }.levelA()

        /*val singleCriterionUpdate = listOf(
            SuccessCriterion(
                name = "Informasjon og relasjoner",
                description = "Ting skal være kodet som det ser ut som.",
                principle = "Principle 1",
                guideline = "1-3 Mulig å tilpasse",
                tools = "DevTools/headingsMap",
                number = "1.3.1",
                breakingTheLaw = "nei",
                lawDoesNotApply = "nei",
                tooHardToComply = "nei",
                contentGroup = "Group 1",
                status = Status.NOT_TESTED,
                wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships",
                helpUrl = "https://www.helpurl.com",
                wcagVersion = "2.1"
            )
        )*/

        //Bruk en av criteriene som er definert i successcriteria v1
        //Sjekk med Nima om det kommer som enkeltobjekt eller i en liste=universal function , enkeltobjekt- metadata; success criteria- liste; bare den som endrer seg
        val singleCriterionUpdate = """
        {
            "reportId": "${dummyreport.reportId}",
            
            "successCriteria": [{
                "name": "single updated criterion",
                "description": "single updated description",
                "principle": "single updated principle",
                "guideline": "single updated guideline",
                "tools": "single updated tools",
                "number": "single updated number",
                "breakingTheLaw": "single updated breakingTheLaw",
                "lawDoesNotApply": "single updated lawDoesNotApply",
                "tooHardToComply": "single updated tooHardToComply",
                "contentGroup": "single updated contentGroup",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            }]
        }
    """.trimIndent()
       //patch istedet for put
        val response2 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(singleCriterionUpdate)
            contentType(ContentType.Application.Json)
        }
        response2.status shouldBe HttpStatusCode.OK

        val updatedResponse2 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse2.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse2 = objectmapper.readTree(updatedResponse2.bodyAsText())


        val criteriaList = updatedJsonResponse2["successCriteria"].toList()
        criteriaList.size shouldBe 49

        /*updatedJsonResponse2["successCriteria"].toList().assert {
            size shouldBe 1
            //sjekk at kriterielista sin size fortsatt er 49
            val criterion = this.first() //find (se på sammen)
            criterion["name"].asText() shouldBe "Informasjon og relasjoner" //sjekk at det ikke har endret seg
            criterion["description"].asText() shouldBe "Ting skal være kodet som det ser ut som."
            criterion["principle"].asText() shouldBe "Principle 1"
            criterion["guideline"].asText() shouldBe "1-3 Mulig å tilpasse"
            criterion["tools"].asText() shouldBe "$devTools/headingsMap"
            criterion["number"].asText() shouldBe "1.3.1"
            criterion["breakingTheLaw"].asText() shouldBe "nei"
            criterion["lawDoesNotApply"].asText() shouldBe "nei"
            criterion["tooHardToComply"].asText() shouldBe "nei"
            criterion["contentGroup"].asText() shouldBe "Group 1"
            criterion["status"].asText() shouldBe "COMPLIANT"
            criterion["wcagLevel"].asText() shouldBe "AA"

            //Sjekk at andre kriterier ikke har endret seg
        }*/

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
            it["wcagLevel"].asText() shouldBe originalCriteria.wcagLevel
        } ?: throw ResourceNotFoundException("Criterion", "1.3.1")

        val otherCriteria = criteriaList.filter { it["number"].asText() != "1.3.1" }
        otherCriteria.isNotEmpty() shouldBe true


    }

    @Test
    fun `partial updates multipleCriteria`() = setupTestApi(database) {
        //liste

        val originalCriteria = 1.perceivable("1.3.1", "Informasjon og relasjoner") {
            description = "Ting skal være kodet som det ser ut som."
            guideline = `1-3 Mulig å tilpasse`
            tools = "$devTools/headingsMap"
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/info-and-relationships"
        }.levelA()

        val multipleCriteriaUpdate = """ 
        {
            "reportId": "${dummyreport.reportId}",
            "successCriteria": [{
                "name": "multiple updated criteria",
                "description": "multiple updated description",
                "principle": "multiple updated principle",
                "guideline": "multiple updated guideline",
                "tools": "multiple updated tools",
                "number": "multiple updated number",
                "breakingTheLaw": "multiple updated breakingTheLaw",
                "lawDoesNotApply": "multiple updated lawDoesNotApply",
                "tooHardToComply": "multiple updated tooHardToComply",
                "contentGroup": "multiple updated contentGroup",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            },{
            "name": "flere updated criteria",
                "description": "flere updated description",
                "principle": "flere updated principle",
                "guideline": "flere updated guideline",
                "tools": "flere updated tools",
                "number": "flere updated number",
                "breakingTheLaw": "flere updated breakingTheLaw",
                "lawDoesNotApply": "flere updated lawDoesNotApply",
                "tooHardToComply": "flere updated tooHardToComply",
                "contentGroup": "flere updated contentGroup",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            }]
        }
    """.trimIndent()
        //patch
        val response3 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(multipleCriteriaUpdate)
            contentType(ContentType.Application.Json)
        }
        response3.status shouldBe HttpStatusCode.OK

        val updatedResponse3 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse3.status shouldBe HttpStatusCode.OK
        val updatedResponseBody3 = updatedResponse3.bodyAsText()
        val updatedJsonResponse3 = objectmapper.readTree(updatedResponseBody3)

        updatedJsonResponse3["successCriteria"].toList().assert {
            size shouldBe 2
            val firstCriterion = this[0]
            firstCriterion["name"].asText() shouldBe "multiple updated criteria"
            firstCriterion["description"].asText() shouldBe "multiple updated description"
            firstCriterion["principle"].asText() shouldBe "multiple updated principle"
            firstCriterion["guideline"].asText() shouldBe "multiple updated guideline"
            firstCriterion["tools"].asText() shouldBe "multiple updated tools"
            firstCriterion["number"].asText() shouldBe "multiple updated number"
            firstCriterion["breakingTheLaw"].asText() shouldBe "multiple updated breakingTheLaw"
            firstCriterion["lawDoesNotApply"].asText() shouldBe "multiple updated lawDoesNotApply"
            firstCriterion["tooHardToComply"].asText() shouldBe "multiple updated tooHardToComply"
            firstCriterion["contentGroup"].asText() shouldBe "multiple updated contentGroup"
            firstCriterion["status"].asText() shouldBe "COMPLIANT"
            firstCriterion["wcagLevel"].asText() shouldBe "AA"
            val secondCriterion = this[1]
            secondCriterion["name"].asText() shouldBe "flere updated criteria"
            secondCriterion["description"].asText() shouldBe "flere updated description"
            secondCriterion["principle"].asText() shouldBe "flere updated principle"
            secondCriterion["guideline"].asText() shouldBe "flere updated guideline"
            secondCriterion["tools"].asText() shouldBe "flere updated tools"
            secondCriterion["number"].asText() shouldBe "flere updated number"
            secondCriterion["breakingTheLaw"].asText() shouldBe "flere updated breakingTheLaw"
            secondCriterion["lawDoesNotApply"].asText() shouldBe "flere updated lawDoesNotApply"
            secondCriterion["tooHardToComply"].asText() shouldBe "flere updated tooHardToComply"
            secondCriterion["contentGroup"].asText() shouldBe "flere updated contentGroup"
            secondCriterion["status"].asText() shouldBe "COMPLIANT"
            secondCriterion["wcagLevel"].asText() shouldBe "AA"
        }
    }

    @Test
    fun `full update`() = setupTestApi(database) {

        val fullUpdate = """{
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "fullUpdated Report Title",
            "team": {
                "id": "test-report2",
                "name": "Team-Universell Utforming",
                "email": "team@testUU.com"
            },
            "author": {
                "email": "fullupdateduser@test.com",
                "oid": "123"
            },
            "created": "${LocalDateTime.now()}",
            "lastChanged": "${LocalDateTime.now()}"
      
            "successCriteria" : [ {
            "name": "full updated criteria",
            "description": "full updated description",
            "principle": "full updated principle",
            "guideline": "full updated guideline",
            "tools": "full updated tools",
            "number": "full updated number",
            "breakingTheLaw": "full updated breakingTheLaw",
            "lawDoesNotApply": "full updated lawDoesNotApply",
            "tooHardToComply": "full updated tooHardToComply",
            "contentGroup": "full updated contentGroup",
            "status": "COMPLIANT",
            "wcagLevel": "AA"
        }]
        
        }""".trimIndent()

        val response4 = client.putWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(fullUpdate)
            contentType(ContentType.Application.Json)
        }
        response4.status shouldBe HttpStatusCode.OK
        val response4Body = response4.bodyAsText()
        val jsonResponse4 = objectmapper.readTree(response4Body)
        jsonResponse4["reportId"].asText() shouldBe dummyreport.reportId
        val updatedResponse4 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse4.status shouldBe HttpStatusCode.OK
        val updatedResponseBody4 = updatedResponse4.bodyAsText()
        val updatedJsonResponse4 = objectmapper.readTree(updatedResponseBody4)

        updatedJsonResponse4["descriptiveName"].asText() shouldBe "fullUpdated Report Title"
        updatedJsonResponse4["team"]["name"].asText() shouldBe "Team-Universell Utforming"
        updatedJsonResponse4["team"]["email"].asText() shouldBe "team@testUU.com"
        updatedJsonResponse4["author"]["email"].asText() shouldBe "fullupdateduser@test.com"
        updatedJsonResponse4["successCriteria"].toList().assert {
            size shouldBe 1
            val criterion = this.first()
            criterion["name"].asText() shouldBe "full updated criteria"
            criterion["description"].asText() shouldBe "full updated description"
            criterion["principle"].asText() shouldBe "full updated principle"
            criterion["guideline"].asText() shouldBe "full updated guideline"
            criterion["tools"].asText() shouldBe "full updated tools"
            criterion["number"].asText() shouldBe "full updated number"
            criterion["breakingTheLaw"].asText() shouldBe "full updated breakingTheLaw"
            criterion["lawDoesNotApply"].asText() shouldBe "full updated lawDoesNotApply"
            criterion["tooHardToComply"].asText() shouldBe "full updated tooHardToComply"
            criterion["contentGroup"].asText() shouldBe "full updated contentGroup"
            criterion["status"].asText() shouldBe "COMPLIANT"
            criterion["wcagLevel"].asText() shouldBe "AA"
        }
    }
}

//Tester for:
// oppdatere kun metada
// oppdatere kun 1 criterion (i liste)
// oppdatere kun flere criteria (ikke metadata)
// oppdatere både metadata og criteria?
//NB! bruke jsonstrings, ikke objectmapper i request


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

private fun metadata(
    descriptiveName: String,
    team: OrganizationUnit,
    author: Author,
    created: LocalDateTime,
    lastChanged: LocalDateTime
) = FullReport(
    reportId = "test-report",
    url = "test@test.com",
    descriptiveName = descriptiveName,
    team = team,
    author = author,
    successCriteria = emptyList(),
    created = created,
    lastChanged = lastChanged
)
