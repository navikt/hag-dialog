package local

import forespoersel_utgaatt
import inntektsmelding_feilet
import inntektsmelding_godkjent
import inntektsmelding_mottatt
import inntektsmeldingsforespoersel
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.kafka.Melding
import no.nav.helsearbeidsgiver.utils.json.toJson
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import sykepengesoeknad
import sykmelding
import java.util.Properties

class ConsumerProducerFactory {
    fun createProducer(): KafkaProducer<String, String> {
        val props =
            Properties().apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java.name)
            }
        return KafkaProducer(props)
    }
}

class DialogKlient(
    val factory: ConsumerProducerFactory,
) {
    fun sendToKafka(melding: Melding): Boolean {
        try {
            factory.createProducer().use { producer ->
                val message = melding.toJson(Melding.serializer())
                println(message)
                val record = message.toRecord()
                producer.send(record).get().also { metadata ->
                    println("========================================================")
                    println("Record produced to partition #${metadata.partition()} with offset ${metadata.offset()}")
                    println("========================================================")
                }
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}

fun producerFactory(env: String): ConsumerProducerFactory = ConsumerProducerFactory()

fun main() {
    val factory = producerFactory("dev")
    val dialogKlient = DialogKlient(factory)

    dialogKlient.sendToKafka(sykmelding)
    dialogKlient.sendToKafka(sykepengesoeknad)
    dialogKlient.sendToKafka(inntektsmeldingsforespoersel)
    dialogKlient.sendToKafka(forespoersel_utgaatt)
    dialogKlient.sendToKafka(inntektsmelding_mottatt)
    dialogKlient.sendToKafka(inntektsmelding_godkjent)
    dialogKlient.sendToKafka(inntektsmelding_feilet)
}

private fun JsonElement.toRecord(): ProducerRecord<String, String> = ProducerRecord("helsearbeidsgiver.dialog", "key", this.toString())
