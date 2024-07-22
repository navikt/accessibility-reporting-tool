package accessibility.reporting.tool.database

import accessibility.reporting.tool.rest.ReportListItem
import accessibility.reporting.tool.wcag.AggregatedReport
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportShortSummary
import accessibility.reporting.tool.wcag.Version
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zaxxer.hikari.HikariDataSource
import kotliquery.*
import kotliquery.action.ListResultQueryAction
import kotliquery.action.NullableResultQueryAction
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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
    inline fun <reified T : Report> upsertReportReturning(report: T): T {
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
    fun JsonNode.toLocalDateTimeOrNull(): LocalDateTime? =
        toList()
            .map { it.asText() }
            .let {
                "${it.year}.${it.month}.${it.day} ${it.hour}:${it.minutes}:${it.seconds}".trim()
            }
            .let {
                if (it.isNotBlank()) LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                else null
            }

    fun JsonNode.toLocalDateTime(): LocalDateTime =
        toList()
            .map { it.asText() }
            .let {
                "${it.year}.${it.month}.${it.day} ${it.hour}:${it.minutes}:${it.seconds}".trim()
            }
            .let {
                LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
            }

    private fun String.padWithZero(charCount: Int = 2) = let { "0".repeat(charCount - it.length) + it }
    private val List<String>.year: String
        get() = this[0]
    private val List<String>.month: String
        get() = this[1].padWithZero(2)
    private val List<String>.day: String
        get() = this[2].padWithZero(2)
    private val List<String>.hour: String
        get() = this[3].padWithZero(2)
    private val List<String>.minutes: String
        get() = (if (this.size < 5) "" else this[4]).padWithZero(2)
    private val List<String>.seconds: String
        get() = (if (this.size < 6) "" else this[5]).padWithZero()
}


