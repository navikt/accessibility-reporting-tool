package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.*
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
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
            corsAllowedOrigins = listOf("*.nav.no"),
            corsAllowedSchemes = listOf("http", "https")
        ) {
            if (withEmptyAuth) {
                mockEmptyAuth()
            } else installJwtTestAuth()
        }
    }
    block()
}


suspend fun HttpClient.assertErrorOnSubmit(url: String, block: suspend ErrorAsserter.() -> Unit) {
    ErrorAsserter(this, url).block()
}

class ErrorAsserter(val httpClient: HttpClient, val urlString: String) {
    suspend fun assertBadRequest(requestConfig: ErrorRequestConfig.() -> Unit) {
        val statusCode = ErrorRequestConfig(urlString).apply(requestConfig)
            .doSubmit(httpClient).status
        withClue("Expected $urlString to return 403: BadRequest, actual statuscode was $statusCode") {
            statusCode shouldBe HttpStatusCode.BadRequest
        }
    }

    suspend fun assertForbidden(requestConfig: ErrorRequestConfig.() -> Unit) {
        val statusCode = ErrorRequestConfig(urlString).apply(requestConfig)
            .doSubmit(httpClient).status
        withClue("Expected $urlString to return 403: Forbidden, actual statuscode was $statusCode") {
            statusCode shouldBe HttpStatusCode.Forbidden
        }
    }

    class ErrorRequestConfig(val url: String) {
        lateinit var user: TestUser
        var parametersBuilder: (ParametersBuilder.() -> Unit)? = null

        suspend fun doSubmit(httpClient: HttpClient) =
            httpClient.submitWithJwtUser(user.original, url, parametersBuilder)


    }
}

suspend fun HttpClient.submitWithJwtUser(
    user: User,
    urlString: String,
    appendParameters: (ParametersBuilder.() -> Unit?)? = null
) = submitForm(
    url = urlString,
    formParameters = Parameters.build {
        if (appendParameters != null)
            appendParameters()
    }
) {
    header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
}

suspend fun HttpClient.submitWithJwtTestUser(
    testUser: TestUser,
    urlString: String,
    appendParameters: (ParametersBuilder.() -> Unit?)? = null
) = submitWithJwtUser(testUser.original, urlString, appendParameters)

suspend fun HttpClient.adminAndNonAdminsShouldBeOK(testUser: TestUser, testAdminUser: TestUser, url: String) {
    getWithJwtUser(testUser.original, url).status shouldBe HttpStatusCode.OK
    getWithJwtUser(testUser.capitalized, url).status shouldBe HttpStatusCode.OK
    getWithJwtUser(testAdminUser.original, url).status shouldBe HttpStatusCode.OK
    getWithJwtUser(testAdminUser.capitalized, url).status shouldBe HttpStatusCode.OK
}

fun createTestAdminAndTestUser() = Pair(
    TestUser(
        email = "admin@test.nav",
        name = "Hello Test",
        groups = listOf("test_admin")
    ),
    TestUser(
        email = "notadmin@test.nav",
        name = "Hello Test",
    )
)