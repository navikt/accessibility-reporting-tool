package accessibility.reporting.tool

import accessibility.reporting.tool.database.toStringList
import assert
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateTeamTest: TestApi() {
    private val testOrg = createTestOrg(
        name = "DummyOrg",
        email = "test@nav.no"
    )
    private val testUser = TestUser(email = "testuser@nav.no", name = "Test User")

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email,member) 
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
    }

    @BeforeEach
    fun populateDb() {
        database.update { queryOf("delete from organization_unit ") }
        organizationRepository.upsertOrganizationUnit(testOrg)
    }

    @Test
    fun `update team`() = withTestApi {

        val updateName = "team dolly"
        val updatedTeamName = """
             {
            "id": "${testOrg.id}",
            "name": "$updateName"
            }
            """.trimIndent()


        val teamIdPatchRequest = client.putWithJwtUser(testUser, "api/teams/${testOrg.id}/update") {
            setBody(updatedTeamName)
            contentType(ContentType.Application.Json)
        }
        teamIdPatchRequest.status shouldBe HttpStatusCode.OK

        val teamNameGetRequest = client.get("api/teams/${testOrg.id}")
        teamNameGetRequest.status shouldBe HttpStatusCode.OK
        val teamNameUpdate = testApiObjectmapper.readTree(teamNameGetRequest.bodyAsText())

        teamNameUpdate["name"].asText() shouldBe updateName

        teamNameUpdate["email"].asText() shouldBe testOrg.email
        teamNameUpdate["members"].toList().size shouldBe testOrg.members.size

        val updateEmail = "teamdolly@test.com"
        val updatedEmail = """
             {
            "id": "${testOrg.id}",
            "email": "$updateEmail"
           }
           
        """.trimIndent()

        val teamEmailPatchRequest = client.putWithJwtUser(testUser, "api/teams/${testOrg.id}/update") {
            setBody(updatedEmail)
            contentType(ContentType.Application.Json)
        }
        teamEmailPatchRequest.status shouldBe HttpStatusCode.OK

        val teamEmailGetRequest = client.get("api/teams/${testOrg.id}")
        teamEmailGetRequest.status shouldBe HttpStatusCode.OK
        val teamEmailUpdate = testApiObjectmapper.readTree(teamEmailGetRequest.bodyAsText())

        teamEmailUpdate["email"].asText() shouldBe "teamdolly@test.com"

        teamEmailUpdate["name"].asText() shouldBe updateName
        teamEmailUpdate["members"].toList().size shouldBe testOrg.members.size

        val updatedMembers = """
            {
            "id": "${testOrg.id}",
            "members": ["testuser2@nav.no"]
            }
        """.trimIndent()

        val teamMemberUpdateRequest = client.putWithJwtUser(testUser, "api/teams/${testOrg.id}/update") {
            setBody(updatedMembers)
            contentType(ContentType.Application.Json)
        }
        teamMemberUpdateRequest.status shouldBe HttpStatusCode.OK

        val teamMembersGetRequest = client.get("api/teams/${testOrg.id}")
        teamMembersGetRequest.status shouldBe HttpStatusCode.OK
        val teamMemberUpdate = testApiObjectmapper.readTree(teamMembersGetRequest.bodyAsText())
        teamMemberUpdate["members"].toList().assert {
            this.size shouldBe 1
            first().asText() shouldBe "testuser2@nav.no"

        }
        teamMemberUpdate["name"].asText() shouldBe updateName
        teamMemberUpdate["email"].asText() shouldBe updateEmail
    }
}