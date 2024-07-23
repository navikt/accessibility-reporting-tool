package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.wcag.OrganizationUnit
import kotliquery.Row
import kotliquery.queryOf

class OrganizationRepository(val database: Database) {
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

    fun getOrganizationUnit(id: String): OrganizationUnit? = database.query {
        queryOf("select * from organization_unit where organization_unit_id=:id", mapOf("id" to id)).map { row ->
            organizationUnit(row)
        }.asSingle
    }
    fun organizationUnit(row: Row): OrganizationUnit {
        return OrganizationUnit(
            id = row.string("organization_unit_id"),
            name = row.string("name"),
            email = row.string("email"),
            members = row.stringOrNull("member")?.split(",")?.toMutableSet() ?: mutableSetOf()
        )
    }

}
