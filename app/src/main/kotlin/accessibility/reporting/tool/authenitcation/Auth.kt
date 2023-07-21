package accessibility.reporting.tool.authenitcation


import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.Payload
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.TimeUnit


fun Application.installAuthentication(azureAuthContext: AzureAuthContext) {
    /*
    TODO
    * https://doc.nais.io/security/auth/azure-ad/access-policy/#users
    *sidecar:
      enabled: true
      autoLogin: true

    sjekke om profile er en del av default scope
    * */


    authentication {

        jwt {

            verifier(jwkProvider = azureAuthContext.jwkProvider) {
                withIssuer(azureAuthContext.issuer)
                withAudience(azureAuthContext.azureClientId)
            }

            validate { jwtCredential -> User(jwtCredential.payload) }
        }
    }
}

data class User(val jwtPayload: Payload) : Principal {
    val username = jwtPayload.getClaim("oid") ?: "local-user" //TODO: legg til oid i docker-wonderwall token? går det?
}

class AzureAuthContext() {
    val issuer: String = ""
    val azureClientId: String = getAzureEnvVar("AZURE_APP_CLIENT_ID")
    val azureWellKnownUrl: String = getAzureEnvVar("AZURE_APP_WELL_KNOWN_URL")

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation)
        install(HttpTimeout)
    }

    private val metadata = runBlocking { httpClient.getOAuthServerConfigurationMetadata(azureWellKnownUrl) }

    val jwkProvider = JwkProviderBuilder(URL(metadata.jwksUri))
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
    val tokenEndpoint: String,
    val jwksUri: String,
    var authorizationEndpoint: String = ""
)

