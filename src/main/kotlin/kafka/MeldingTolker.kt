package no.nav.helsearbeidsgiver.kafka

import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory

class MeldingTolker(
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val dialogportenService: DialogportenService,
) {
    private val logger = LoggerFactory.getLogger(MeldingTolker::class.java)
    private val sikkerLogger = sikkerLogger()

    fun lesMelding(melding: String) {
        sikkerLogger.info("Leser mottatt melding: $melding")

        val dekodetMelding =
            runCatching { dekodMelding(melding) }
                .getOrElse { e ->
                    sikkerLogger.error("Klarte ikke dekode melding. Melding blir ignorert", e)
                    return
                }

        runCatching {
            when (dekodetMelding) {
                is Sykmelding -> {
                    if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr = dekodetMelding.orgnr)) {
                        logger.info("Oppretter dialog for sykmelding.")
                        dialogportenService.opprettNyDialogMedSykmelding(dekodetMelding)
                    } else {
                        logger.info("Feature toggle for dialogopprettelse for sykmelding er avskrudd, ignorerer melding.")
                    }
                }
            }
        }.getOrElse { e ->
            sikkerLogger.error("Klarte ikke opprette dialog. Avbryter.", e)
            throw e
        }
    }

    private fun dekodMelding(melding: String): Melding = melding.fromJson(Melding.serializer())
}
