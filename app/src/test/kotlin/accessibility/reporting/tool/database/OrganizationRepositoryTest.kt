package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.dummyReportV2
import accessibility.reporting.tool.toEmail
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.util.*
import kotlin.collections.List

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationRepositoryTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = OrganizationRepository(database)
    private val testUserEmail = Email("test@nav.no")
    private val testOrg = OrganizationUnit(
        id = "dummy-org",
        name = "DummyOrg",
        email = "test@nav.no",
    )
    private val testOrg1 = OrganizationUnit(
        id = "Dummy-with-member",
        name = "DummyOrg1",
        email = "test@nav1.no",
    ).apply { addMember(testUserEmail) }


    @BeforeEach
    fun setup() {
        database.update { queryOf("delete from organization_unit") }
        database.update { createOrganizationUnitQuery(testOrg) }
        database.update { createOrganizationUnitQuery(testOrg1) }
    }

    @Test
    fun `Get all organization units`() {
        val testOrg3 = OrganizationUnit(
            id = "Dummy-with-many-members",
            name = "DummyOrg3",
            email = "test@nav1.no",
            members = mutableSetOf("email@1.nav", "email@2.nav", "email@3.nav")
        )
        database.update { createOrganizationUnitQuery(testOrg3) }

        repository.getAllOrganizationUnits().assert {
            size shouldBe 3
            this shouldContainOrganizationUnit testOrg1
            this shouldContainOrganizationUnit testOrg
            this shouldContainOrganizationUnit testOrg3
        }
    }

    @Test
    fun `alter org units`() {
        val testOrg2 = OrganizationUnit(
            "some-other-two",
            "Child unit",
            "jaha@nav.no",
        )
        val testOrg3 =
            OrganizationUnit("some-id-thats-this", "Grandchild unit", "something@nav.no")

        repository.upsertOrganizationUnit(testOrg1)
        repository.upsertReportReturning<Report>(dummyReportV2(orgUnit = testOrg1))
        testOrg1.addMember("testMember@test.ko".toEmail())
        repository.upsertOrganizationUnit(testOrg1)


        repository.getReportForOrganizationUnit<Report>(testOrg1.id).apply {
            organizationUnit.assert {
                require(this != null)
                members.size shouldBe 2
            }
            reports.size shouldBe 1
            reports.first().assert {
                require(organizationUnit != null)
                organizationUnit!!.id shouldBe testOrg1.id
                organizationUnit!!.members.size shouldBe 2
            }
        }


        repository.upsertOrganizationUnit(testOrg2)
        repository.upsertOrganizationUnit(testOrg3)

        repository.getAllOrganizationUnits().assert {
            map { it.name } shouldContainExactlyInAnyOrder listOf(
                testOrg.name,
                testOrg1.name,
                testOrg2.name,
                testOrg3.name,
            )
        }
        repository.deleteOrgUnit(orgUnitId = testOrg1.id).size shouldBe 3
    }

    @Test
    fun `changes owner of orgunit`() {
        val testOrg1 = OrganizationUnit("some-id", "Some unit", "tadda@nav.no")
        repository.upsertOrganizationUnit(testOrg1)
        repository.upsertOrganizationUnit(testOrg1.copy(email = "newowner@nav.no"))
        repository.getOrganizationUnit(testOrg1.id)!!.assert {
            id shouldBe testOrg1.id
            name shouldBe testOrg1.name
            email shouldBe "newowner@nav.no"
        }
    }

    @Test
    fun `get reports for unit`() {
        repository.upsertReportReturning<Report>(dummyReportV2(orgUnit = testOrg))
        repository.upsertReportReturning<Report>(dummyReportV2("http://dummyurl2.test", testOrg))
        repository.upsertReportReturning<Report>(dummyReportV2("http://dummyurl3.test", testOrg))
        repository.upsertReportReturning<Report>(dummyReportV2("http://dummyurl4.test", testOrg))

        repository.getReportForOrganizationUnit<Report>(testOrg.id).assert {
            require(first != null)
            second.assert {
                size shouldBe 4
                withClue("Report with url dummyUrl.test is missing") {
                    any { it.url == "http://dummyurl.test" } shouldBe true
                }
                withClue("Report with url dummyurl2.test is missing") {
                    any { it.url == "http://dummyurl2.test" } shouldBe true
                }
                withClue("Report with url dummyurl3.test is missing") {
                    any { it.url == "http://dummyurl3.test" } shouldBe true
                }
                withClue("Report with url dummyurl4.test is missing") {
                    any { it.url == "http://dummyurl4.test" } shouldBe true
                }
            }
        }
    }

    @Test
    fun getOrganizationForUser() {
        val result = repository.getOrganizationForUser(testUserEmail)
        Assertions.assertEquals(2, result.size)
        assert(result.names.contains("DummyOrg"))
        assert(result.names.contains("DummyOrg1"))
    }

    private fun createOrganizationUnitQuery(organizationUnit: OrganizationUnit) =
        queryOf(
            """INSERT INTO organization_unit (organization_unit_id, name, email,member) 
                    VALUES (:id,:name,:email,:members)
                """.trimMargin(),
            mapOf(
                "id" to organizationUnit.id,
                "name" to organizationUnit.name,
                "email" to organizationUnit.email,
                "members" to organizationUnit.members.toStringList()
            )
        )

    @Test
    fun `finner organisasjonseneter uavhengig av upper eller lower case`() {

        val brukerLowerCase = Email("test.testtjoho@nav.no")
        val brukerCamelCase = Email("Test.Testjaha@nav.no")
        val brukerUtenEnhet = Email("Test.guest@nav.no")

        val testOrg2 = OrganizationUnit(
            id = UUID.randomUUID().toString(),
            name = "Some org thats 2",
            email = "test2@nav.no",
            members = mutableSetOf(brukerLowerCase.str())
        )
        val testOrg3 = OrganizationUnit(
            id = UUID.randomUUID().toString(),
            name = "Some org thats 3",
            email = "test3@nav.no",
            members = mutableSetOf(brukerLowerCase.str(), brukerCamelCase.str())
        )
        database.update {
            createOrganizationUnitQuery(testOrg2)
        }
        database.update {
            createOrganizationUnitQuery(testOrg3)
        }
        val resultLower = repository.getOrganizationForUser(brukerLowerCase)
        Assertions.assertEquals(2, resultLower.size)
        resultLower.names shouldContain testOrg2.name
        resultLower.names shouldContain testOrg3.name

        val resultUpper = repository.getOrganizationForUser(brukerCamelCase)
        Assertions.assertEquals(1, resultUpper.size)
        resultLower.names shouldContain testOrg3.name

        repository.getOrganizationForUser(brukerUtenEnhet).size shouldBe 0
    }
}

private infix fun List<OrganizationUnit>.shouldContainOrganizationUnit(orgUnit: OrganizationUnit) {
    this.find { it.id == orgUnit.id }.assert {
        require(this != null) { "${orgUnit.name} was not found" }
        name shouldBe orgUnit.name
        id shouldBe orgUnit.id
        email shouldBe orgUnit.email
        members.size shouldBe orgUnit.members.size
    }
}

private val List<OrganizationUnit>.names
    get() = this.map { it.name }