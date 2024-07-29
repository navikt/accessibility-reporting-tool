package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import accessibility.reporting.tool.database.Admins
import accessibility.reporting.tool.rest.NewTeam
import com.fasterxml.jackson.databind.JsonNode

data class OrganizationUnit(
    val id: String,
    val name: String,
    val email: String,
    val members: MutableSet<String> = mutableSetOf()
) {
    fun isMember(user: User) = members.any { it == user.email.str().comparable() }
    fun addMember(userEmail: User.Email) = members.add(userEmail.str().comparable())
    fun removeMember(userEmail: String) = members.removeIf { userEmail.comparable() == it.comparable() }

    companion object {
        fun createNew(name: String, email: String, shortName: String? = null) = OrganizationUnit(
            id = shortName?.toOrgUnitId() ?: name.toOrgUnitId(),
            name = name,
            email = email,
            members = mutableSetOf()
        )

        fun createNew(newTeam: NewTeam) = OrganizationUnit(
            id = newTeam.name.toOrgUnitId(),
            name = newTeam.name,
            email = newTeam.email,
            members = newTeam.members.toMutableSet()
        )


        private fun String.toOrgUnitId() = trimMargin()
            .lowercase()
            .replace(" ", "-")

        fun fromJson(organizationJson: JsonNode): OrganizationUnit = OrganizationUnit(
            id = organizationJson["id"].asText(),
            name = organizationJson["name"].asText(),
            email = organizationJson["email"].asText(),
            members = organizationJson["members"].takeIf { it != null }?.toList()?.map { it.asText() }
                ?.toMutableSet()
                ?: mutableSetOf()
        )
    }

    private fun String.comparable(): String = trimIndent().lowercase()
    fun hasWriteAccess(user: User): Boolean =
        user.email.str() == email || Admins.isAdmin(user) || members.any { it == user.email.str() }
}

class Author(val email: String, val oid: String) {
    companion object {
        fun fromJsonOrNull(jsonNode: JsonNode?, fieldName: String): Author? =
            jsonNode?.get(fieldName)?.takeIf { !it.isNull && !it.isEmpty }
                ?.let { Author(email = it["email"].asText(), oid = it["oid"].asText()) }

        fun fromJson(jsonNode: JsonNode, fieldName: String): Author =
            jsonNode[fieldName].let { Author(email = it["email"].asText(), oid = it["oid"].asText()) }
    }
}