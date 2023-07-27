package accessibility.reporting.tool.database

import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.Version
import kotliquery.Row
import kotliquery.queryOf
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.time.ZoneId

class ReportRepository(val database: Database) {

    fun upsertReport(report: Report): Report {
        //TODO: changelog
        database.update {
            queryOf(
                """insert into report (report_id,report_data) 
                    values (:id, :data) on conflict (report_id) do update set report_data=:data

                """.trimMargin(),
                mapOf(
                    "id" to report.reportId,
                    "data" to report.toJson().jsonB()
                )
            )

        }
        // if we cant put-get we're screwed anyway
        return getReport(reportId = report.reportId)!!
    }

    fun getReport(reportId: String): Report? =
        database.query {
            queryOf(
                "select report_data ->> 'version' as version, report_data from report where report_id=:reportid",
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
                    """select report_data ->> 'version' as version, report_data 
                    |from report
                    |where report_data -> 'organizationUnit' ->> 'id' = :id """.trimMargin(),
                    mapOf("id" to id)
                ).map { row -> report(row) }.asList
            }

            Pair(orgUnit, reports)
        }

    fun getReportsForUser(userEmail: String): List<Report> = database.list {
        queryOf(
            "select report_data ->> 'version' as version, report_data from report where report_data -> 'user'->>'email'=:email ",
            mapOf("email" to userEmail)
        ).map { row -> report(row) }.asList
    }

    fun getReports(): List<Report> =
        database.list {
            queryOf(
                "select report_data ->> 'version' as version, report_data from report"
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

    private fun report(row: Row) = Version.valueOf(row.string("version")).deserialize(row.string("report_data"))
    private fun organizationUnit(row: Row) = OrganizationUnit(
        id = row.string("organization_unit_id"),
        name = row.string("name"),
        email = row.string("email")
    )

}

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

