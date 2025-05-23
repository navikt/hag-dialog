package no.nav.helsearbeidsgiver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

class DatabaseConfig(
    url: String? = Env.Database.url,
    private val username: String? = Env.Database.username,
    private val password: String? = Env.Database.password,
) {
    private val dbName = Env.Database.name
    private val host = Env.Database.host
    private val port = Env.Database.port

    private val jdbcUrl = url ?: "jdbc:postgresql://%s:%s/%s".format(host, port, dbName)

    fun init() {
        val dataSource = postgresDataSource()
        runMigrate(dataSource)
    }

    private fun postgresDataSource(): HikariDataSource {
        val config = HikariConfig()
        config.driverClassName = "org.postgresql.Driver"
        config.jdbcUrl = jdbcUrl
        config.username = username
        config.password = password
        config.maximumPoolSize = 3
        config.isAutoCommit = true
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
        config.validate()
        return HikariDataSource(config)
    }

    private fun runMigrate(dataSource: HikariDataSource) {
        val flyway =
            Flyway
                .configure(ClassLoader.getSystemClassLoader())
                .validateMigrationNaming(true)
                .dataSource(dataSource)
                .load()
        flyway.migrate()
        flyway.validate()
    }
}
