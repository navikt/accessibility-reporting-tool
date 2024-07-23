package accessibility.reporting.tool.html

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.getWithJwtUser
import accessibility.reporting.tool.uuidStr
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import org.junit.jupiter.api.Test

class LegacyAdminRoutesTest {
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


    @Test
    fun `admin routes`() = setupLegacyTestApi(db) {
        client.getWithJwtUser(testAdminUser, "/admin").status shouldBe HttpStatusCode.OK
        client.getWithJwtUser(testUser, "/admin").status shouldBe HttpStatusCode.Unauthorized
    }
}