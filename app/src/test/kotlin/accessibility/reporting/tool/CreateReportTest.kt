package accessibility.reporting.tool

import accessibility.reporting.tool.database.ReportRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
}