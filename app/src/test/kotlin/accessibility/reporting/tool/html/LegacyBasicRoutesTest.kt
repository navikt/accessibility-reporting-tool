package accessibility.reporting.tool.html

import accessibility.reporting.tool.TestUser
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.getWithJwtUser
import accessibility.reporting.tool.uuidStr
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyBasicRoutesTest {
    private val db = LocalPostgresDatabase.cleanDb()

    @ParameterizedTest
    @ValueSource(strings = ["/", "/orgunit", "/user", "faq"])
    fun `summary pages`(url: String) = setupLegacyTestApi(
        database = LocalPostgresDatabase.cleanDb()
    ) {
        val (testAdminUser, testUser) = createTestAdminAndTestUser()
        client.adminAndNonAdminsShouldBeOK(testUser, testAdminUser, url)
    }

    @Test
    fun `admin routes`() = setupLegacyTestApi(db) {
        val (testAdminUser, testUser) = createTestAdminAndTestUser()
        client.getWithJwtUser(testAdminUser.original, "/admin").status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser.original, "/admin").status shouldBe HttpStatusCode.Unauthorized
    }
}