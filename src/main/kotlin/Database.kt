package no.nav.helsearbeidsgiver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database as ExposedDatabase

class Database(
    private val config: HikariConfig,
) {
    constructor() : this(
        dbConfig(),
    )

    val dataSource by lazy { HikariDataSource(config) }
    val db by lazy { ExposedDatabase.connect(dataSource) }

    fun migrate(location: String? = null) {
        migrationConfig(config)
            .let(::HikariDataSource)
            .also { dataSource ->
                Flyway
                    .configure()
                    .dataSource(dataSource)
                    .lockRetryCount(50)
                    .let {
                        if (location != null) {
                            it.locations("filesystem:$location")
                        } else {
                            it
                        }
                    }.load()
                    .migrate()
            }.close()
    }
}

private fun dbConfig(): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = Env.Database.url
        username = Env.Database.username
        password = Env.Database.password
        maximumPoolSize = 5
    }

private fun migrationConfig(config: HikariConfig): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password
        maximumPoolSize = 3
    }
