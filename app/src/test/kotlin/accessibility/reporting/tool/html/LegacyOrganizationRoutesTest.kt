package accessibility.reporting.tool.html

import LocalPostgresDatabase
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

    private val testAdminUser = User(
        email = Email("admin@test.nav"),
        name = "Hello Test",
        oid = Oid(uuidStr()),
        groups = listOf("test_admin")
    )

    private val testUser = User(
        email = Email("not.admin@test.nav"),
        name = "Hello Test",
        oid = Oid(uuidStr()),
        groups = listOf()
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
        client.getWithJwtUser(testUser, orgSubRoute("does-not-exists")).status shouldBe HttpStatusCode.NotFound

        //new
        client.submitWithJwtUser(testUser, orgSubRoute("new")) {
            append("unit-email", testUser.email.str())
            append("unit-name", testUnit)
        }.assert {
            status shouldBe HttpStatusCode.Created
            headers["HX-Redirect"] shouldBe "/orgunit"
        }
        client.adminAndNonAdminsShouldBeOK(orgSubRoute(expectedTestUnitId))

        client.assertErrorOnSubmit(orgSubRoute("new")) {
            assertBadRequest {
                user = testUser
                parametersBuilder = null
            }
            assertBadRequest {
                user = testUser
                parametersBuilder = {
                    append("unit-email", testUser.email.str())
                }
            }
            assertBadRequest {
                user = testUser
                parametersBuilder = {
                    append("unit-name", testUser.email.str())
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
            User(email = Email("new.new@test.nav"), name = "The New New", oid = Oid(uuidStr()), groups = emptyList())
        val newOwnerCapitalLetters =
            User(email = Email("new.new@test.nav"), name = "The New New", oid = Oid(uuidStr()), groups = emptyList())


        client.postNewOrg(testUser, newOrgName)

        client.submitWithJwtUser(testUser, ownerUpdateRoute) {
            append("orgunit-email", newOwner.email.str())
        }.status shouldBe HttpStatusCode.OK

        client.submitWithJwtUser(newOwnerCapitalLetters, ownerUpdateRoute) {
            append("orgunit-email", "anotherOne@test.nav")
        }.status shouldBe HttpStatusCode.OK

        client.submitWithJwtUser(testAdminUser, ownerUpdateRoute) {
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

            //OBS OBS: det er ikke satt noen grenser for hvem som får lov til å oppdatere eier
        }
    }


    @Test
    fun `delete organization unit`() = setupLegacyTestApi(db) {
        val orgName = "Unit with Owner"
        val orgId = "unit-with-owner"
        val deleteRoute = orgSubRoute(orgId)
        val otherUser =
            User(email = Email("new.new@test.nav"), name = "The New New", oid = Oid(uuidStr()), groups = emptyList())
        val otherUserCapitalized = otherUser.copy(email = Email("New.New@test.nav"))

        client.postNewOrg(testUser, orgName)
        client.getWithJwtUser(testUser,orgId).status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testUser, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser,orgId).status shouldBe HttpStatusCode.NotFound

        client.postNewOrg(otherUser, orgName)
        //client.deleteWithJwtUser(testUser, deleteRoute).status shouldBe HttpStatusCode.Forbidden
        client.getWithJwtUser(testUser,orgId).status shouldBe HttpStatusCode.OK
        client.deleteWithJwtUser(testAdminUser, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser,orgId).status shouldBe HttpStatusCode.NotFound

        client.postNewOrg(otherUser, orgName)
        client.deleteWithJwtUser(otherUserCapitalized, deleteRoute).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser,orgId).status shouldBe HttpStatusCode.NotFound
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
        getWithJwtUser(testUser, url).status shouldBe HttpStatusCode.OK
        getWithJwtUser(testAdminUser, url).status shouldBe HttpStatusCode.OK
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