package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User.Oid
import accessibility.reporting.tool.wcag.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.queryOf
import org.postgresql.util.PGobject
import java.lang.StringBuilder


class ReportRepository(database: Database) : BaseRepository(database) {
    val objectmapper = jacksonObjectMapper()

    fun upsertReport(report: Report) = upsertReportReturning<Report>(report)
    inline fun <reified T : ReportContent> getReport(reportId: String): T? =
        database.query {
            queryOf(
                "select created, last_changed, report_data ->> 'version' as version, report_data from report where report_id=:reportid",
                mapOf("reportid" to reportId)
            ).map { row -> report<T>(row) }.asSingle
        }
    inline fun <reified T : ReportContent> getReportsForUser(oid: Oid): List<T> = database.list {
        queryOf(
            """select created, last_changed, report_data ->> 'version' as version, report_data from report
                | where report_data -> 'user'->>'oid'=:oid
                |  OR report_data -> 'author'->>'oid'=:oid""".trimMargin(),
            mapOf(
                "oid" to oid.str()
            )
        ).map { row -> report<T>(row) }.asList
    }
    inline fun <reified T> getReports(type: ReportType? = null, ids: List<String>? = null): List<T> =
        try {
            database.list {
                queryOf(
                    StringBuilder("select created, last_changed,report_data ->> 'version' as version, report_data from report")
                        .apply {
                            if (type != null)
                                append(" where report_data ->> 'reportType' = :type")
                            if (ids != null)
                                append(" where report_id IN ${ids.sqlList()}")
                        }
                        .toString(),
                    mapOf(
                        "type" to type?.name
                    )
                ).map { row -> report<T>(row) }.asList
            }
        } catch (e: Exception) {
            log.error {
                StringBuilder("select created, last_changed,report_data ->> 'version' as version, report_data from report")
                    .apply {
                        if (type != null)
                            append(" where report_data ->> 'reportType' = :type")
                        if (ids != null)
                            append(" where report_data ->> 'reportid' IN ${ids.joinToString(",", "(", ")")}")
                    }
                    .toString()
            }
            throw e
        }
    fun deleteReport(reportId: String) =
        database.update {
            queryOf(
                """
                     delete from report where report_id=:id 
                    """.trimMargin(),
                mapOf(
                    "id" to reportId
                )
            )
        }
}

fun List<String>.sqlList(): String = joinToString(",", "(", ")") { "'$it'" }

fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

