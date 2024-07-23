package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.TestUser
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.*
import accessibility.reporting.tool.deleteWithJwtUser
import accessibility.reporting.tool.getWithJwtUser
import accessibility.reporting.tool.uuidStr
import assert
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.http.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/**
 * Legacy routes test Testing that legacy routes are still available NB:
 * Does not test content of site, just that the correct statuses are
 * returned and that the correct operations is performed (manual testing IS
 * neccesary)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyOrganizationRoutesTest {
    private val db = LocalPostgresDatabase.cleanDb()

    private val testAdminUser = TestUser(
        email = "admin@test.nav",
        name = "Hello Test",
        groups = listOf("test_admin")
    )

    private val testUser = TestUser(
        email = "not.admin@test.nav",
        name = "Hello Test",
    )

    @BeforeEach
    fun cleanDb() {
        LocalPostgresDatabase.prepareForNextTest()
    }

    @Test
    fun `Create new organization unit`() = setupLegacyTestApi(db) {
        val testUnit = "New and shiny testunit"
        val expectedTestUnitId = "new-and-shiny-testunit"

        client.adminAndNonAdminsShouldBeOK(orgSubRoute("new"))
        client.getWithJwtUser(testUser.original, orgSubRoute("does-not-exists")).status shouldBe HttpStatusCode.NotFound

        //new
        client.submitWithJwtUser(testUser.original, orgSubRoute("new")) {
            append("unit-email", testUser.original.email.str())
            append("unit-name", testUnit)
        }.assert {
            status shouldBe HttpStatusCode.Created
            headers["HX-Redirect"] shouldBe "/orgunit"
        }
        client.adminAndNonAdminsShouldBeOK(orgSubRoute(expectedTestUnitId))

        client.assertErrorOnSubmit(orgSubRoute("new")) {
            assertBadRequest {
                user = testUser.original
                parametersBuilder = null
            }
            assertBadRequest {
                user = testUser.original
                parametersBuilder = {
                    append("unit-email", testUser.original.email.str())
                }
            }
            assertBadRequest {
                user = testUser.original
                parametersBuilder = {
                    append("unit-name", testUser.original.email.str())
                }
            }

        }
    }

    @Test
    fun `Update owner `() = setupLegacyTestApi(db) {
        val newOrgName = "Unit with Owner"
        val orgid = "unit-with-owner"
        val ownerUpdateRoute = orgSubRoute("$orgid/owner")
        val newOwner =
            TestUser(email = "new.new@test.nav", name = "The New New")



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
                user = testAdminUser.original
            }
            assertBadRequest {
                user = testAdminUser.original
                parametersBuilder = {
                    append("organic-email", "newowner@test.nav")
                }
            }

            //OBS OBS: det er ikke satt noen grenser for hvem som får lov til å oppdatere eier
        }
    }


    @Disabled
    @Test
    fun `delete organization unit`() = setupLegacyTestApi(db) {
        val orgName = "Unit with Owner"
        val orgId = "unit-with-owner"
        val deleteRoute = orgSubRoute(orgId)
        val otherUser =
            User(email = Email("new.new@test.nav"), name = "The New New", oid = Oid(uuidStr()), groups = emptyList())
        val otherUserCapitalized = otherUser.copy(email = Email("New.New@test.nav"))

        client.postNewOrg(testUser.original, orgName)
        client.getWithJwtUser(testUser.original,orgId).status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testUser.original, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original,orgId).status shouldBe HttpStatusCode.NotFound

        client.postNewOrg(otherUser, orgName)
        //client.deleteWithJwtUser(testUser.original, deleteRoute).status shouldBe HttpStatusCode.Forbidden
        client.getWithJwtUser(testUser.original,orgId).status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testAdminUser.original, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original,orgId).status shouldBe HttpStatusCode.NotFound

        client.postNewOrg(otherUser, orgName)
        client.deleteWithJwtUser(otherUserCapitalized, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original,orgId).status shouldBe HttpStatusCode.NotFound
    }


    //delete /{id}
    //post {id}/owner
    //post {id}/member
    //delete {id}/member


    @ParameterizedTest
    @ValueSource(strings = ["/", "/orgunit", "/user", "faq"])
    fun `summary pages`(url: String) = setupLegacyTestApi(
        database = LocalPostgresDatabase.cleanDb()
    ) {
        client.adminAndNonAdminsShouldBeOK(url)
    }

    private fun orgSubRoute(route: String) = "orgunit/$route"

    private suspend fun HttpClient.adminAndNonAdminsShouldBeOK(url: String) {
        getWithJwtUser(testUser.original, url).status shouldBe HttpStatusCode.OK
        getWithJwtUser(testUser.capitalized, url).status shouldBe HttpStatusCode.OK
        getWithJwtUser(testAdminUser.original, url).status shouldBe HttpStatusCode.OK
        getWithJwtUser(testAdminUser.capitalized, url).status shouldBe HttpStatusCode.OK
    }

    private suspend fun HttpClient.postNewOrg(user: User, newName: String) =
        submitWithJwtUser(user, orgSubRoute("new")) {
            append("unit-email", user.email.str())
            append("unit-name", newName)
        }.also {
            if (it.status != HttpStatusCode.Created) throw TestSetupException("Post new org failed with status ${it.status}")
        }
}

private class TestSetupException(override val message: String?) : Throwable()