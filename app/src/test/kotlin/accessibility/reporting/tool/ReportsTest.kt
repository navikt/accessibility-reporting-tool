package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportsTest {
    private val db = LocalPostgresDatabase.cleanDb()

    @Test
    fun `når alle rapportRuter`()= testApplication{
        application {
            authentication { jwt {
                skipWhen { true }
            } }
            api(repository = ReportRepository(db), authInstaller = {})
        }
       // client.get("/reports").status shouldBe HttpStatusCode.OK
       // client.get("/reports/someid").status shouldBe HttpStatusCode.OK
       // client.get("/reports/new").status shouldBe HttpStatusCode.OK

    }
}