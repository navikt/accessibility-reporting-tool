package accessibility.reporting.tool.html

import LocalPostgresDatabase
import accessibility.reporting.tool.JwtConfig
import accessibility.reporting.tool.api
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.OrganizationRepository
import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.installJwtTestAuth
import accessibility.reporting.tool.mockEmptyAuth
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



suspend fun HttpClient.assertErrorOnSubmit(url: String, block: suspend ErrorAsserter.() -> Unit) {
    ErrorAsserter(this, url).block()
}
class ErrorAsserter(val httpClient: HttpClient, val urlString: String) {
    suspend fun assertBadRequest(requestConfig: ErrorRequestConfig.() -> Unit) {
        val statusCode = ErrorRequestConfig(urlString).apply(requestConfig)
            .doSubmit(httpClient).status
        withClue("Expected $urlString to return 403: BadRequest, actual statuscode was $statusCode"){
          statusCode  shouldBe HttpStatusCode.BadRequest
        }
    }

    suspend fun assertForbidden(requestConfig: ErrorRequestConfig.() -> Unit) {
        val statusCode = ErrorRequestConfig(urlString).apply(requestConfig)
            .doSubmit(httpClient).status
        withClue("Expected $urlString to return 403: Forbidden, actual statuscode was $statusCode"){
            statusCode  shouldBe HttpStatusCode.Forbidden
        }
    }

    class ErrorRequestConfig(val url: String) {
        lateinit var user: User
        var parametersBuilder: (ParametersBuilder.() -> Unit)? = null

        suspend fun doSubmit(httpClient: HttpClient) = httpClient.submitWithJwtUser(user, url, parametersBuilder)


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