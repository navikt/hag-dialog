package no.nav.helsearbeidsgiver.kafka.kafka

import dokumentkobling.Dokumentkobling
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.logger
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
            behandleMelding(melding)
        }.getOrElse { e ->
            logger.error("Klarte ikke behandle dialog-melding. Melding blir ignorert.")
            sikkerLogger.error("Klarte ikke behandle dialog-melding. Melding blir ignorert. Melding: $melding", e)
        }
    }

    private fun behandleMelding(melding: String) {
        logger.info(melding)
        val dekodMelding = dekodMelding(melding)
        when (dekodMelding) {
            is GravidSoeknadMelding -> dialogportenService.opprettDialogForFritakAgp()
        }

        logger.info("Behandler dialog-melding")
        sikkerLogger.info("Behandler dialog-melding: $melding")
    }
}

private fun dekodMelding(melding: String): DialogMelding = melding.fromJson(DialogMelding.serializer())
