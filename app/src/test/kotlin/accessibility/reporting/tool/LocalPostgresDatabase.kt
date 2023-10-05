import accessibility.reporting.tool.database.Database
import com.zaxxer.hikari.HikariDataSource
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDateTime

class LocalPostgresDatabase private constructor() : Database {

    private val memDataSource: HikariDataSource
    private val container = PostgreSQLContainer<Nothing>("postgres:14.5")

    companion object {
        private val instance by lazy {
            LocalPostgresDatabase().also {
                it.migrate() shouldBe 4
            }
        }

        fun cleanDb(): LocalPostgresDatabase {
            instance.update { queryOf("delete from changelog") }
            instance.update { queryOf("delete from report") }
            instance.update { queryOf("delete from organization_unit") }
            return instance
        }
    }

    init {
        container.start()
        memDataSource = createDataSource()
    }

    override val dataSource: HikariDataSource
        get() = memDataSource

    private fun createDataSource(): HikariDataSource {
        return HikariDataSource().apply {
            jdbcUrl = container.jdbcUrl
            username = container.username
            password = container.password
            isAutoCommit = true
            validate()
        }
    }

    private fun migrate(): Int =
        Flyway.configure()
        .connectRetries(3)
        .dataSource(dataSource)
        .load()
        .migrate().migrationsExecuted
}

inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }
