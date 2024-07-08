package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import accessibility.reporting.tool.wcag.OrganizationUnit
import assert
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)



class CreateReportTest {

    val objectmapper = jacksonObjectMapper()
    private val db = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(db)

    @Test
    fun `Create a new report `() = testApplication {
        application {
            api(
                repository = repository,
                corsAllowedOrigins = "*",
                corsAllowedSchemes = listOf("http", "https")
            ) { mockEmptyAuth() }

        }
        val response = client.post("api/reports/new") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                "name": "report 1",
                "urlTilSiden": "abc@gmail.no",
                "team": "team uu"
                }
                
            """.trimMargin()
            )
        }
        response.status shouldBe HttpStatusCode.OK
        objectmapper.readTree(response.bodyAsText())["id"].asText() shouldNotBe null

    }


    private fun Application.mockEmptyAuth() = authentication {
        jwt {
            skipWhen { true }
        }
    }
}