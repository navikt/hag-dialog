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
                    logger.info("Ignorerer sykmelding med id: ${dekodetMelding.sykmeldingId} på dialog topic")
                }

                is Sykepengesoeknad -> {
                    logger.info("Ignorerer sykepengesoeknad med id: ${dekodetMelding.soeknadId} på dialog topic")
                }

                is Inntektsmeldingsforespoersel -> {
                    logger.info("Ignorerer forespoersel sendt med id: ${dekodetMelding.forespoerselId} på dialog topic")
                }

                is Inntektsmelding -> {
                    logger.info("Ignorerer inntektsmelding sendt med id: ${dekodetMelding.innsendingId} på dialog topic")
                }

                is UtgaattInntektsmeldingForespoersel -> {
                    logger.info("Ignorerer forespoersel utgatt med id: ${dekodetMelding.forespoerselId} på dialog topic")
                }
            }
        }.getOrElse { e ->
            sikkerLogger.error("Klarte ikke opprette/oppdatere dialog. Avbryter.", e)
            throw e
        }
    }

    private fun dekodMelding(melding: String): Melding = melding.fromJson(Melding.serializer())
}
