package local

import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.GravidKravOpprettet
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.Properties
import java.util.UUID

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
    fun sendToKafka(melding: DialogMelding): Boolean {
        try {
            factory.createProducer().use { producer ->
                val message = melding.toJson(DialogMelding.serializer())
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
    val gravidkravId = UUID.randomUUID()
    dialogKlient.sendToKafka(
        GravidKravOpprettet(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
        ),
    )
}

private fun JsonElement.toRecord(): ProducerRecord<String, String> = ProducerRecord("helsearbeidsgiver.dialog", "key", this.toString())
