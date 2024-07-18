package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User.Email
import accessibility.reporting.tool.wcag.OrganizationUnit
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
}
