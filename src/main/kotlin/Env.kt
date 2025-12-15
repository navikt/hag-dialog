package no.nav.helsearbeidsgiver

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

private val appConfig = HoconApplicationConfig(ConfigFactory.load())

object Env {
    object Application {
        val local = "application.isLocal".fromEnvOrNull()?.toBoolean() ?: false
    }

    object Unleash {
        val apiKey = "UNLEASH_SERVER_API_TOKEN".fromEnv()
        val apiUrl = "UNLEASH_SERVER_API_URL".fromEnv()
        val apiEnv = "UNLEASH_SERVER_API_ENV".fromEnv()
    }

    object Kafka {
        val dokumentkoblingTopic = "kafka.dokumentkoblingTopic".fromEnv()
        val kafkaBrokers = "kafka.brokers".fromEnv()
        val kafkaTruststorePath = "kafka.truststorePath".fromEnvOrNull()
        val kafkaCredstorePassword = "kafka.credstorePassword".fromEnvOrNull()
        val kafkaKeystorePath = "kafka.keystorePath".fromEnvOrNull()
    }

    object Database {
        val url = "database.jdbcUrl".fromEnv()
        val username = "database.username".fromEnv()
        val password = "database.password".fromEnv()
        val name = "database.name".fromEnv()
    }

    object Nav {
        val arbeidsgiverApiBaseUrl = "arbeidsgiver.apiBaseUrl".fromEnv()
        val arbeidsgiverGuiBaseUrl = "arbeidsgiver.guiBaseUrl".fromEnv()
    }

    object Nais {
        val tokenEndpoint = "NAIS_TOKEN_ENDPOINT".fromEnv()
    }

    object Altinn {
        val baseUrl = "ALTINN_3_BASE_URL".fromEnv()
        val dialogportenRessurs = "ALTINN_DIALOGPORTEN_RESSURS".fromEnv()
        val tokenAltinn3ExchangeEndpoint =
            "${"ALTINN_3_BASE_URL".fromEnv()}/authentication/api/v1/exchange/maskinporten"
        val dialogportenScope = "DIALOGPORTEN_SCOPE".fromEnv()
    }

    fun String.fromEnv(): String =
        System.getenv(this)
            ?: appConfig.propertyOrNull(this)?.getString()
            ?: throw RuntimeException("Missing required environment variable \"$this\".")

    fun String.fromEnvOrNull(): String? =
        System.getenv(this)
            ?: appConfig.propertyOrNull(this)?.getString()?.takeIf { it.isNotEmpty() }
}
