package no.nav.helsearbeidsgiver.kafka

import no.nav.helsearbeidsgiver.Env
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

fun createKafkaConsumerConfig(): Properties {
    val consumerKafkaProperties =
        mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to Env.Kafka.kafkaBrokers,
            ConsumerConfig.GROUP_ID_CONFIG to "helsearbeidsgiver-dialog",
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java.name,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to "false",
            ConsumerConfig.MAX_POLL_RECORDS_CONFIG to "1",
            ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG to "30000",
            ConsumerConfig.CLIENT_ID_CONFIG to "dialog",
        )
    return Properties().apply { putAll(consumerKafkaProperties + commonKafkaProperties()) }
}

private fun commonKafkaProperties(): Map<String, String> {
    val truststorePath = Env.Kafka.kafkaTruststorePath
    val keystorePath = Env.Kafka.kafkaKeystorePath
    val credstorePassword = Env.Kafka.kafkaCredstorePassword

    if (truststorePath == null ||
        keystorePath == null ||
        credstorePassword == null ||
        truststorePath.isEmpty() ||
        keystorePath.isEmpty() ||
        credstorePassword.isEmpty()
    ) {
        return emptyMap()
    }

    return buildSslConfig(truststorePath, keystorePath, credstorePassword)
}

private fun buildSslConfig(
    truststorePath: String,
    keystorePath: String,
    credstorePassword: String,
): Map<String, String> {
    val pkcs12 = "PKCS12"
    val javaKeyStore = "jks"

    val truststoreConfig =
        mapOf(
            SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG to truststorePath,
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG to SecurityProtocol.SSL.name,
            SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG to "",
            SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG to javaKeyStore,
            SslConfigs.SSL_KEYSTORE_TYPE_CONFIG to pkcs12,
        )

    val credstoreConfig =
        mapOf(
            SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG to credstorePassword,
            SslConfigs.SSL_KEY_PASSWORD_CONFIG to credstorePassword,
        )

    val keystoreConfig =
        mapOf(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG to keystorePath)

    return truststoreConfig + credstoreConfig + keystoreConfig
}
