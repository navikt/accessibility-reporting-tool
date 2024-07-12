package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.wcag.OrganizationUnit
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = OrganizationRepository(database)
    private val testUserEmail = Email("test@nav.no")
    private val testUserName = "Tadda Taddasen"
    private val testUserOid = User.Oid(UUID.randomUUID().toString())
    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val testOrg1 = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg1",
        email = "test@nav1.no",
        members = mutableSetOf()
    )


    @BeforeAll
    fun setup() {
        database.update {
            cretaeOrgunitInsertQuery(testOrg)

        }
        database.update {
            cretaeOrgunitInsertQuery(testOrg1)
        }
        testOrg1.addMember(testUserEmail)
        database.update {
            cretaeOrgunitInsertQuery(testOrg1)
        }

    }

    fun cretaeOrgunitInsertQuery(organizationUnit: OrganizationUnit) =
        queryOf(
            """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name,:email) on conflict (organization_unit_id) do update set member=:members , email=:email
                """.trimMargin(),
            mapOf(
                "id" to organizationUnit.id,
                "name" to organizationUnit.name,
                "email" to organizationUnit.email,
                "members" to organizationUnit.members.toStringList()
            )
        )


    @Test
    fun getOrganizationForUser() {
        val result = repository.getOrganizationForUser(testUserEmail)
        Assertions.assertEquals(2, result.size)
        assert(result.contains("DummyOrg"))
        assert(result.contains("DummyOrg1"))
    }
}