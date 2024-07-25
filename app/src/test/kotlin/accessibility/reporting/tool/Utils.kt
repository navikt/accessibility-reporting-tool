package accessibility.reporting.tool

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.withClue
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import java.util.*

val defaultUserEmail = User.Email("tadda@test.tadda")
const val defaultUserName = "Tadda Taddasen"
val defaultUserOid = User.Oid(UUID.randomUUID().toString())
val objectmapper = jacksonObjectMapper()
fun dummyReportV2(
    url: String = "http://dummyurl.test",
    orgUnit: OrganizationUnit? = null,
    user: User = User(email = defaultUserEmail, name = defaultUserName, oid = defaultUserOid, groups = listOf()),
    reportType: ReportType = ReportType.SINGLE,
    id: String = UUID.randomUUID().toString(),
    descriptiveName: String = "Dummynavn"
) = Report(
    reportId = id,
    url = url,
    organizationUnit = orgUnit,
    version = Version.V2,
    author = user.toAuthor(),
    successCriteria = Version.V2.criteria,
    lastChanged = LocalDateTimeHelper.nowAtUtc(),
    created = LocalDateTimeHelper.nowAtUtc(),
    lastUpdatedBy = null,
    descriptiveName = descriptiveName,
    reportType = reportType
)

fun dummyAggregatedReportV2(
    orgUnit: OrganizationUnit? = null,
) =
    AggregatedReport(
        url = "https://aggregated.test",
        descriptiveName = "Aggregated dummy report",
        user = User(email = defaultUserEmail, name = defaultUserName, oid = defaultUserOid, groups = listOf()),
        organizationUnit = orgUnit,
        reports = listOf(
            dummyReportV2(),
            dummyReportV2(orgUnit = OrganizationUnit("something", "something", "something"))
        )
    )

fun setupTestApi(
    database: LocalPostgresDatabase,
    withEmptyAuth: Boolean = false,
    block: suspend ApplicationTestBuilder.() -> Unit
) = testApplication {
    application {
        api(
            reportRepository = ReportRepository(database),
            organizationRepository = OrganizationRepository(database),
            corsAllowedOrigins = listOf("*"),
            corsAllowedSchemes = listOf("http", "https")
        ) {
            if (withEmptyAuth) {
                mockEmptyAuth()
            } else installJwtTestAuth()
        }
    }
    block()
}

private fun Application.mockEmptyAuth() = authentication {
    jwt {
        skipWhen { true }
    }
}

class TestUser(email: String, name: String, groups: List<String> = listOf()) {
    val original =
        User(email = User.Email(s = email), name = name, oid = User.Oid(UUID.randomUUID().toString()), groups = groups)
    val capitalized = original.copy(email = User.Email(email.replaceFirstChar(Char::titlecase)))
}

fun withJsonClue(jsonField: String, assertFuntion: (String) -> Unit) {
    withClue("Jsonfield: $jsonField") {
        try {
            assertFuntion(jsonField)
        } catch (nullpointerException: NullPointerException) {
            throw JsonAssertionFailedException(jsonField)
        }
    }
}

class JsonAssertionFailedException(field: String) : Throwable("field $field is not present in jsonnode")