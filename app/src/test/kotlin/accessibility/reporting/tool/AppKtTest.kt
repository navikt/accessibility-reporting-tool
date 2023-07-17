package accessibility.reporting.tool

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test


class AppKtTest {

    @Test
    fun testGetIsalive() = testApplication {
        application {
            api()
        }
        assertEquals(HttpStatusCode.OK, client.get("/isAlive").status)
    }
}