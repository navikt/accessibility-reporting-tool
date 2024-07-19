package accessibility.reporting.tool.database

import LocalPostgresDatabase
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.toEmail
import accessibility.reporting.tool.wcag.*
import assert
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportRepositoryTest {

    private val testOrg = OrganizationUnit(
        id = UUID.randomUUID().toString(),
        name = "DummyOrg",
        email = "test@nav.no",
        members = mutableSetOf()
    )
    private val database = LocalPostgresDatabase.cleanDb()
    private val reportRepository = ReportRepository(database)
    private val organizationRepository= OrganizationRepository(database)
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
    fun upsertReport() {
        val testReport = dummyReportV2()
        val aggregatedTestReport = dummyAggregatedReportV2(orgUnit = testOrg)
        reportRepository.upsertReport(testReport)
        reportRepository.getReport<Report>(testReport.reportId).assert {
            require(this != null)
            successCriteria.size shouldBe 49
            reportType shouldBe ReportType.SINGLE
        }
        reportRepository.upsertReport(aggregatedTestReport)
        reportRepository.getReport<AggregatedReport>(aggregatedTestReport.reportId).assert {
            require(this != null)
            successCriteria.size shouldBe 49
            reportType shouldBe ReportType.AGGREGATED
            fromReports.size shouldBe 2
            fromOrganizations.size shouldBe 1

        }

        reportRepository.upsertReport(testReport)
        reportRepository.upsertReport(testReport)
        reportRepository.getReport<Report>(testReport.reportId).assert { require(this != null) }
        database.list {
            queryOf(
                "SELECT * from changelog where report_id=:id",
                mapOf("id" to testReport.reportId)
            ).map { row -> row.string("report_id") }.asList
        }.size shouldBe 3

    }

    @Test
    fun `alter org units`() {
        val testOrg1 = OrganizationUnit("some-id", "Some unit", "tadda@nav.no")
        val childTestOrg = OrganizationUnit("some-other-id", "Child unit", "jaha@nav.no")
        val testOrg2 = OrganizationUnit(
            "some-other-two",
            "Child unit",
            "jaha@nav.no",
        )
        val grandchildTestOrg =
            OrganizationUnit("some-id-thats-this", "Grandchild unit", "something@nav.no")

        reportRepository.upsertOrganizationUnit(testOrg1)
        reportRepository.upsertReport(dummyReportV2(orgUnit = testOrg1))
        testOrg1.addMember("testMember@test.ko".toEmail())
        reportRepository.upsertOrganizationUnit(testOrg1)

        reportRepository.getReportForOrganizationUnit<Report>(testOrg1.id).apply {
            first.assert {
                require(this != null)
                members.size shouldBe 1
            }
            second.size shouldBe 1
            second.first().assert {
                require(organizationUnit != null)
                organizationUnit!!.id shouldBe testOrg1.id
                organizationUnit!!.members.size shouldBe 1
            }
        }

        reportRepository.upsertOrganizationUnit(childTestOrg)
        //Ønsket oppførsel om navn ikke er unikt, kaste expception?

        reportRepository.upsertOrganizationUnit(testOrg2)
        reportRepository.upsertOrganizationUnit(grandchildTestOrg)

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
        reportRepository.deleteOrgUnit(orgUnitId = testOrg1.id).size shouldBe 4
    }

    @Test
    fun `changes owner of orgunit`(){
        val testOrg1 = OrganizationUnit("some-id", "Some unit", "tadda@nav.no")
        reportRepository.upsertOrganizationUnit(testOrg1)
        reportRepository.upsertOrganizationUnit(testOrg1.copy(email = "newowner@nav.no"))
        organizationRepository.getOrganizationUnit(testOrg1.id)!!.assert {
            id shouldBe testOrg1.id
            name shouldBe testOrg1.name
            email shouldBe "newowner@nav.no"
        }
    }

    @Test
    fun `get reports by type`() {
        val testReport = dummyReportV2()
        val aggregatedTestReport = dummyAggregatedReportV2(orgUnit = testOrg)
        reportRepository.upsertReport(testReport)
        reportRepository.upsertReport(aggregatedTestReport)
        reportRepository.upsertReport(dummyAggregatedReportV2(orgUnit = testOrg))

        reportRepository.getReports<Report>().size shouldBe 3
        reportRepository.getReports<Report>(ReportType.SINGLE).size shouldBe 1
        reportRepository.getReports<AggregatedReport>(ReportType.AGGREGATED).size shouldBe 2
        reportRepository.getReports<ReportShortSummary>().size shouldBe 3
    }

    @Test
    fun `get reports by id`() {
        val testReport = dummyReportV2()
        val aggregatedTestReport = dummyAggregatedReportV2(orgUnit = testOrg)
        reportRepository.upsertReport(testReport)
        reportRepository.upsertReport(aggregatedTestReport)
        reportRepository.upsertReport(dummyAggregatedReportV2(orgUnit = testOrg))

        reportRepository.getReports<Report>(ids = listOf(aggregatedTestReport.reportId, testReport.reportId)).size shouldBe 2
        reportRepository.getReports<AggregatedReport>(ids = listOf(aggregatedTestReport.reportId)).size shouldBe 1
    }


    @Test
    fun `get reports for unit`() {
        reportRepository.upsertReport(dummyReportV2(orgUnit = testOrg))
        reportRepository.upsertReport(dummyReportV2("http://dummyurl2.test", testOrg))
        reportRepository.upsertReport(dummyReportV2("http://dummyurl3.test", testOrg))
        reportRepository.upsertReport(dummyReportV2("http://dummyurl4.test", testOrg))

        reportRepository.getReportForOrganizationUnit<Report>(testOrg.id).assert {
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
        val testUser = User(email = Email("randomemail"), name = null, oid = testUserOid, groups = listOf())
        val updatedReport = dummyReportV2(
            url = "http://dummyurl4.test",
            user = User(email = Email("randomemail"), name = null, oid = testUserOid, groups = listOf())
        )

        reportRepository.upsertReport(dummyReportV2(user = testUser, url = "http://dummyx2.test"))
        reportRepository.upsertReport(dummyReportV2(user = testUser, url = "http://dummyurl2.test"))
        reportRepository.upsertReport(dummyReportV2(user = testUser, url = "http://dummyurl3.test"))
        reportRepository.upsertReport(
            updatedReport
        )

        reportRepository.upsertReport(
            updatedReport
        )


        reportRepository.getReportsForUser<Report>(testUserOid).assert {
            size shouldBe 4
            withClue("Report with url http://dummyx2.test is missing") {
                any { it.url == "http://dummyx2.test" } shouldBe true
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

    private fun dummyReportV2(
        url: String = "http://dummyurl.test",
        orgUnit: OrganizationUnit? = null,
        user: User = User(email = testUserEmail, name = testUserName, oid = testUserOid, groups = listOf()),
        reportType: ReportType = ReportType.SINGLE,
        id: String = UUID.randomUUID().toString()
    ) = Report(
        reportId = id,
        url = url,
        organizationUnit = orgUnit,
        version = Version.V2,
        testData = null,
        author = user.toAuthor(),
        successCriteria = Version.V2.criteria,
        filters = mutableListOf(),
        lastChanged = LocalDateTimeHelper.nowAtUtc(),
        created = LocalDateTimeHelper.nowAtUtc(),
        lastUpdatedBy = null,
        descriptiveName = "Dummyname",
        reportType = reportType
    )

    private fun dummyAggregatedReportV2(
        orgUnit: OrganizationUnit? = null,
    ) =
        AggregatedReport(
            url = "https://aggregated.test",
            descriptiveName = "Aggregated dummy report",
            user = User(email = testUserEmail, name = testUserName, oid = testUserOid, groups = listOf()),
            organizationUnit = orgUnit,
            reports = listOf(
                dummyReportV2(),
                dummyReportV2(orgUnit = OrganizationUnit("something", "something", "something"))
            )
        )

}

