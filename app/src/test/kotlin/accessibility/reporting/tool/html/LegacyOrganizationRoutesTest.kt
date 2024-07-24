package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.TestUser
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.deleteWithJwtUser
import accessibility.reporting.tool.getWithJwtUser
import assert
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.http.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Legacy routes test Testing that legacy routes are still available NB:
 * Does not test content of site, just that the correct statuses are
 * returned and that the correct operations is performed (manual testing IS
 * neccesary)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyOrganizationRoutesTest {
    private val db = LocalPostgresDatabase.cleanDb()


    @BeforeEach
    fun cleanDb() {
        LocalPostgresDatabase.prepareForNextTest()
    }

    @Test
    fun `Should create new organization unit`() = setupLegacyTestApi(db) {
        val (testAdminUser, testUser) = createTestAdminAndTestUser()
        val testUnit = "New and shiny testunit"
        val expectedTestUnitId = "new-and-shiny-testunit"

        client.adminAndNonAdminsShouldBeOK(testUser, testAdminUser, orgSubRoute("new"))
        client.getWithJwtUser(testUser.original, orgSubRoute("does-not-exists")).status shouldBe HttpStatusCode.NotFound

        //new
        client.submitWithJwtUser(testUser.original, orgSubRoute("new")) {
            append("unit-email", testUser.original.email.str())
            append("unit-name", testUnit)
        }.assert {
            status shouldBe HttpStatusCode.Created
            headers["HX-Redirect"] shouldBe "/orgunit"
        }
        client.adminAndNonAdminsShouldBeOK(testUser, testAdminUser, orgSubRoute(expectedTestUnitId))

        client.assertErrorOnSubmit(orgSubRoute("new")) {
            assertBadRequest {
                user = testUser
                parametersBuilder = null
            }
            assertBadRequest {
                user = testUser
                parametersBuilder = {
                    append("unit-email", testUser.original.email.str())
                }
            }
            assertBadRequest {
                user = testUser
                parametersBuilder = {
                    append("unit-name", testUser.original.email.str())
                }
            }

        }
    }

    @Test
    fun `Should update owner `() = setupLegacyTestApi(db) {
        val newOrgName = "Unit with Owner"
        val orgid = "unit-with-owner"
        val ownerUpdateRoute = orgSubRoute("$orgid/owner")
        val newOwner =
            TestUser(email = "new.new@test.nav", name = "The New New")
        val (testAdminUser,testUser)=createTestAdminAndTestUser()



        client.postNewOrg(testUser.original, newOrgName)

        client.submitWithJwtUser(testUser.original, ownerUpdateRoute) {
            append("orgunit-email", newOwner.original.email.str())
        }.status shouldBe HttpStatusCode.OK

        client.submitWithJwtUser(newOwner.capitalized, ownerUpdateRoute) {
            append("orgunit-email", "anotherOne@test.nav")
        }.status shouldBe HttpStatusCode.OK

        client.submitWithJwtUser(testAdminUser.original, ownerUpdateRoute) {
            append("orgunit-email", "another@test.nav")
        }.status shouldBe HttpStatusCode.OK

        client.assertErrorOnSubmit(ownerUpdateRoute) {
            assertBadRequest {
                user = testAdminUser
            }
            assertBadRequest {
                user = testAdminUser
                parametersBuilder = {
                    append("organic-email", "newowner@test.nav")
                }
            }
        }
    }

    @Test
    fun `Should delete organization unit`() = setupLegacyTestApi(db) {
        val orgName = "Unit with Owner"
        val orgRoute = orgSubRoute("unit-with-owner")
        val otherUser =
            TestUser(email = "new.new@test.nav", name = "The New New")
        val (testAdminUser,testUser)=createTestAdminAndTestUser()

        //should be able to delete organization you own
        client.postNewOrg(testUser.original, orgName)
        client.deleteWithJwtUser(testUser.original, orgRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original, orgRoute).status shouldBe HttpStatusCode.NotFound

        //admin should be allowed to delete org
        client.postNewOrg(otherUser.original, orgName)
        client.deleteWithJwtUser(testAdminUser.original, orgRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original, orgRoute).status shouldBe HttpStatusCode.NotFound

        //user should be recognized regardless of caps
        client.postNewOrg(otherUser.original, orgRoute)
        client.deleteWithJwtUser(otherUser.capitalized, orgRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original, orgRoute).status shouldBe HttpStatusCode.NotFound

    }

    @Test
    fun `Should add and remove members in organization unit`() = setupLegacyTestApi(database = db) {
        val testorgName = "Another one"
        val testorgId = "another-one"
        val memberRoute = orgSubRoute("member")
        val otherMemberEmail = "tadda@test.nav"
        val otherMember2Email = "tadda2@test.nav"
        val otherMember3Email = "tadda3@test.nav"
        val (testAdminUser,testUser)=createTestAdminAndTestUser()

        client.postNewOrg(testUser.original, testorgName)
        client.submitWithJwtTestUser(testUser, memberRoute) {
            append("member", otherMemberEmail)
            append("orgunit", testorgId)
        }.status shouldBe HttpStatusCode.OK

        client.assertErrorOnSubmit(memberRoute) {
            assertBadRequest {
                user = testAdminUser
                parametersBuilder = {
                    append("member", otherMemberEmail)
                }
            }
            assertBadRequest {
                user = testAdminUser
                parametersBuilder = {
                    append("org-unit", "no-exists")
                }
            }
            assertBadRequest {
                user = testAdminUser
                parametersBuilder = {
                    append("org-unit", testorgId)
                }
            }
        }

        client.submitWithJwtTestUser(testUser, memberRoute) {
            append("member", otherMember2Email)
            append("orgunit", testorgId)
        }.status shouldBe HttpStatusCode.OK
        client.submitWithJwtTestUser(testUser, memberRoute) {
            append("member", otherMember3Email)
            append("orgunit", testorgId)
        }.status shouldBe HttpStatusCode.OK

        client.deleteWithJwtUser(testUser.original, "$memberRoute?email=$otherMember2Email&orgunit=$testorgId")
            .status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testUser.original, "$memberRoute?email=nope&orgunit=$testorgId")
            .status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testUser.original, "$memberRoute?email=nope&orgunit=notorg")
            .status shouldBe HttpStatusCode.BadRequest

    }

    private fun orgSubRoute(route: String) = "orgunit/$route"

    private suspend fun HttpClient.postNewOrg(user: User, newName: String) =
        submitWithJwtUser(user, orgSubRoute("new")) {
            append("unit-email", user.email.str())
            append("unit-name", newName)
        }.also {
            if (it.status != HttpStatusCode.Created) throw TestSetupException("Post new org failed with status ${it.status}")
        }
}

private class TestSetupException(override val message: String?) : Throwable()