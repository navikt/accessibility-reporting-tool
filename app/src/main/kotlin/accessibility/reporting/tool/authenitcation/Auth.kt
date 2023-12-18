package accessibility.reporting.tool.authenitcation


import accessibility.reporting.tool.database.Admins
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
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
import java.net.URL
import java.util.concurrent.TimeUnit

val ApplicationCall.user: User
    get() = principal<User>() ?: throw java.lang.IllegalArgumentException("Princial ikke satt")

val ApplicationCall.adminUser: User
    get() = user.also { if (!user.groups.contains("ADMIN ENV")) throw NotAdminException() }

fun Application.installAuthentication(azureAuthContext: AzureAuthContext) {

    authentication {
        jwt {
            verifier(jwkProvider = azureAuthContext.jwkProvider) {
                withIssuer(azureAuthContext.issuer)
                withAudience(azureAuthContext.azureClientId)
            }

            validate { jwtCredential ->
                User(
                    name = jwtCredential.payload.getClaim("name")?.asString(),
                    // V- This here is a bug, preferred_username, upn and or email are
                    email = jwtCredential.payload.getClaim("preferred_username")?.asString() ?: jwtCredential.payload.getClaim("oid").asString()!!,
                    oid = jwtCredential.payload.getClaim("oid").asString()!!,
                    groups = jwtCredential.payload.getClaim("groups").asList(String::class.java),
                )
            }

            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}

data class User(val email: String, val name: String?, val oid: String?, val groups: List<String>) : Principal {

    override fun equals(other: Any?): Boolean {
        if (other is User) {
            return other.email == this.oid || other.oid == this.oid || this.email == other.oid
        }
        return super.equals(other)
    }

    companion object {
        fun fromJson(jsonNode: JsonNode?): User? = jsonNode?.takeIf { !it.isNull }?.let { node ->
            User(
                email = node["email"].asText(),
                name = node["name"].asText(),
                oid = node["oid"]?.takeIf { !it.isNull }?.asText(),
                groups = node["groups"].map({ it.asText()})
            )
        }
    }
}

class AzureAuthContext {
    var issuer: String = System.getenv("AZURE_OPENID_CONFIG_ISSUER") ?: ""
    val azureClientId: String = getAzureEnvVar("AZURE_APP_CLIENT_ID")
    private val azureWellKnownUrl: String = getAzureEnvVar("AZURE_APP_WELL_KNOWN_URL")

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

internal class NotAdminException : Exception()