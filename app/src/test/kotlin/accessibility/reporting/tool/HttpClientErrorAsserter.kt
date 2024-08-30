package accessibility.reporting.tool

import accessibility.reporting.tool.html.submitWithJwtUser
import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

abstract class HttpAsserter(val client: HttpClient, val url: String) {
    abstract suspend fun badRequest(block: ErrorRequestConfig.() -> Unit = {})

    companion object {
        suspend fun HttpClient.shouldReturnErrorOnSubmit(url: String, block: suspend SubmitAsserter.() -> Unit) {
            SubmitAsserter(this, url).block()
        }

        suspend fun HttpClient.shouldReturnErrorOnPatch(url: String, block: suspend SubmitAsserter.() -> Unit) {
            SubmitAsserter(this, url).block()
        }
    }
}

abstract class ErrorRequestConfig(val httpClient: HttpClient) {
    lateinit var user: TestUser
    var parametersBuilder: (ParametersBuilder.() -> Unit)? = null

    abstract suspend fun performRequest(url: String, block: HttpRequestBuilder.() -> Unit = {}): HttpResponse
}

class SubmitAsserter(client: HttpClient, url: String) : HttpAsserter(client, url) {
    class SubmitBadRequestConfig(httpClient: HttpClient) : ErrorRequestConfig(httpClient) {
        override suspend fun performRequest(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
            httpClient.submitWithJwtUser(user.original, url, parametersBuilder)
    }

    override suspend fun badRequest(block: ErrorRequestConfig.() -> Unit) {
        val response = SubmitBadRequestConfig(client).apply { block() }.performRequest(url)
        response.status shouldBe HttpStatusCode.BadRequest
    }
}

class PatchAsserter(client: HttpClient, url: String) : HttpAsserter(client, url) {
    class PatchBadRequestConfig(httpClient: HttpClient) : ErrorRequestConfig(httpClient) {
        override suspend fun performRequest(url: String, block: HttpRequestBuilder.() -> Unit): HttpResponse =
            httpClient.patchWithJwtUser(user.original, url) { block() }
    }

    override suspend fun badRequest(block: ErrorRequestConfig.() -> Unit) {
        val response = PatchBadRequestConfig(client).apply { block() }.performRequest(url)
        response.status shouldBe HttpStatusCode.BadRequest
    }
}