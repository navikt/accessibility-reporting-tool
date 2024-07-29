package accessibility.reporting.tool.database

import accessibility.reporting.tool.rest.ReportListItem
import accessibility.reporting.tool.wcag.report.AggregatedReport
import accessibility.reporting.tool.wcag.report.PersistableReport
import accessibility.reporting.tool.wcag.report.ReportShortSummary
import accessibility.reporting.tool.wcag.report.Version
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import kotliquery.*
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction
import java.time.LocalDateTime
import java.time.ZoneId


interface Database {

    val dataSource: HikariDataSource
    fun update(queryBuilder: () -> Query) {
        using(sessionOf(dataSource)) {
            it.run(queryBuilder.invoke().asUpdate)
        }
    }

    fun <T> query(action: () -> NullableResultQueryAction<T>): T? =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

    fun <T> list(action: () -> ListResultQueryAction<T>): List<T> =
        using(sessionOf(dataSource)) {
            it.run(action.invoke())
        }

}

abstract class BaseRepository(val database: Database) {

    val repositoryObjectMapper = jacksonObjectMapper()
    inline fun <reified T : PersistableReport> upsertReportReturning(report: T): T {
        val reports = database.query {
            queryOf(
                """insert into report (report_id,report_data,created, last_changed) 
                    values (:id, :data, :created, :lastChanged) on conflict (report_id) do update 
                    | set report_data=:data, last_changed=:lastChanged
                    | returning created, last_changed, report_data ->> 'version' as version, report_data,(select report_data from report where report_id=:id) as old_data
                """.trimMargin(),
                mapOf(
                    "id" to report.reportId,
                    "data" to report.toJson().jsonB(),
                    "created" to report.created,
                    "lastChanged" to report.lastChanged
                )
            ).map { row ->
                Pair(report<T>(row), row.stringOrNull("old_data"))
            }.asSingle
        }
        val newReport = reports!!.first
        database.update {
            queryOf(
                """insert into changelog (report_id, time,old_data,new_data,user_oid) values (:reportId, :timeOfUpdate,:oldData, :newData, :userOid)""".trimMargin(),
                mapOf(
                    "reportId" to newReport.reportId,
                    "timeOfUpdate" to newReport.lastChanged,
                    "oldData" to reports.second?.jsonB(),
                    "newData" to newReport.toJson().jsonB(),
                    "userOid" to report.lastUpdatedBy?.oid
                )
            )
        }
        return reports.first
    }

    inline fun <reified T> report(row: Row): T {
        val rapportData = repositoryObjectMapper.readTree(row.string("report_data"))
        return when (val name = T::class.simpleName) {
            "AggregatedReport" -> AggregatedReport.deserialize(Version.valueOf(row.string("version")), rapportData)
            "Report" -> Version.valueOf(row.string("version"))
                .deserialize(rapportData)
            "ReportShortSummary" -> ReportShortSummary.fromJson(rapportData)
            "ReportListItem" -> ReportListItem.fromJson(rapportData)
            else -> throw IllegalArgumentException("Kan ikke transformere rapport-data til $name")
        } as T
    }
}

object LocalDateTimeHelper {

    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    fun JsonNode.toLocalDateTimeFromArrayOrNull(): LocalDateTime? =
        toList()
            .let { dateList ->
                if (dateList.any { it.asText() != "" })
                    dateList.localDateTimeFromArray()
                else null
            }

    fun JsonNode.toLocalDateTimeFromArray(): LocalDateTime =
        toList().localDateTimeFromArray()

    private fun List<JsonNode>.localDateTimeFromArray(): LocalDateTime =
        LocalDateTime.of(
            this[0].asInt(),
            this[1].asInt(),
            this[2].asInt(),
            this[3].asInt(),
            (if (this.size < 5) null else this[4])?.asInt() ?: 0,
            (if (this.size < 6) null else this[5])?.asInt() ?: 0
        )
}


