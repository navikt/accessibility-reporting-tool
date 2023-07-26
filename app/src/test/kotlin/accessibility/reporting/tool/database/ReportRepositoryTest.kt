package accessibility.reporting.tool.database

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.wcag.Deviation
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Version
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryTest {

    private val testOrg = OrganizationUnit(id = UUID.randomUUID().toString(), name = "DummyOrg", email = "test@nav.no")
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)
    private val testUserEmail = "tadda@test.tadda"
    private val testUserName = "Tadda Taddasen"

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
    fun upsertReport() {
        val testReport = dummyReportV1()
        repository.upsertReport(testReport)
        repository.getReport(testReport.reportId).assert {
            require(this != null)
            this.successCriteria.size shouldBe 49
            this.successCriteria.first().deviations.size shouldBe 0
        }
        testReport.successCriteria.first().deviations.add(Deviation(LocalDateTimeHelper.nowAtUtc(), "some error"))
        repository.upsertReport(testReport)
        repository.getReport(testReport.reportId).assert {
            require(this != null)
            this.successCriteria.first().deviations.size shouldBe 1
        }
    }

    @Test
    fun `insert org units`() {
        val testOrg1 = OrganizationUnit("some-id", "Some unit", "tadda@nav.no")
        val childTestOrg = OrganizationUnit("some-other-id", "Child unit", "jaha@nav.no", "shorty")
        val testOrg2 = OrganizationUnit(
            "some-other-two",
            "Child unit",
            "jaha@nav.no",
            "shorty short"
        )
        val grandchildTestOrg =
            OrganizationUnit("some-id-thats-this", "Grandchild unit", "something@nav.no")

        repository.insertOrganizationUnit(testOrg1)
        repository.insertOrganizationUnit(testOrg1)
        repository.insertOrganizationUnit(childTestOrg)
        //Ønsket oppførsel om navn ikke er unikt, kaste expception?

        repository.insertOrganizationUnit(testOrg2)
        repository.insertOrganizationUnit(grandchildTestOrg)

        database.list {
            queryOf(
                "select * from organization_unit"
            ).map { row ->
                OrganizationUnit(
                    id = row.string("organization_unit_id"),
                    name = row.string("name"),
                    email = row.string("email")
                )
            }.asList
        }.assert {
            map { it.name } shouldContainExactlyInAnyOrder listOf(
                testOrg1.name,
                testOrg2.name,
                childTestOrg.name,
                grandchildTestOrg.name,
                testOrg.name
            )
        }
    }


    @Test
    fun `get projects for unit`() {
        repository.upsertReport(dummyReportV1(orgUnit = testOrg))
        repository.upsertReport(dummyReportV1("http://dummyurl2.test", testOrg))
        repository.upsertReport(dummyReportV1("http://dummyurl3.test", testOrg))
        repository.upsertReport(dummyReportV1("http://dummyurl4.test", testOrg))

        repository.getReportForOrganizationUnit(testOrg.id).assert {
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
    fun `get reports for user`() {

        repository.upsertReport(dummyReportV1())
        repository.upsertReport(dummyReportV1("http://dummyurl2.test"))
        repository.upsertReport(dummyReportV1("http://dummyurl3.test"))
        repository.upsertReport(dummyReportV1("http://dummyurl4.test"))

        repository.getReportsForUser(testUserEmail).assert {
            size shouldBe 4 //TODO
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

    private fun dummyReportV1(url: String = "http://dummyurl.test", orgUnit: OrganizationUnit? = null) = Report(
        reportId = UUID.randomUUID().toString(),
        url = url,
        organizationUnit = orgUnit,
        version = Version.V1,
        testData = null,
        user = User(email = testUserEmail, name = testUserName),
        successCriteria = Version.V1.criteria,
        filters = mutableListOf()
    )
}
