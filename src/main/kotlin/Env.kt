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
        val url = "NAIS_DATABASE_DIALOG_DIALOG_JDBC_URL".fromEnv()
        val username = "NAIS_DATABASE_DIALOG_DIALOG_USERNAME".fromEnv()
        val password = "NAIS_DATABASE_DIALOG_DIALOG_PASSWORD".fromEnv()
        val name = "NAIS_DATABASE_DIALOG_DIALOG_DATABASE".fromEnv()
    }

    val navArbeidsgiverApiBaseUrl = "NAV_ARBEIDSGIVER_API_BASEURL".fromEnv()
    val altinnBaseUrl = "ALTINN_3_BASE_URL".fromEnv()
    val altinnImRessurs = "ALTINN_IM_RESSURS".fromEnv()
    val tokenEndpoint = "NAIS_TOKEN_ENDPOINT".fromEnv()
    val tokenAltinn3ExchangeEndpoint = "${"ALTINN_3_BASE_URL".fromEnv()}/authentication/api/v1/exchange/maskinporten"

    val dialogportenScope = "DIALOGPORTEN_SCOPE".fromEnv()

    private fun String.fromEnv(): String =
        System.getenv(this)
            ?: appConfig.propertyOrNull(this)?.getString()
            ?: throw RuntimeException("Missing required environment variable \"$this\".")
}
