package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.database.PostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppKtTest {

    private val db = LocalPostgresDatabase.cleanDb()

    @Test
    fun testGetIsalive() = testApplication {
        application {
            api(repository = ReportRepository(db), authInstaller = {})
        }
        assertEquals(HttpStatusCode.OK, client.get("/isAlive").status)
    }
}