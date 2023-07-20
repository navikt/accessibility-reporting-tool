package accessibility.reporting.tool.database

import LocalPostgresDatabase
import accessibility.reporting.tool.wcag.Deviation
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.ReportV1
import accessibility.reporting.tool.wcag.Version
import assert
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryTest {

    private val testOrg = OrganizationUnit(id = UUID.randomUUID().toString(), name = "DummyOrg", parent = null)
    private val database = LocalPostgresDatabase.cleanDb()
    private val repository = ReportRepository(database)

    @BeforeAll
    fun setup() {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name) 
                    VALUES (:id,:name) 
                """.trimMargin(),
                mapOf(
                    "id" to testOrg.id,
                    "name" to testOrg.name,
                )
            )
        }
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
        testReport.successCriteria.first().deviations.add(Deviation(LocalDateTimeHelper.nowAtUtc(),"some error"))
        repository.upsertReport(testReport)
        repository.getReport(testReport.reportId).assert {
            require(this!=null)
            this.successCriteria.first().deviations.size shouldBe 1
        }
    }

    @Test
    fun `insert org units`() {
        repository.insertOrganizationUnit(OrganizationUnit("some-id", "Some unit"))

        database.query {
            queryOf(
                "select * from organization_unit where organization_unit_id=:id",
                mapOf("id" to "some-id")
            ).map { row ->
                OrganizationUnit(id = row.string("organization_unit_id"), name = row.string("name"))
            }.asSingle

        }.assert {
            require(this!=null)
            name shouldBe "Some unit"
        }
    }

    private fun dummyReportV1() = ReportV1.createEmpty(
        url = "http://dummyurl.test",
        organizationUnit = testOrg,
        testUrl = null,
        testpersonIdent = null
    )

}

