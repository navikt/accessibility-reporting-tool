package accessibility.reporting.tool.database

import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportType
import accessibility.reporting.tool.wcag.Version
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotliquery.Row
import kotliquery.queryOf
import org.postgresql.util.PGobject
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class ReportRepository(val database: Database) {

    fun upsertReport(report: Report): Report {
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
                Pair(report(row), row.stringOrNull("old_data"))
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

    fun getReport(reportId: String): Report? =
        database.query {
            queryOf(
                "select created, last_changed, report_data ->> 'version' as version, report_data from report where report_id=:reportid",
                mapOf("reportid" to reportId)
            ).map { row -> report(row) }.asSingle
        }

    fun getReportForOrganizationUnit(id: String): Pair<OrganizationUnit?, List<Report>> =
        database.query {
            queryOf(
                """select * from organization_unit where organization_unit_id=:id""",
                mapOf("id" to id)
            ).map { row -> organizationUnit(row) }.asSingle
        }.let { orgUnit ->
            val reports = if (orgUnit == null) emptyList() else database.list {
                queryOf(
                    """select created, last_changed,report_data ->> 'version' as version, report_data 
                    |from report
                    |where report_data -> 'organizationUnit' ->> 'id' = :id """.trimMargin(),
                    mapOf("id" to id)
                ).map { row -> report(row) }.asList
            }

            Pair(orgUnit, reports)
        }

    fun getReportsForUser(oid: String): List<Report> = database.list {
        //tmp fiks
        queryOf(
            """select created, last_changed, report_data ->> 'version' as version, report_data from report
                | where report_data -> 'user'->>'email'=:oid OR report_data -> 'user'->>'oid'=:oid""".trimMargin(),
            mapOf(
                "oid" to oid
            )
        ).map { row -> report(row) }.asList
    }

    fun getReports(type: ReportType? = null): List<Report> =
        database.list {
            queryOf(
                StringBuilder("select created, last_changed,report_data ->> 'version' as version, report_data from report")
                    .apply {
                        if (type != null)
                            append(" where report_data ->> 'reportType' = :type")
                    }
                    .toString(),
                mapOf("type" to type?.name)
            ).map { row -> report(row) }.asList
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

    fun insertOrganizationUnit(organizationUnit: OrganizationUnit) {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name,:email) on conflict do nothing 
                """.trimMargin(),
                mapOf(
                    "id" to organizationUnit.id,
                    "name" to organizationUnit.name,
                    "email" to organizationUnit.email
                )
            )
        }
    }

    fun getOrganizationUnit(id: String): OrganizationUnit? = database.query {
        queryOf("select * from organization_unit where organization_unit_id=:id", mapOf("id" to id)).map { row ->
            organizationUnit(row)
        }.asSingle
    }

    fun getAllOrganizationUnits(): List<OrganizationUnit> = database.list {
        queryOf("select * from organization_unit")
            .map { row ->
                organizationUnit(row)
            }
            .asList
    }

    private fun report(row: Row) = Version.valueOf(row.string("version"))
        .deserialize(jacksonObjectMapper().readTree(row.string("report_data")))

    private fun organizationUnit(row: Row) = OrganizationUnit(
        id = row.string("organization_unit_id"),
        name = row.string("name"),
        email = row.string("email")
    )

}

object LocalDateTimeHelper {

    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
    fun JsonNode.toLocalDateTime(): LocalDateTime? =
        toList()
            .map { it.asText() }
            .let {
                "${it.year}.${it.month}.${it.day} ${it.hour}:${it.minutes}:${it.seconds}".trim()
            }
            .let {
                if (it.isNotBlank()) LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss"))
                else null
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

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

