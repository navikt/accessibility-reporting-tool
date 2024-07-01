package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.authenitcation.User.Oid
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportType
import accessibility.reporting.tool.wcag.Version
import assert
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
        val testUserOid = UUID.randomUUID().toString()
        application {
            authentication {
                jwt {
                    skipWhen { true }
                }
            }
            api(
                repository = ReportRepository(db),
                corsAllowedOrigins = "*",
                corsAllowedSchemes = listOf("http", "https"),
                authInstaller = {})
        }

        repository.upsertReport(
            Report(
                reportId = "knownid",
                url = "tadda",
                organizationUnit = null,
                version = Version.V2,
                testData = null,
                author = User(
                    email = Email("tadda"),
                    name = "tadda",
                    oid = Oid(testUserOid),
                    groups = listOf()
                ).toAuthor(),
                successCriteria = Version.V2.criteria,
                filters = mutableListOf(),
                created = LocalDateTimeHelper.nowAtUtc(),
                lastChanged = LocalDateTimeHelper.nowAtUtc(),
                lastUpdatedBy = null,
                descriptiveName = "Somename",
                reportType = ReportType.SINGLE
            )
        )

        client.get("/").assert {
            this.status shouldBe HttpStatusCode.OK
            this.headers["Content-Type"] shouldBe "text/html"
        }
        client.get("/reports/knownid").status shouldBe HttpStatusCode.OK
        client.get("/reports/notknownid").status shouldBe HttpStatusCode.BadRequest
        client.get("/reports/new").status shouldBe HttpStatusCode.OK
    }
}