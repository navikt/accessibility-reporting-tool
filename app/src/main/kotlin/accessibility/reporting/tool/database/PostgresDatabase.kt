package accessibility.reporting.tool.database

import accessibility.reporting.tool.Environment
import accessibility.reporting.tool.authenitcation.User
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

class PostgresDatabase(environment: Environment) : Database {


    private val envDataSource: HikariDataSource = hikariFromLocalDb(environment)
    override val dataSource: HikariDataSource
        get() = envDataSource


    companion object {

        fun hikariFromLocalDb(env: Environment): HikariDataSource {
            val config = hikariCommonConfig(env)
            config.validate()
            return HikariDataSource(config)
        }

        private fun hikariCommonConfig(env: Environment): HikariConfig {
            val config = HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = env.dbUrl
                minimumIdle = 1
                maxLifetime = 1800000
                maximumPoolSize = 5
                connectionTimeout = 4000
                validationTimeout = 1000
                idleTimeout = 30000
                isAutoCommit = true
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                username = env.dbUser
                password = env.dbPassword
            }
            return config
        }
    }
}

object Admins {
    private val admin_group = System.getenv("ADMIN_GROUP")
    fun isAdmin(user: User) = user.groups.contains(admin_group)
}
