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
    user: User? = null,
    descriptiveName: String? = null
) =
    AggregatedReport(
        url = "https://aggregated.test",
        descriptiveName = descriptiveName ?: "Aggregated dummy report",
        user = user ?: User(email = defaultUserEmail, name = defaultUserName, oid = defaultUserOid, groups = listOf()),
        organizationUnit = orgUnit,
        reports = listOf(
            dummyReportV2(),
            dummyReportV2(orgUnit = OrganizationUnit("something", "something", "something"))
        )
    )

fun setupTestApi(
    database: LocalPostgresDatabase,
    orgRepository: OrganizationRepository? = null,
    reportRepository: ReportRepository? = null,
    withEmptyAuth: Boolean = false,
    block: suspend ApplicationTestBuilder.() -> Unit
) = testApplication {
    application {
        api(
            reportRepository = reportRepository ?: ReportRepository(database),
            organizationRepository = orgRepository ?: OrganizationRepository(database),
            corsAllowedOrigins = listOf("*.this.shitt"),
            corsAllowedSchemes = listOf("http", "https")
        ) {
            if (withEmptyAuth) {
                mockEmptyAuth()
            } else installJwtTestAuth()
        }
    }
    block()
}

fun Application.mockEmptyAuth() = authentication {
    jwt {
        skipWhen { true }
    }
}

class TestUser(email: String? = null, name: String, groups: List<String> = listOf()) {
    private val emailStr = email ?: "$name@test.nav"
    val original =
        User(
            email = User.Email(s = emailStr),
            name = name,
            oid = User.Oid(UUID.randomUUID().toString()),
            groups = groups
        )
    val capitalized = original.copy(email = User.Email(emailStr.replaceFirstChar(Char::titlecase)))

    companion object {
        fun createAdminUser(email: String? = null, name: String) = TestUser(email, name, listOf("test_admin"))
    }
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