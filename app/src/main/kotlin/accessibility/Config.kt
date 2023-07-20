package accessibility

import accessibility.JwkProviderBuilder.createJwkProvider
import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

internal suspend fun HttpClient.getOAuthServerConfigurationMetadata(url: String)
        : OauthServerConfigurationMetadata = withContext(Dispatchers.IO) {
    request {
        method = HttpMethod.Get
        url(url)
        accept(ContentType.Application.Json)
    }.body()
}

internal object HttpClientBuilder {
    internal fun build(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation)
            install(HttpTimeout)
        }
    }


}

private fun getAzureEnvVar(varName: String): String {
    return System.getenv(varName)
        ?: throw IllegalArgumentException("Fant ikke $varName for azure. Påse at nais.yaml er konfigurert riktig.")
}

internal object JwkProviderBuilder {
    fun createJwkProvider(metadata: OauthServerConfigurationMetadata): JwkProvider =
        JwkProviderBuilder(URL(metadata.jwksUri))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
}


internal data class OauthServerConfigurationMetadata(
    val issuer: String,
    val tokenEndpoint: String,
    val jwksUri: String,
    var authorizationEndpoint: String = ""
)

internal class RuntimeContext() {

    private val azureClientId: String = getAzureEnvVar("AZURE_APP_CLIENT_ID")
    private val azureWellKnownUrl: String = getAzureEnvVar("AZURE_APP_WELL_KNOWN_URL")

    private val httpClient = HttpClientBuilder.build()
    private val metadata = runBlocking {   httpClient.getOAuthServerConfigurationMetadata(azureWellKnownUrl)}

    private val jwkProvider = createJwkProvider(metadata)

    val verifierWrapper = TokenVerifier(
        jwkProvider = jwkProvider,
        clientId = azureClientId,
        issuer = metadata.issuer
    )
}

internal class TokenVerifier(
    private val jwkProvider: JwkProvider,
    private val clientId: String,
    private val issuer: String
) {

    fun verify(accessToken: String): DecodedJWT {
        return JWT.decode(accessToken).keyId
            .let { kid -> jwkProvider.get(kid) }
            .run { azureAccessTokenVerifier(clientId, issuer) }
            .run { verify(accessToken) }
    }

    private fun Jwk.azureAccessTokenVerifier(clientId: String, issuer: String): JWTVerifier =
        JWT.require(this.RSA256())
            .withAudience(clientId)
            .withIssuer(issuer)
            .build()

    private fun Jwk.RSA256() = Algorithm.RSA256(publicKey as RSAPublicKey, null)
}

internal fun AuthenticationConfig.azureAccessToken(authenticatorName: String?, verifier: TokenVerifier) {
    register(AccessTokenAuthenticationProvider.build(verifier, authenticatorName))
}

private class AccessTokenAuthenticationProvider constructor(
    val verifier: TokenVerifier,
    config: Config
) : AuthenticationProvider(config) {

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val accessToken = context.call.bearerToken
        if (accessToken != null) {
            try {
                val decodedJWT = verifier.verify(accessToken)
                context.principal(AzurePrincipal(decodedJWT))
            } catch (e: Exception) {
                context.respondUnauthorized("Invalid or expired token.")
            }
        } else {
            context.respondUnauthorized("No bearer token found.")
        }
    }

    class Configuration(name: String?) : AuthenticationProvider.Config(name)
    companion object {
        fun build(verifier: TokenVerifier, name: String?) =
            AccessTokenAuthenticationProvider(verifier, Configuration(name))
    }
}

data class AzurePrincipal(val decodedJWT: DecodedJWT) : Principal

private fun AuthenticationContext.respondUnauthorized(message: String) {
    challenge("JWTAuthKey", AuthenticationFailedCause.InvalidCredentials) { challenge, call ->
        call.respond(HttpStatusCode.Unauthorized, message)
        challenge.complete()
    }
}

private val bearerRegex = "Bearer .+".toRegex()

private val ApplicationCall.bearerToken: String?
    get() {
        return tokenFromAzureHeader()
            ?: tokenFromAuthHeader()
    }

private fun ApplicationCall.tokenFromAzureHeader(): String? {
    return request.headers["azure-authorization"]
        ?.takeIf { bearerRegex.matches(it) }
        ?.let { it.split(" ")[1] }
}

private fun ApplicationCall.tokenFromAuthHeader(): String? {
    return request.headers[HttpHeaders.Authorization]
        ?.takeIf { bearerRegex.matches(it) }
        ?.let { it.split(" ")[1] }
}

