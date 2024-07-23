package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.getWithJwtUser
import accessibility.reporting.tool.uuidStr
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class LegacyRoutesTest {
    private val db = LocalPostgresDatabase.cleanDb()

    private val testAdminUser = User(
        email = User.Email("admin@test.nav"),
        name = "Hello Test",
        oid = User.Oid(uuidStr()),
        groups = listOf("test_admin")
    )

    private val testUser = User(
        email = User.Email("notadmin@test.nav"),
        name = "Hello Test",
        oid = User.Oid(uuidStr()),
        groups = listOf()
    )

    @ParameterizedTest
    @ValueSource(strings = ["/", "/orgunit", "/user", "faq"])
    fun `summary pages`(url: String) = setupLegacyTestApi(
        database = LocalPostgresDatabase.cleanDb()
    ) {
        client.getWithJwtUser(testUser, url).status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testAdminUser, url).status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `admin routes`() = setupLegacyTestApi(db){
        client.getWithJwtUser(testAdminUser, "/admin").status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser, "/admin").status shouldBe HttpStatusCode.Unauthorized
    }
}