package accessibility.reporting.tool.database

import LocalPostgresDatabase
import accessibility.reporting.tool.TestUser
import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.datestr
import accessibility.reporting.tool.objectmapper
import accessibility.reporting.tool.rest.ReportListItem
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.report.*
import assert
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import org.junit.jupiter.api.*
import java.time.LocalDateTime
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

        reportRepository.getReports<Report>(
            ids = listOf(
                aggregatedTestReport.reportId,
                testReport.reportId
            )
        ).size shouldBe 2
        reportRepository.getReports<AggregatedReport>(ids = listOf(aggregatedTestReport.reportId)).size shouldBe 1
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

    @Test
    fun `can dezerialize lastChanged from legacy report versions`() {
        val legacyReport = dummyAggregatedReportV2()
        val withoutLastChanged = legacyReport.removeJsonFields("lastChanged").toString()

        database.insertLegacyReport(
            legacyReport,
            withoutLastChanged
        )
        reportRepository.getReport<AggregatedReport>(legacyReport.reportId).assert {
            require(this != null)
            "yyyy-MM-dd".datestr(lastChanged) shouldBe "yyyy-MM-dd".datestr(LocalDateTime.now())
        }

        assertSerializationIsOK(legacyReport.reportId)
    }

    @Test
    fun `can deserialize author from legacyfield user`() {
        val user = TestUser(email = "Curtiss@nav.test", name = "Mariela").original
        val dummyReport = dummyReportV2(user = user)
        val jsonWithVersion1UserEmail = dummyReport.removeJsonFields("author", "version").apply {
            this as ObjectNode
            put("version","V1")
            val userNode = putObject("user")
            userNode.put("email", user.email.str())

        }
        database.insertLegacyReport(dummyReport, jsonWithVersion1UserEmail.toString())
        assertSerializationIsOK(dummyReport.reportId){ result ->
            result.author.email shouldBe  user.email.str()
        }

        val dummyReport2 = dummyReportV2(user = user)
        val jsonWithVersion1User = dummyReport2.removeJsonFields("author").apply {
            this as ObjectNode
            val userNode = putObject("user")
            put("version","V1")
            userNode.put("email", user.email.str())
            userNode.put("oid", user.oid.str())
        }
        database.insertLegacyReport(dummyReport2, jsonWithVersion1User.toString())
        assertSerializationIsOK(dummyReport2.reportId){ result ->
            result.author.email shouldBe user.email.str()
            result.author.oid shouldBe user.oid.str()
        }

    }

    private fun assertSerializationIsOK(
        reportId: String,
        isAggregated: Boolean = false,
        assertValues: (Report) -> Unit = {}
    ) {
        if (isAggregated) {
            reportRepository.getReport<AggregatedReport>(reportId).assert {
                this shouldNotBe null
            }
        }
        reportRepository.getReport<Report>(reportId).assert {
            require(this != null)
            assertValues(this)
        }
        reportRepository.getReport<ReportShortSummary>(reportId).assert {
            this shouldNotBe null
        }
        reportRepository.getReport<ReportListItem>(reportId).assert {
            this shouldNotBe null
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
        author = user.toAuthor(),
        successCriteria = Version.V2.criteria,
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

private fun Report.removeJsonFields(vararg fields: String): JsonNode =
    toJson().let {
        objectmapper.readTree(it).apply {
            this as ObjectNode
            fields.forEach { jsonFieldName ->
                remove(jsonFieldName)
            }
        }
    }


private fun Database.insertLegacyReport(report: Report, legacyJsonNode: String) =
    update {
        queryOf(
            """insert into report (report_id,report_data,created, last_changed) 
                    values (:id, :data, :created, :lastChanged) 
                """.trimMargin(),
            mapOf(
                "id" to report.reportId,
                "data" to legacyJsonNode.jsonB(),
                "created" to report.created,
                "lastChanged" to LocalDateTimeHelper.nowAtUtc()
            )
        )

    }

