package accessibility.reporting.tool.database

import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Version
import kotliquery.queryOf
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.time.ZoneId

class ReportRepository(val database: Database) {

    fun upsertReport(report: Report) {
        //TODO: changelog
        database.update {
            queryOf(
                """INSERT INTO report (report_id,organization_unit_id,report_data) 
                    VALUES (:id,:org, :data) ON CONFLICT (report_id) DO UPDATE SET organization_unit_id=:org, report_data=:data
                """.trimMargin(),
                mapOf(
                    "id" to report.reportId,
                    "org" to report.organizationUnit.id,
                    "data" to report.toJson().jsonB()
                )
            )
        }
    }

    private fun insertOrganizationUnit(): String {
        TODO("Not yet implemented")
    }

    fun getReport(reportId: String): Report? =
        database.query {
            queryOf(
                "select report_data ->> 'version' as version, report_data from report where report_id=:reportid",
                mapOf("reportid" to reportId)
            ).map { row ->
                Version.valueOf(row.string("version")).deserialize(row.string("report_data"))
            }.asSingle
        }

}

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}