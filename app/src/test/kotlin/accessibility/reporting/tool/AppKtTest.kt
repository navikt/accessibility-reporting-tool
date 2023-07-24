package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import assert
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppKtTest {

    private val db = LocalPostgresDatabase.cleanDb()
    private val testuserEmail = "test@testing.ok"
    private val testuserName = "Testa testerson"

    @Test
    fun testGetIsalive() = testApplication {
        application {
            authentication {
                jwt {
                    skipWhen { true }
                }
            }
            api(repository = ReportRepository(db), authInstaller = {})
        }

        client.get("/isAlive").status shouldBe HttpStatusCode.OK
    }
}