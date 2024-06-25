package accessibility.reporting.tool

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.LocalDateTimeHelper
import accessibility.reporting.tool.wcag.*
import java.util.*

val defaultUserEmail = User.Email("tadda@test.tadda")
const val defaultUserName = "Tadda Taddasen"
val defaultUserOid = User.Oid(UUID.randomUUID().toString())
fun dummyReportV2(
    url: String = "http://dummyurl.test",
    orgUnit: OrganizationUnit? = null,
    user: User = User(email = defaultUserEmail, name = defaultUserName, oid = defaultUserOid, groups = listOf()),
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

fun dummyAggregatedReportV2(
    orgUnit: OrganizationUnit? = null,
) =
    AggregatedReport(
        url = "https://aggregated.test",
        descriptiveName = "Aggregated dummy report",
        user = User(email = defaultUserEmail, name = defaultUserName, oid = defaultUserOid, groups = listOf()),
        organizationUnit = orgUnit,
        reports = listOf(
            dummyReportV2(),
            dummyReportV2(orgUnit = OrganizationUnit("something", "something", "something"))
        )
    )
