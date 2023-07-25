package accessibility.reporting.tool.wcag

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OrganizationUnitTest {

    @Test
    fun `Lager korrekt id`() {
        OrganizationUnit.createNew(
            name = "A name that is a name that is a name",
            email = "someemail@nav.no",
            shortName = "A shorter name"
        ).id shouldBe "a-shorter-name"
        OrganizationUnit.createNew(
            name = "A name that is a name that is a name",
            email = "someemail@nav.no",
            shortName = null
        ).id shouldBe "a-name-that-is-a-name-that-is-a-name"
    }
}
