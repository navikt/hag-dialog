package no.nav.helsearbeidsgiver.kafka

import dokumentkobling.DokumentkoblingService
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.helsesjekker.ShutDownAppState
import no.nav.helsearbeidsgiver.kafka.kafka.DokumentkoblingTolker
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

fun Application.configureKafkaConsumer(
    unleashFeatureToggles: UnleashFeatureToggles,
    dokumentkoblingRepository: DokumentkoblingRepository,
) {
    val kafkaConsumerExceptionHandler =
        CoroutineExceptionHandler { _, exception ->
            "Kafka consumer feilet. Tar ned applikasjonen ved å la helsesjekkene feile.".also {
                logger().error(it)
                sikkerLogger().error(it, exception)
            }
            ShutDownAppState.shutDownApp = true
        }

    launch(Dispatchers.Default + kafkaConsumerExceptionHandler) {
        startDokumentkoblingKafkaConsumer(
            dokumentkoblingTolker =
                DokumentkoblingTolker(
                    unleashFeatureToggles = unleashFeatureToggles,
                    dokumentkoblingService =
                        DokumentkoblingService(
                            dokumentkoblingRepository = dokumentkoblingRepository,
                        ),
                ),
        )
    }
}

private fun startDokumentkoblingKafkaConsumer(dokumentkoblingTolker: DokumentkoblingTolker) {
    val consumer = KafkaConsumer<String, String>(createKafkaConsumerConfig() as Map<String, Any>)
    val topic = Env.Kafka.dokumentkoblingTopic
    consumer.subscribe(listOf(topic))
    val running = true
    while (running) {
        val records = consumer.poll(Duration.ofMillis(1000))
        for (record in records) {
            dokumentkoblingTolker.lesMelding(record.value())
            consumer.commitSync()
        }
    }
}
