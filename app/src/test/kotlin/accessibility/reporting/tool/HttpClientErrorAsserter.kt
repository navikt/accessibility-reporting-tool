package accessibility.reporting.tool

import accessibility.reporting.tool.html.submitWithJwtUser
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.http.*

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