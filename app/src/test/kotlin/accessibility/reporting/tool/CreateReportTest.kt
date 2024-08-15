package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.toStringList
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotliquery.queryOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance


@TestInstance(TestInstance.Lifecycle.PER_CLASS)


class CreateReportTest: TestApi() {

    private val testUser = TestUser(email = "author.tadda@test.nav", name = "Author")
    private val adminUser = TestUser(email = "admin.tadda@test.nav", name = "Admin", groups = listOf("test_admin"))
    private val teamMemberUser = TestUser(email = "member.tadda@test.nav", name = "Member")
    private val noWriteUser = TestUser(email = "nowrite.adda@test.nav", name = "No Write")
    private val testOrg = createTestOrg(
        name = "Hello Reports",
        email = "Russel",
        teamMemberUser.email.str()
    )

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email, member) 
                    VALUES (:id,:name,:email, :members)
                """.trimMargin(),
                mapOf(
                    "id" to testOrg.id,
                    "name" to testOrg.name,
                    "email" to testOrg.email,
                    "members" to testOrg.members.toStringList()
                )
            )
        }
    }


    @Test
    fun `Creates a new report`() = withTestApi{
        val response = client.postWithJwtUser(testUser.original, "api/reports/new") {
            contentType(ContentType.Application.Json)
            setBody(
                """{
                    "name": "Rrrreport",
                    "urlTilSiden": "https://some.page.nav.no",
                    "teamId": "${testOrg.id}"
                    }
                    
                """.trimMargin()
            )
        }
        response.status shouldBe HttpStatusCode.OK
        val id = objectmapper.readTree(response.bodyAsText())["id"].asText()
        id shouldNotBe null
        client.getWithJwtUser(testUser.original, "api/reports/$id").assert {
            status shouldBe HttpStatusCode.OK
            objectmapper.readTree(bodyAsText()).assert {
                this["author"]["email"].asText() shouldBe testUser.original.email.str()
                this["hasWriteAccess"].asBoolean() shouldBe true
            }
        }


        client.run { assertCorrectAccess(id, testUser.capitalized, true) }
        client.run { assertCorrectAccess(id, adminUser.original, true) }
        client.run { assertCorrectAccess(id, adminUser.capitalized, true) }
        client.run { assertCorrectAccess(id, teamMemberUser.original, true) }
        client.run { assertCorrectAccess(id, teamMemberUser.capitalized, true) }
        client.run { assertCorrectAccess(id, noWriteUser.capitalized, false) }
        client.run { assertCorrectAccess(id, noWriteUser.original, false) }
    }
}

private suspend fun HttpClient.assertCorrectAccess(id: String, user: User, expected: Boolean) {
    getWithJwtUser(user, "api/reports/$id").assert {
        status shouldBe HttpStatusCode.OK
        withClue("${user.email.str()} has inncorrect access to report") {
            objectmapper.readTree(bodyAsText())["hasWriteAccess"].asBoolean() shouldBe expected
        }
    }
}
