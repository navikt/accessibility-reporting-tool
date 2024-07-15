package accessibility.reporting.tool.database

import accessibility.reporting.tool.authenitcation.User.Email
import kotliquery.queryOf

class OrganizationRepository(val database: Database) {
    fun getOrganizationForUser(email: Email): List<String> = database.list {
        queryOf(
            /*"""SELECT DISTINCT ou.name
                       FROM report r
                       JOIN organization_unit ou ON r.organization_unit_id = ou.organization_unit_id = ou.organization_unit_id
                       WHERE r.report_data -> 'user' ->> 'email' = :email
                          OR r.report_data -> 'author' ->> 'email' = :email""",*/

            /*"""SELECT ou.name
           FROM organization_unit ou
           WHERE ou.email = :email""",
            mapOf(
                "email" to email.str()
            )*/
            """SELECT ou.name
               FROM organization_unit ou<<<<<<< jaspreet-is-here
               WHERE LOWER(ou.email) = LOWER(:email) OR LOWER(:email) = ANY(string_to_array(LOWER(ou.member), ','))         

            """,
            mapOf(
                "email" to email.str()
            )
        ).map { row -> row.string("name") }.asList
    }
}
