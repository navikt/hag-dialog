package no.nav.helsearbeidsgiver.kafka.kafka

import no.nav.helsearbeidsgiver.dokumentKobling.DokumentKobling
import no.nav.helsearbeidsgiver.dokumentKobling.DokumentKoblingService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory

class DokumentKoblingTolker(
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val dokumentKoblingService: DokumentKoblingService,
) {
    private val logger = LoggerFactory.getLogger(DokumentKoblingTolker::class.java)
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
                is no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding -> {
                    if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr = dekodetMelding.orgnr)) {
                        dokumentKoblingService.lagreSykmelding(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for dialogopprettelse for sykmelding er avskrudd, " +
                                "ignorerer melding for sykmeldingId ${dekodetMelding.sykmeldingId}.",
                        )
                    }
                }

                is no.nav.helsearbeidsgiver.dokumentKobling.Sykepengesoeknad -> {
                    if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr = dekodetMelding.orgnr)) {
                        dokumentKoblingService.lagreSykepengesoeknad(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for oppdatering av dialog med sykepengesøknad er avskrudd, " +
                                "ignorerer melding for sykepengesøknadId ${dekodetMelding.soeknadId}.",
                        )
                    }
                }
            }
        }.getOrElse { e ->
            sikkerLogger.error("Klarte ikke opprette/oppdatere dialog. Avbryter.", e)
            throw e
        }
    }

    private fun dekodMelding(melding: String): DokumentKobling = melding.fromJson(DokumentKobling.serializer())
}
