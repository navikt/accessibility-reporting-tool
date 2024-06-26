package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.database.ReportRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ApiTest {


        private val db = mockk<LocalPostgresDatabase>()
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

            client.get("isalive")
            client.get("isready")
            client.get("open/metrics").apply {
                bodyAsText() shouldNotBe  ""
            }
        }

}