package accessibility.reporting.tool.authenitcation


import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.css.body
import java.net.URL
import java.util.concurrent.TimeUnit

val ApplicationCall.user: User
    get() = principal<User>() ?: throw java.lang.IllegalArgumentException("Azuread sucks")

fun Application.installAuthentication(azureAuthContext: AzureAuthContext) {

    authentication {
        jwt {
            verifier(jwkProvider = azureAuthContext.jwkProvider) {
                withIssuer(azureAuthContext.issuer)
                withAudience(azureAuthContext.azureClientId)
            }
            validate { jwtCredential ->
                User(
                    name = jwtCredential.payload.getClaim("name").asString(),
                    email = jwtCredential.payload.getClaim("email").asString()
                )
            }

            challenge { defaultScheme, realm ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

data class User(val email: String, val name: String) : Principal



class AzureAuthContext() {
    var issuer: String = ""
    val azureClientId: String = getAzureEnvVar("AZURE_APP_CLIENT_ID")
    val azureWellKnownUrl: String = getAzureEnvVar("AZURE_APP_WELL_KNOWN_URL")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson() {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
        install(HttpTimeout)
    }

    private val metadata = runBlocking {
        httpClient.getOAuthServerConfigurationMetadata(azureWellKnownUrl)
    }.also { issuer = it.issuer }

    val jwkProvider: JwkProvider = JwkProviderBuilder(URL(metadata.jwksUri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    private fun getAzureEnvVar(varName: String): String {
        return System.getenv(varName)
            ?: throw IllegalArgumentException("$varName not present in env")
    }
}

internal suspend fun HttpClient.getOAuthServerConfigurationMetadata(url: String)
        : OauthServerConfigurationMetadata = withContext(Dispatchers.IO) {
    request {
        method = HttpMethod.Get
        url(url)
        accept(ContentType.Application.Json)
    }.body()
}

internal data class OauthServerConfigurationMetadata(
    val issuer: String,
    @JsonProperty("token_endpoint") val tokenEndpoint: String,
    @JsonProperty("jwks_uri") val jwksUri: String,
    @JsonProperty("authorization_endpoint") val authorizationEndpoint: String
)
