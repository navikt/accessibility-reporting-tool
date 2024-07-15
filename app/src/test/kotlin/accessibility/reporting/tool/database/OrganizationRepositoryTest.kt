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


    @Test
    fun `finner organisasjonseneter uavhengig av upper eller lower case`(){

        val bruker1 = Email("test.test@nav.no")
        val bruker2 = Email("Test.Test@nav.no")

        val testOrg2 = OrganizationUnit(
            id = UUID.randomUUID().toString(),
            name = "DummyOrg2",
            email = "test2@nav.no",
            members = mutableSetOf()
        )
        val testOrg3 = OrganizationUnit(
            id = UUID.randomUUID().toString(),
            name = "DummyOrg3",
            email = "test3@nav.no",
            members = mutableSetOf()
        )
        database.update {
            cretaeOrgunitInsertQuery(testOrg2)
        }
        testOrg2.addMember(bruker1)
        database.update {
            cretaeOrgunitInsertQuery(testOrg3)
        }
        testOrg3.addMember(bruker2)

        database.update {
            cretaeOrgunitInsertQuery(testOrg2)
        }
        database.update {
            cretaeOrgunitInsertQuery(testOrg3)
        }
        val resultLower = repository.getOrganizationForUser(bruker1)
        Assertions.assertEquals(2, resultLower.size)
        assert(resultLower.contains("DummyOrg2"))
        assert(resultLower.contains("DummyOrg3"))

        val resultUpper = repository.getOrganizationForUser(bruker2)
        Assertions.assertEquals(2, resultUpper.size)
        assert(resultUpper.contains("DummyOrg2"))
        assert(resultUpper.contains("DummyOrg3"))


    //Legge inn en bruker i en organisasjon som har email test.test@nav.no
        // addMemeber
        //database upsert

        //Legge inn en bruker i en annen organisasjon som har email Test.Test@nav.no
        // addMemeber
        //database upsert


        //repository.getOrganizationFor User(test.test@nav.no)
        //teste på at size er 2
        //repository.getOrganizationFor User(Test.Test@nav.no)
        //teste på at size er 2
    }


}