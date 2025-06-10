package no.nav.helsearbeidsgiver

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.HoconApplicationConfig

private val appConfig = HoconApplicationConfig(ConfigFactory.load())

object Env {
    object Unleash {
        val apiKey = "UNLEASH_SERVER_API_TOKEN".fromEnv()
        val apiUrl = "UNLEASH_SERVER_API_URL".fromEnv()
        val apiEnv = "UNLEASH_SERVER_API_ENV".fromEnv()
    }

    object Kafka {
        val topic = "DIALOG_KAFKA_TOPIC".fromEnv()
        val kafkaBrokers = "KAFKA_BROKERS".fromEnv()
        val kafkaTruststorePath = "KAFKA_TRUSTSTORE_PATH".fromEnv()
        val kafkaCredstorePassword = "KAFKA_CREDSTORE_PASSWORD".fromEnv()
        val kafkaKeystorePath = "KAFKA_KEYSTORE_PATH".fromEnv()
    }

    object Database {
        private val databasePrefix = "NAIS_DATABASE_DIALOG_DIALOG"
        val url = "${databasePrefix}_JDBC_URL".fromEnv()
        val username = "${databasePrefix}_USERNAME".fromEnv()
        val password = "${databasePrefix}_PASSWORD".fromEnv()
        val name = "${databasePrefix}_DATABASE".fromEnv()
    }

    object Nav {
        val arbeidsgiverApiBaseUrl = "arbeidsgiver.apiBaseUrl".fromEnv()
        val arbeidsgiverSykepengerApiSwaggerUrl = "arbeidsgiver.apiSwaggerUrl".fromEnv()
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
}
