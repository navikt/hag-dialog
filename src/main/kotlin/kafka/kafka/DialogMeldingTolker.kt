package no.nav.helsearbeidsgiver.kafka.kafka

import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory

class DialogMeldingTolker(
    val dialogportenService: DialogportenService,
) {
    private val logger = LoggerFactory.getLogger(DialogMeldingTolker::class.java)
    private val sikkerLogger = sikkerLogger()

    fun lesMelding(melding: String) {
        sikkerLogger.info("Leser mottatt dialog-melding: $melding")

        runCatching {
            val dekodMelding = dekodMelding(melding)
            dialogportenService.opprettDialogForFritakAgp(dekodMelding)
        }.getOrElse { e ->
            logger.error("Klarte ikke behandle dialog-melding. Melding blir ignorert.")
            sikkerLogger.error("Klarte ikke behandle dialog-melding. Melding blir ignorert. Melding: $melding", e)
        }
    }
}

private fun dekodMelding(melding: String): DialogMelding = melding.fromJson(DialogMelding.serializer())
