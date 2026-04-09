package local

import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravStatus
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravMelding
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadMelding
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
    val kroniskKravId = UUID.randomUUID()
    dialogKlient.sendToKafka(
        GravidSoeknadMelding(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
        ),
    )
    dialogKlient.sendToKafka(
        GravidKravMelding(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.OPPRETTET,
        ),
    )
    dialogKlient.sendToKafka(
        GravidKravMelding(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.ENDRET,
        ),
    )
    dialogKlient.sendToKafka(
        GravidKravMelding(
            id = gravidkravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.SLETTET,
        ),
    )
    dialogKlient.sendToKafka(
        KroniskSoeknadMelding(
            id = UUID.randomUUID(),
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
        ),
    )

    dialogKlient.sendToKafka(
        KroniskKravMelding(
            id = kroniskKravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.OPPRETTET,
        ),
    )
    dialogKlient.sendToKafka(
        KroniskKravMelding(
            id = kroniskKravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.ENDRET,
        ),
    )
    dialogKlient.sendToKafka(
        KroniskKravMelding(
            id = kroniskKravId,
            orgnr = Orgnr("214398982"),
            navn = "Test Navn",
            fnr = "01010112345",
            status = FritakKravStatus.SLETTET,
        ),
    )
}

private fun JsonElement.toRecord(): ProducerRecord<String, String> = ProducerRecord("helsearbeidsgiver.dialog", "key", this.toString())
