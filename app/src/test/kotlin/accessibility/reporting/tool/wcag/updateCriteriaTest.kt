package accessibility.reporting.tool.wcag

import accessibility.reporting.tool.authenitcation.User
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDateTime
import java.util.*

class updateCriteriaTest {
    private val testOrg = OrganizationUnit.createNew(name = "Test organisasjonsenhet", email = "test@nav.no")
    private val testUser =
        User(User.Email("testuser@nav.no"), "Test User", User.Oid(UUID.randomUUID().toString()), groups = listOf())

    @Test
    fun `oppdaterer rapport ved å lage ny kopi`(){
        val updateBy = testUser

        val newCriteria = listOf(SuccessCriterion(
            name ="1.1.1",
            description ="Description 1",
            principle ="Principle 1",
            guideline ="Guideline 1",
            tools ="Tools 1",
            number ="tull",
            breakingTheLaw ="tull",
            lawDoesNotApply ="tull",
            tooHardToComply ="tull",
            contentGroup ="tull",
            status =Status.NOT_TESTED
        ))
        val initialReport = Report(
            reportId ="tull",
            url ="tull",
            descriptiveName ="tull",
            organizationUnit =testOrg,
            version =Version.V2,
            author =testUser.toAuthor(),
            successCriteria = listOf(
                SuccessCriterion(
                    name ="initial criteria",
                    description ="initial description",
                    principle ="initial principle",
                    guideline ="initial guideline",
                    tools ="initial tools",
                    number ="initial number",
                    breakingTheLaw ="NO",
                    lawDoesNotApply ="YES",
                    tooHardToComply ="NO",
                    contentGroup ="A",
                    status =Status.COMPLIANT

                )
            ),
            created = LocalDateTime.now(),
            lastChanged =LocalDateTime.now(),
            lastUpdatedBy =testUser.toAuthor(),
            reportType =ReportType.SINGLE
        )
        val updatedReport = initialReport.updateCriteria(newCriteria,updateBy)

        assertAll(
            { assertEquals(newCriteria, updatedReport.successCriteria, "Success criteria not updated correctly") },
            { assertNotNull(updatedReport.lastChanged, "Last changed time not updated") },

        )
    } }