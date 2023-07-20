package accessibility.reporting.tool.database

import accessibility.reporting.tool.wcag.OrganizationUnit
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

    fun insertOrganizationUnit(organizationUnit: OrganizationUnit) {
        database.update {
            queryOf(
                """INSERT INTO organization_unit (organization_unit_id, name, email) 
                    VALUES (:id,:name, :email) 
                """.trimMargin(),
                mapOf(
                    "id" to organizationUnit.id,
                    "name" to organizationUnit.name,
                    "email" to organizationUnit.email
                )
            )
        }
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

    fun getReportForOrganizationUnit(id: String): Pair<OrganizationUnit?, List<Report>> =
        database.query {
            queryOf(
                """select * from organization_unit where organization_unit_id=:id""",
                mapOf("id" to id)
            ).map { row ->
                OrganizationUnit(
                    id = row.string("organization_unit_id"),
                    name = row.string("name"),
                    parent = null,
                    email = row.string("email")
                )

            }.asSingle
        }.let { orgUnit ->
            val reports = if (orgUnit == null) emptyList() else database.list {
                queryOf(
                    """select report_data ->> 'version' as version, report_data 
                    |from report
                    |where report_data -> 'organizationUnit' ->> 'id' = :id """.trimMargin(),
                    mapOf("id" to id)
                ).map { row ->
                    Version.valueOf(row.string("version")).deserialize(row.string("report_data"))
                }.asList
            }

            Pair(orgUnit, reports)
        }

    fun getAllOrganizationUnits(): List<OrganizationUnit> = database.list {
        queryOf("select * from organization_unit")
            .map { row ->
                OrganizationUnit(
                    id = row.string("organization_unit_id"),
                    name = row.string("name"),
                    parent = null,
                    email = row.string("email")
                )
            }
            .asList
    }
}

object LocalDateTimeHelper {
    fun nowAtUtc(): LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
}

private fun String.jsonB() = PGobject().apply {
    type = "jsonb"
    value = this@jsonB
}

