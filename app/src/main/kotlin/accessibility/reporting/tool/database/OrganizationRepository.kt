package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.wcag.OrganizationUnit
import accessibility.reporting.tool.wcag.Report
import accessibility.reporting.tool.wcag.ReportContent
import accessibility.reporting.tool.wcag.ReportType
import kotliquery.Row
import kotliquery.queryOf

class OrganizationRepository(database: Database) : BaseRepository(database) {

    fun getOrganizationUnit(id: String) = database.query {
        queryOf(
            """select * from organization_unit where organization_unit_id=:id""",
            mapOf("id" to id)
        ).map { row -> organizationUnit(row) }.asSingle
    }

    fun getOrganizationForUser(email: Email): List<OrganizationUnit> = database.list {
        queryOf(
            """SELECT *
               FROM organization_unit ou
               WHERE LOWER(ou.email) = LOWER(:email) OR LOWER(:email) = ANY(string_to_array(LOWER(ou.member), ','))         

            """,
            mapOf(
                "email" to email.str()
            )
        ).map { row ->
            OrganizationUnit(
                id = row.string("organization_unit_id"),
                name = row.string("name"),
                email = row.string("email")
            )
        }.asList
    }

    inline fun <reified T : ReportContent> getReportForOrganizationUnit(id: String): Pair<OrganizationUnit?, List<T>> =
        getOrganizationUnit(id).let { orgUnit ->
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

    fun getAllOrganizationUnits(): List<OrganizationUnit> = database.list {
        queryOf("select * from organization_unit")
            .map { row ->
                organizationUnit(row)
            }
            .asList
    }

    fun deleteOrgUnit(orgUnitId: String): List<OrganizationUnit> {
        database.update {
            queryOf(
                "delete from organization_unit where organization_unit_id=:id",
                mapOf("id" to orgUnitId)
            )
        }
        return getAllOrganizationUnits()
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
                upsertReportReturning<Report>(
                    report.withUpdatedMetadata(
                        organizationUnit = organizationUnit,
                        updateBy = User(
                            email = Email(organizationUnit.email),
                            name = organizationUnit.name,
                            oid = User.Oid("organizationUnit"),
                            groups = emptyList()
                        )
                    )
                )
        }
    }

    fun organizationUnit(row: Row) = OrganizationUnit(
        id = row.string("organization_unit_id"),
        name = row.string("name"),
        email = row.string("email"),
        members = row.stringOrNull("member")
            ?.split(",")
            ?.takeIf { it.size > 1 || it[0].isNotEmpty() }
            ?.toMutableSet() ?: mutableSetOf()
    )
}

fun MutableSet<String>.toStringList(): String = toList()
    .filter { it.isNotEmpty() }
    .joinToString((","))

val Pair<OrganizationUnit?, List<Report>>.organizationUnit
    get() = this.first
val Pair<OrganizationUnit?, List<Report>>.reports
    get() = this.second