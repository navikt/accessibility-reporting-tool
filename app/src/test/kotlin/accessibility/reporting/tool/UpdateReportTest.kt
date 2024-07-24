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
val newDescriptiveName="Updated Report Title"
        val updateDescriptiveName = """
        {
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "${newDescriptiveName}"
        }
    """.trimIndent()

        val response5 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
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
        updatedJsonResponse5["team"]["name"].asText() shouldBe dummyreport.organizationUnit!!.name
        updatedJsonResponse5["author"]["email"].asText() shouldBe dummyreport.author.email
        updatedJsonResponse5["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
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

        val response6 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateTeam)
            contentType(ContentType.Application.Json)
        }
        response6.status shouldBe HttpStatusCode.OK
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

        val updatedResponse6 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse6.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse6 = objectmapper.readTree(updatedResponse6.bodyAsText())

        updatedJsonResponse6["team"]["name"].asText() shouldBe "Team-dolly"
        updatedJsonResponse6["team"]["email"].asText() shouldBe "teamdolly@test.com"

        updatedJsonResponse6["descriptiveName"].asText() shouldBe newDescriptiveName
        updatedJsonResponse6["author"]["email"].asText() shouldBe dummyreport.author.email
        updatedJsonResponse6["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
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

        val response7 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateAuthor)
            contentType(ContentType.Application.Json)
        }
        response7.status shouldBe HttpStatusCode.OK

        val updatedResponse7 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse7.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse7 = objectmapper.readTree(updatedResponse7.bodyAsText())

        updatedJsonResponse7["author"]["email"].asText() shouldBe "author@test.com"

        updatedJsonResponse7["descriptiveName"].asText() shouldBe newDescriptiveName
        updatedJsonResponse7["team"]["name"].asText() shouldBe newTeamName
        updatedJsonResponse7["team"]["email"].asText() shouldBe newTeamEmail
        updatedJsonResponse7["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
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

        val response = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(metadataTest)
            contentType(ContentType.Application.Json)
        }
        response.status shouldBe HttpStatusCode.OK

        val updatedResponse = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse = objectmapper.readTree(updatedResponse.bodyAsText())

        updatedJsonResponse["descriptiveName"].asText() shouldBe "Updated Report Title"
        updatedJsonResponse["team"]["name"].asText() shouldBe "Team-dolly"
        updatedJsonResponse["team"]["email"].asText() shouldBe "teamdolly@test.com"
        updatedJsonResponse["author"]["email"].asText() shouldBe "author@test.com"
        updatedJsonResponse["successCriteria"].toList().size shouldBe dummyreport.successCriteria.size
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
                "breakingTheLaw": "nei",
                "lawDoesNotApply": "nei",
                "tooHardToComply": "nei",
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
                "number": "flere updated number",
                "breakingTheLaw": "nei",
                "lawDoesNotApply": "nei",
                "tooHardToComply": "ja",
                "contentGroup": "Group 1",
                "status": "COMPLIANT",
                "wcagLevel": "AA"
            }]
        }
    """.trimIndent()
        //patch
        val response3 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(multipleCriteriaUpdate)
            contentType(ContentType.Application.Json)
        }
        response3.status shouldBe HttpStatusCode.OK

        val updatedResponse3 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse3.status shouldBe HttpStatusCode.OK
        val updatedResponseBody3 = updatedResponse3.bodyAsText()
        val updatedJsonResponse3 = objectmapper.readTree(updatedResponseBody3)

        val criteriaList = updatedJsonResponse3["successCriteria"].toList()
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
        updatedCriterion1["wcagLevel"].asText() shouldBe originalCriterion1.wcagLevel

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
        updatedCriterion2["wcagLevel"].asText() shouldBe originalCriterion2.wcagLevel

        val otherCriteria = criteriaList.filter { it["number"].asText() != "1.3.1"
                && it["number"].asText() != "1.3.2"}
        otherCriteria.isNotEmpty() shouldBe true
    }


    @Test
    fun `full update`() = setupTestApi(database) {
        val updateDescriptiveName = """
        {
            "reportId": "${dummyreport.reportId}",
            "descriptiveName": "Updated Report Title"
        }
    """.trimIndent()

        val response = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateDescriptiveName)
            contentType(ContentType.Application.Json)
        }
        response.status shouldBe HttpStatusCode.OK

        val updatedResponse = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse = objectmapper.readTree(updatedResponse.bodyAsText())

        updatedJsonResponse["descriptiveName"].asText() shouldBe "Updated Report Title"

        updatedJsonResponse["team"].asText() shouldBe dummyreport.organizationUnit
        updatedJsonResponse["author"].asText() shouldBe dummyreport.author
        updatedJsonResponse["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse["lastChanged"].asText() shouldBe dummyreport.lastChanged

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

        val response1 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateTeam)
            contentType(ContentType.Application.Json)
        }
        response1.status shouldBe HttpStatusCode.OK

        val updatedResponse1 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse1.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse1 = objectmapper.readTree(updatedResponse1.bodyAsText())

        updatedJsonResponse1["team"]["name"].asText() shouldBe "Team-dolly"
        updatedJsonResponse1["team"]["email"].asText() shouldBe "teamdolly@test.com"

        updatedJsonResponse1["descriptiveName"].asText() shouldBe dummyreport.descriptiveName
        updatedJsonResponse1["author"].asText() shouldBe dummyreport.author
        updatedJsonResponse1["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse1["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse1["lastChanged"].asText() shouldBe dummyreport.lastChanged

        val updateAuthor = """
        {
            "reportId": "${dummyreport.reportId}",
            "author": {
                "email": "author@test.com",
                "oid": "123"
            }
        }
    """.trimIndent()

        val response2 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateAuthor)
            contentType(ContentType.Application.Json)
        }
        response2.status shouldBe HttpStatusCode.OK

        val updatedResponse2 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse2.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse2 = objectmapper.readTree(updatedResponse2.bodyAsText())

        updatedJsonResponse2["author"]["email"].asText() shouldBe "author@test.com"

        updatedJsonResponse2["descriptiveName"].asText() shouldBe dummyreport.descriptiveName
        updatedJsonResponse2["team"].asText() shouldBe dummyreport.organizationUnit
        updatedJsonResponse2["successCriteria"].asText() shouldBe dummyreport.successCriteria
        updatedJsonResponse2["created"].asText() shouldBe dummyreport.created
        updatedJsonResponse2["lastChanged"].asText() shouldBe dummyreport.lastChanged

        val originalCriterion = 1.perceivable("1.3.4", "Visningsretning") {
            description = "Brukeren må få velge om innholdet skal vises i liggende eller stående retning."
            guideline = `1-3 Mulig å tilpasse`
            tools = devTools
            wcagUrl = "https://www.w3.org/WAI/WCAG21/Understanding/orientation"
        }.levelA()

        val updateCriterion = """
        {
            "reportId": "${dummyreport.reportId}",
            "criteria": {
                "id": "criterion-id",
                "name": "Updated Criterion",
                "description": "Updated Description",
                "principle": "Updated Principle",
                "guideline": "Updated Guideline",
                "tools": "Updated Tools",
                "number": "Updated Number",
                "breakingTheLaw": "ja",
                "lawDoesNotApply": "ja",
                "tooHardToComply": "ja",
                "contentGroup": "Group 1",
                "status": "PARTIALLY_COMPLIANT",
                "wcagLevel": "A"
            }
        }
        """.trimIndent()
        val response3 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/update") {
            setBody(updateCriterion)
            contentType(ContentType.Application.Json)
        }
        response3.status shouldBe HttpStatusCode.OK

        val updatedResponse3 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse3.status shouldBe HttpStatusCode.OK
        val updatedJsonResponse3 = objectmapper.readTree(updatedResponse3.bodyAsText())
        val criteriaList = updatedJsonResponse3["successCriteria"].toList()
        criteriaList.size shouldBe 49
            val specificCriterion = criteriaList.find { it["number"].asText() == "1.3.4" }
            specificCriterion?.let {
                it["name"].asText() shouldBe originalCriterion.name
                it["description"].asText() shouldBe originalCriterion.description
                it["principle"].asText() shouldBe originalCriterion.principle
                it["guideline"].asText() shouldBe originalCriterion.guideline
                it["tools"].asText() shouldBe originalCriterion.tools
                it["number"].asText() shouldBe originalCriterion.number
                it["breakingTheLaw"].asText() shouldBe "ja"
                it["lawDoesNotApply"].asText() shouldBe "ja"
                it["tooHardToComply"].asText() shouldBe "ja"
                it["contentGroup"].asText() shouldBe "Group 1"
                it["status"].asText() shouldBe "PARTIALLY_COMPLIANT"
                it["wcagLevel"].asText() shouldBe originalCriterion.wcagLevel
            } ?: throw ResourceNotFoundException("Criterion", "1.3.4")

            val otherCriteria = criteriaList.filter { it["number"].asText() != "1.3.4" }
            otherCriteria.isNotEmpty() shouldBe true


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
            "breakingTheLaw": "ja",
            "lawDoesNotApply": "ja",
            "tooHardToComply": "ja",
            "contentGroup": "Group 1",
            "status": "COMPLIANT",
            "wcagLevel": "AA"
        }]
        
        }""".trimIndent()

        val response4 = client.patchWithJwtUser(testUser, "api/reports/${dummyreport.reportId}/partialUpdate") {
            setBody(fullUpdate)
            contentType(ContentType.Application.Json)
        }
        response4.status shouldBe HttpStatusCode.OK

        val updatedResponse4 = client.get("api/reports/${dummyreport.reportId}")
        updatedResponse4.status shouldBe HttpStatusCode.OK
        val updatedResponseBody4 = updatedResponse4.bodyAsText()
        val updatedJsonResponse4 = objectmapper.readTree(updatedResponseBody4)

        updatedJsonResponse4["descriptiveName"].asText() shouldBe "fullUpdated Report Title"
        updatedJsonResponse4["team"]["name"].asText() shouldBe "Team-Universell Utforming"
        updatedJsonResponse4["team"]["email"].asText() shouldBe "team@testUU.com"
        updatedJsonResponse4["author"]["email"].asText() shouldBe "fullupdateduser@test.com"
        val criteriaList2 = updatedJsonResponse3["successCriteria"].toList()
            criteriaList2.size shouldBe 49
        val specificCriterion2 = criteriaList.find { it["number"].asText() == "1.3.4" }
        specificCriterion2?.let {

            it["name"].asText() shouldBe originalCriterion.name
            it["description"].asText() shouldBe originalCriterion.description
            it["principle"].asText() shouldBe originalCriterion.principle
            it["guideline"].asText() shouldBe originalCriterion.guideline
            it["tools"].asText() shouldBe originalCriterion.tools
            it["number"].asText() shouldBe originalCriterion.number
            it["breakingTheLaw"].asText() shouldBe "ja"
            it["lawDoesNotApply"].asText() shouldBe "ja"
            it["tooHardToComply"].asText() shouldBe "ja"
            it["contentGroup"].asText() shouldBe "Group 1"
            it["status"].asText() shouldBe "COMPLIANT"
            it["wcagLevel"].asText() shouldBe originalCriterion.wcagLevel

        }?: throw ResourceNotFoundException("Criterion", "1.3.4")

        val otherCriteria2 = criteriaList.filter { it["number"].asText() != "1.3.4" }
        otherCriteria2.isNotEmpty() shouldBe true
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
