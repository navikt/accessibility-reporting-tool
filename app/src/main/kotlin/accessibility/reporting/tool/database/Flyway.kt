package accessibility.reporting.tool.database

import org.flywaydb.core.api.configuration.FluentConfiguration

object Flyway {
    fun runFlywayMigrations(env: Environment) {
        val flyway = configure(env).load()
        flyway.migrate()
    }
    private fun configure(env: Environment): FluentConfiguration {
        val configBuilder = configure(env).connectRetries(5)
        val dataSource = PostgresDatabase.hikariFromLocalDb(env)
        configBuilder.dataSource(dataSource)
        return configBuilder
    }
}
