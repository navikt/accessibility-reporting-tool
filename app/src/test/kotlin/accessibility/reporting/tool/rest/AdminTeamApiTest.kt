package accessibility.reporting.tool.rest

import accessibility.reporting.tool.*
import accessibility.reporting.tool.database.Database
import accessibility.reporting.tool.database.toStringList
import accessibility.reporting.tool.wcag.OrganizationUnit
import assert
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.Test

class AdminTeamApiTest : TestApi() {
    val testAdmin = TestUser.createAdminUser("some@admin.test", "Admin Adminser")
    val testTeamOwner = TestUser(name = "Usy Usery")
    val testTeamMember = TestUser(name = "Member Memberson")
    private val adminTestOrg = createTestOrg(testAdmin, listOf(testTeamMember.email))
    private val userTestOrg = createTestOrg(testTeamOwner, listOf(testTeamMember.email))


    @Test
    fun `only admin can delete organization unit`() = withTestApi {
        database.insertTestOrg(adminTestOrg)
        database.insertTestOrg(userTestOrg)

        client.deleteWithJwtUser(testAdmin,"/api/admin/teams/${adminTestOrg.id}").assert {
            status shouldBe HttpStatusCode.OK
        }

        client.getWithJwtUser(testAdmin, "/api/teams/${adminTestOrg.id}/details").assert {
            status shouldBe HttpStatusCode.NotFound
        }


        client.deleteWithJwtUser(testTeamMember,"/api/admin/teams/${userTestOrg.id}").assert {
            status shouldBe HttpStatusCode.Forbidden
        }
        client.getWithJwtUser(testAdmin, "/api/teams/${userTestOrg.id}/details").assert {
            status shouldBe HttpStatusCode.OK
        }
        client.deleteWithJwtUser(testTeamOwner,"/api/admin/teams/${userTestOrg.id}").assert {
            status shouldBe HttpStatusCode.Forbidden
        }

        client.getWithJwtUser(testAdmin, "/api/teams/${userTestOrg.id}/details").assert {
            status shouldBe HttpStatusCode.OK
        }

        client.deleteWithJwtUser(testAdmin,"/api/admin/teams/${userTestOrg.id}").assert {
            status shouldBe HttpStatusCode.OK
        }

        client.getWithJwtUser(testAdmin, "/api/teams/${userTestOrg.id}/details").assert {
            status shouldBe HttpStatusCode.NotFound
        }

    }

    private fun Database.insertTestOrg(testOrg: OrganizationUnit) {
        update {
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
}