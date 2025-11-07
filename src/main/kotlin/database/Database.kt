package no.nav.helsearbeidsgiver.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.helsearbeidsgiver.Env
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

    fun migrate() {
        config
            .let(::HikariDataSource)
            .also { dataSource ->
                Flyway
                    .configure()
                    .dataSource(dataSource)
                    .load()
                    .migrate()
            }.close()
    }
}

private fun dbConfig(): HikariConfig =
    HikariConfig().apply {
        jdbcUrl = Env.Database.url
        username = Env.Database.username
        password = Env.Database.password
        maximumPoolSize = 3
    }
