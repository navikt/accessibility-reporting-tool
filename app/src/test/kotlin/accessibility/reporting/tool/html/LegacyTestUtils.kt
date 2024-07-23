package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.api
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.installJwtTestAuth
import accessibility.reporting.tool.mockEmptyAuth
import io.ktor.server.testing.*

fun setupLegacyTestApi(
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