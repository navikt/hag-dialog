package no.nav.helsearbeidsgiver

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database as ExposedDatabase

class Database(
    private val config: HikariConfig,
) {
    constructor() : this(
        dbConfig(Secrets()),
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

private fun dbConfig(secrets: Secrets): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = secrets.url
        username = secrets.username
        password = secrets.password
        maximumPoolSize = 5
    }

private fun migrationConfig(config: HikariConfig): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = config.jdbcUrl
        username = config.username
        password = config.password
        maximumPoolSize = 3
    }

private class Secrets {
    val username = Env.Database.username
    val password = Env.Database.password

    val url =
        "jdbc:postgresql://%s:%s/%s".format(
            Env.Database.host,
            Env.Database.port,
            Env.Database.name,
        )
}

