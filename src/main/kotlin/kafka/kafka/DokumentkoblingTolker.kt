package no.nav.helsearbeidsgiver.kafka.kafka

import dokumentkobling.Dokumentkobling
import dokumentkobling.DokumentkoblingService
import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InntektsmeldingGodkjent
import dokumentkobling.Sykepengesoeknad
import dokumentkobling.Sykmelding
import dokumentkobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.fromJson
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.slf4j.LoggerFactory

class DokumentkoblingTolker(
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val dokumentkoblingService: DokumentkoblingService,
) {
    private val logger = LoggerFactory.getLogger(DokumentkoblingTolker::class.java)
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
                        dokumentkoblingService.lagreSykmelding(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for dialogopprettelse for sykmelding er avskrudd, " +
                                "ignorerer melding for sykmeldingId ${dekodetMelding.sykmeldingId}.",
                        )
                    }
                }

                is Sykepengesoeknad -> {
                    if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr = dekodetMelding.orgnr)) {
                        dokumentkoblingService.lagreSykepengesoeknad(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for oppdatering av dialog med sykepengesøknad er avskrudd, " +
                                "ignorerer melding for sykepengesøknadId ${dekodetMelding.soeknadId}.",
                        )
                    }
                }

                is VedtaksperiodeSoeknadKobling -> {
                    dokumentkoblingService.lageVedtaksperiodeSoeknadKobling(dekodetMelding)
                }

                is ForespoerselSendt -> {
                    if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = dekodetMelding.orgnr)) {
                        dokumentkoblingService.lagreForespoerselSendt(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for oppdatering av dialog med forespørsel sendt er avskrudd, " +
                                "ignorerer melding for forespoerselId ${dekodetMelding.forespoerselId}.",
                        )
                    }
                }

                is ForespoerselUtgaatt -> {
                    if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = dekodetMelding.orgnr)) {
                        dokumentkoblingService.lagreForespoerselUtgaatt(dekodetMelding)
                    } else {
                        logger.info(
                            "Feature toggle for oppdatering av dialog med forespørsel utgaatt er avskrudd, " +
                                "ignorerer melding for forespoerselId ${dekodetMelding.forespoerselId}.",
                        )
                    }
                }

                is InntektsmeldingGodkjent -> {
                    dokumentkoblingService.lagreInntektsmeldingGodkjent(dekodetMelding)
                }
            }
        }.getOrElse { e ->
            sikkerLogger.error("Klarte ikke opprette/oppdatere dialog. Avbryter.", e)
            throw e
        }
    }

    private fun dekodMelding(melding: String): Dokumentkobling = melding.fromJson(Dokumentkobling.serializer())
}
