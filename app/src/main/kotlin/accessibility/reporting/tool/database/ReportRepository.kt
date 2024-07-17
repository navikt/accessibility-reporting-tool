package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.authenitcation.User.Oid
import accessibility.reporting.tool.rest.ReportListItem
import accessibility.reporting.tool.wcag.*
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
    val objectmapper = jacksonObjectMapper()

    fun upsertReport(report: Report) = upsertReportReturning<Report>(report)
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

    inline fun <reified T : ReportContent> getReport(reportId: String): T? =
        database.query {
            queryOf(
                "select created, last_changed, report_data ->> 'version' as version, report_data from report where report_id=:reportid",
                mapOf("reportid" to reportId)
            ).map { row -> report<T>(row) }.asSingle
        }


    inline fun <reified T : ReportContent> getReportForOrganizationUnit(id: String): Pair<OrganizationUnit?, List<T>> =
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
                ).map { row -> report<T>(row) }.asList
            }

            Pair(orgUnit, reports)
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

    fun upsertOrganizationUnit(organizationUnit: OrganizationUnit) {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name,:email) on conflict (organization_unit_id) do update set member=:members , email=:email
                """.trimMargin(),
                mapOf(
                    "id" to organizationUnit.id,
                    "name" to organizationUnit.name,
                    "email" to organizationUnit.email,
                    "members" to organizationUnit.members.toStringList()
                )
            )
        }

        getReportForOrganizationUnit<Report>(organizationUnit.id).second.forEach { report ->
            if (report.reportType == ReportType.SINGLE)
                upsertReport(
                    report.withUpdatedMetadata(
                        organizationUnit = organizationUnit,
                        updateBy = User(
                            email = Email(organizationUnit.email),
                            name = organizationUnit.name,
                            oid = Oid("organizationUnit"),
                            groups = emptyList()
                        )
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

    inline fun <reified T> report(row: Row): T {
        val rapportData = jacksonObjectMapper().readTree(row.string("report_data"))
        return when (val name = T::class.simpleName) {
            "AggregatedReport" -> AggregatedReport.deserialize(Version.valueOf(row.string("version")), rapportData)
            "Report" -> Version.valueOf(row.string("version"))
                .deserialize(rapportData)

            "ReportShortSummary" -> ReportShortSummary.fromJson(rapportData)
            "ReportListItem" -> ReportListItem.fromJson(rapportData)
            else -> throw IllegalArgumentException("Kan ikke transformere rapport-data til $name")
        } as T
    }

    fun organizationUnit(row: Row) = OrganizationUnit(
        id = row.string("organization_unit_id"),
        name = row.string("name"),
        email = row.string("email"),
        members = row.stringOrNull("member")?.split(",")?.toMutableSet() ?: mutableSetOf()
    )

    fun deleteOrgUnit(orgUnitId: String): List<OrganizationUnit> {
        database.update {
            queryOf(
                "delete from organization_unit where organization_unit_id=:id",
                mapOf("id" to orgUnitId)
            )
        }
        return getAllOrganizationUnits()
    }
}

fun MutableSet<String>.toStringList(): String = toList()
    .filter { it.isNotEmpty() }
    .joinToString((","))

fun List<String>.sqlList(): String = joinToString(",", "(", ")") { "'$it'" }

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

fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

