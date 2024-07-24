package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.util.*

object JwtConfig {

    private const val secret = "zAP5MBA4B4Ijz0MZaS48"
    const val issuer = "test.issuer"
    private const val validityInMs = 36_000_00 * 1 // 1 hour
    private val algorithm = Algorithm.HMAC512(secret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()
    fun generateToken(
        user: User
    ): String = JWT.create()
        .withSubject("Authentication")
        .withIssuer(issuer)
        .withClaim("oid", user.oid.str())
        .withClaim("preferred_username", user.email.str())
        .withClaim("name", user.name)
        .withArrayClaim("groups", user.groups.toTypedArray())
        .withExpiresAt(getExpiration())
        .sign(algorithm)

    private fun getExpiration() = Date(System.currentTimeMillis() + validityInMs)
}

fun Application.installJwtTestAuth() {
    authentication {
        jwt {
            verifier(JwtConfig.verifier)
            realm = "ktor.io"

            validate { jwtCredential ->
                User(
                    name = jwtCredential.payload.getClaim("name")?.asString(),
                    email = User.Email(jwtCredential.payload.getClaim("preferred_username").asString()),
                    oid = User.Oid(jwtCredential.payload.getClaim("oid").asString()!!),
                    groups = jwtCredential.payload.getClaim("groups").asList(String::class.java),
                )
            }
            challenge { _, _ ->
                println("SKIPPING AUTH CHALLENGE FOR TEST")
            }
        }
    }
}

suspend fun HttpClient.getWithJwtUser(user: User, urlString: String, block: HttpRequestBuilder.() -> Unit = {}) =
    get(urlString) {
        block()
        header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
    }

suspend fun HttpClient.postWithJwtUser(user: User, urlString: String, block: HttpRequestBuilder.() -> Unit = {}) =
    post(urlString) {
        block()
        header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
    }

suspend fun HttpClient.putWithJwtUser(user: User, urlString: String, block: HttpRequestBuilder.() -> Unit = {}) =
    put(urlString) {
        block()
        header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
    }

suspend fun HttpClient.deleteWithJwtUser(user: User, urlString: String, block: HttpRequestBuilder.() -> Unit = {}) =
    delete(urlString) {
        block()
        header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
    }

suspend fun HttpClient.optionsWithJwtUser(user: User, urlString: String, block: HttpRequestBuilder.() -> Unit = {}) =
    options(urlString) {
        block()
        header("Authorization", "Bearer ${JwtConfig.generateToken(user)}")
    }