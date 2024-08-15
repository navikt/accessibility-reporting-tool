package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.toStringList
import accessibility.reporting.tool.wcag.OrganizationUnit
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
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateTeamTest: TestApi() {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )

    private val testUser =
        User(User.Email("testuser@nav.no"), "Test User", User.Oid(UUID.randomUUID().toString()), groups = listOf())
    private val testUser2 =
        User(User.Email("testuser2@nav.no"), "Test User2", User.Oid(UUID.randomUUID().toString()), groups = listOf())


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
            "name": "${updateName}"
            }
            """.trimIndent()


        val teamIdPatchRequest = client.putWithJwtUser(testUser, "api/teams/${testOrg.id}/update") {
            setBody(updatedTeamName)
            contentType(ContentType.Application.Json)
        }
        teamIdPatchRequest.status shouldBe HttpStatusCode.OK

        val teamNameGetRequest = client.get("api/teams/${testOrg.id}/details")
        teamNameGetRequest.status shouldBe HttpStatusCode.OK
        val teamNameUpdate = objectmapper.readTree(teamNameGetRequest.bodyAsText())

        teamNameUpdate["name"].asText() shouldBe updateName

        teamNameUpdate["email"].asText() shouldBe testOrg.email
        teamNameUpdate["members"].toList().size shouldBe testOrg.members.size

        val updateEmail = "teamdolly@test.com"
        val updatedEmail = """
             {
            "id": "${testOrg.id}",
            "email": "${updateEmail}"
           }
           
        """.trimIndent()

        val teamEmailPatchRequest = client.putWithJwtUser(testUser, "api/teams/${testOrg.id}/update") {
            setBody(updatedEmail)
            contentType(ContentType.Application.Json)
        }
        teamEmailPatchRequest.status shouldBe HttpStatusCode.OK

        val teamEmailGetRequest = client.get("api/teams/${testOrg.id}/details")
        teamEmailGetRequest.status shouldBe HttpStatusCode.OK
        val teamEmailUpdate = objectmapper.readTree(teamEmailGetRequest.bodyAsText())

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

        val teamMembersGetRequest = client.get("api/teams/${testOrg.id}/details")
        teamMembersGetRequest.status shouldBe HttpStatusCode.OK
        val teamMemberUpdate = objectmapper.readTree(teamMembersGetRequest.bodyAsText())
        teamMemberUpdate["members"].toList().assert {
            this.size shouldBe 1
            first().asText() shouldBe "testuser2@nav.no"

        }
        teamMemberUpdate["name"].asText() shouldBe updateName
        teamMemberUpdate["email"].asText() shouldBe updateEmail
    }
}