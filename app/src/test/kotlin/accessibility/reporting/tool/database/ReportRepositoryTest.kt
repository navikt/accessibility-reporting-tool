package accessibility.reporting.tool.database

import LocalPostgresDatabase
import accessibility.reporting.tool.wcag.Deviation
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportV1
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryTest {

    private val testOrg = OrganizationUnit(id = UUID.randomUUID().toString(), name = "DummyOrg", email = "test@nav.no")
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)

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
    fun `cleanDb`() {
        database.update { queryOf("delete from changelog") }
        database.update { queryOf("delete from report") }

    }

    @Test
    fun upsertReport() {
        val testReport = dummyReportV1()
        repository.upsertReport(testReport)
        repository.getReport(testReport.reportId).assert {
            require(this != null)
            this.successCriteria.size shouldBe 4
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
        repository.insertOrganizationUnit(OrganizationUnit("some-id", "Some unit", null, "tadda@nav.no"))

        database.query {
            queryOf(
                "select * from organization_unit where organization_unit_id=:id",
                mapOf("id" to "some-id")
            ).map { row ->
                OrganizationUnit(
                    id = row.string("organization_unit_id"),
                    name = row.string("name"),
                    email = row.string("email")
                )
            }.asSingle

        }.assert {
            require(this != null)
            name shouldBe "Some unit"
        }
    }

    @Test
    fun `get projects for unit`() {
        repository.upsertReport(dummyReportV1())
        repository.upsertReport(dummyReportV1("http://dummyurl2.test"))
        repository.upsertReport(dummyReportV1("http://dummyurl3.test"))
        repository.upsertReport(dummyReportV1("http://dummyurl4.test"))

        repository.getReportForOrganizationUnit(testOrg.id).assert {
            require(first != null)
            second.assert {
                size shouldBe 4
                withClue("Report with url dummyUrl.test is missing") {
                    any { it.testUrl == "http://dummyurl.test" } shouldBe true
                }
                withClue("Report with url dummyurl2.test is missing") {
                    any { it.testUrl == "http://dummyurl2.test" } shouldBe true
                }
                withClue("Report with url dummyurl3.test is missing") {
                    any { it.testUrl == "http://dummyurl3.test" } shouldBe true
                }
                withClue("Report with url dummyurl4.test is missing") {
                    any { it.testUrl == "http://dummyurl4.test" } shouldBe true
                }
            }

        }
    }

    private fun dummyReportV1(url: String = "http://dummyurl.test") = ReportV1.createEmpty(
        url = url,
        organizationUnit = testOrg,
        testUrl = null,
        testpersonIdent = null
    )

}

