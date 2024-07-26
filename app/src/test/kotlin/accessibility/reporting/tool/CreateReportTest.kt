package accessibility.reporting.tool

import assert
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)


class CreateReportTest {

    private val db = LocalPostgresDatabase.cleanDb()
    private val testUser = TestUser(email = "Cierra", name = "Shelli").original
    @Test
    fun `Create a new report `() = setupTestApi(db) {
        //TODO: legg til user på rapport
        val response = client.postWithJwtUser(testUser,"api/reports/new") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                "name": "report 1",
                "urlTilSiden": "abc@gmail.no",
                "teamId": "team-uu"
                }
                
            """.trimMargin()
            )
        }
        response.status shouldBe HttpStatusCode.OK
        objectmapper.readTree(response.bodyAsText())["id"].asText() shouldNotBe null
    }

    @Disabled
    @Test
    fun `debug options response`()= setupTestApi(db){
        client.request("api/reports/new") {
            method = HttpMethod.Options
            headers {
                append(HttpHeaders.Accept, "*/*")
                append(HttpHeaders.AcceptLanguage, "en-GB,en-US;q=0.9,en;q=0.8")
                append(HttpHeaders.CacheControl, "no-cache")
                append(HttpHeaders.Pragma, "no-cache")
                append("priority", "u=1, i")
                append("sec-fetch-dest", "empty")
                append("sec-fetch-mode", "cors")
                append("sec-fetch-site", "same-site")
                append(HttpHeaders.Referrer, "https://a11y-statement-ny.ansatt.dev.nav.no/")
                append("referrer-policy", "strict-origin-when-cross-origin")
            }
        }.assert {
            this.status shouldBe HttpStatusCode.OK
        }
    }
}