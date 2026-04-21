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

    suspend fun lesMelding(melding: String) {
        sikkerLogger.info("Leser mottatt dialog-melding: $melding")

        val dekodMelding =
            runCatching { dekodMelding(melding) }
                .getOrElse { e ->
                    logger.error("Klarte ikke deserialisere dialog-melding. Melding blir ikke prosessert.")
                    sikkerLogger.error("Klarte ikke deserialisere dialog-melding. Melding blir ikke prosessert. Melding: $melding", e)
                    throw e
                }

        runCatching { dialogportenService.opprettDialogForFritakAgp(dekodMelding) }
            .getOrElse { e ->
                logger.error("Klarte ikke sende dialog-melding til Dialogporten. Melding blir ikke prosessert.")
                sikkerLogger.error("Klarte ikke sende dialog-melding til Dialogporten. Melding blir ikke prosessert. Melding: $melding", e)
                throw e
            }
    }
}

private fun dekodMelding(melding: String): DialogMelding = melding.fromJson(DialogMelding.serializer())
