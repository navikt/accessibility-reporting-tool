package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Version
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRoutesTest {
    private val db = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(db)

    @Disabled
    @Test
    fun `når alle rapportRuter`() = testApplication {
        application {
            authentication {
                jwt {
                    skipWhen { true }
                }
            }
            api(repository = ReportRepository(db), authInstaller = {})
        }

        repository.upsertReport(
            Report(
                reportId = "knownid",
                url = "tadda",
                organizationUnit = null,
                version = Version.V1,
                testData = null,
                user = User(email = "tadda", name = "tadda"),
                successCriteria = Version.V1.criteria,
                filters = mutableListOf()
            )
        )

        client.get("/reports").status shouldBe HttpStatusCode.OK
        client.get("/reports/knownid").status shouldBe HttpStatusCode.OK
        client.get("/reports/notknownid").status shouldBe HttpStatusCode.BadRequest
        client.get("/reports/new").status shouldBe HttpStatusCode.OK
    }
}