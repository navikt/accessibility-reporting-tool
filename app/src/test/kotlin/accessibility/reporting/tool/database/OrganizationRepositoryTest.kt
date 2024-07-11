package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.wcag.OrganizationUnit
import groovy.test.GroovyTestCase.assertEquals
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationRepositoryTest {
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = OrganizationRepository(database)
    private val testUserEmail = Email("tadda@test.tadda")
    private val testUserName = "Tadda Taddasen"
    private val testUserOid = User.Oid(UUID.randomUUID().toString())

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name, :email) 
                """.trimMargin(),
                mapOf(
                    "id" to testOrg.id,
                    "name" to testOrg.name,
                    "email" to testOrg.email
                )
            )
        }
    }

    @BeforeEach
    fun cleanDb() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }

    }

    @Test
    fun getOrganizationForUser() {
        val result = repository.getOrganizationForUser(Email("test@example.com"))
        Assertions.assertEquals(1, result.size)
        assert(result.contains("DummyOrg"))

    }

}