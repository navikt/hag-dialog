package no.nav.helsearbeidsgiver.kafka.kafka

import dokumentkobling.Dokumentkobling
import dokumentkobling.DokumentkoblingService
import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InntektsmeldingAvvist
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
                    dokumentkoblingService.lagreSykmelding(dekodetMelding)
                }

                is Sykepengesoeknad -> {
                    dokumentkoblingService.lagreSykepengesoeknad(dekodetMelding)
                }

                is VedtaksperiodeSoeknadKobling -> {
                    dokumentkoblingService.lageVedtaksperiodeSoeknadKobling(dekodetMelding)
                }

                is ForespoerselSendt -> {
                    dokumentkoblingService.lagreForespoerselSendt(dekodetMelding)
                }

                is ForespoerselUtgaatt -> {
                    dokumentkoblingService.lagreForespoerselUtgaatt(dekodetMelding)
                }

                is InntektsmeldingGodkjent -> {
                    dokumentkoblingService.lagreInntektsmeldingGodkjent(dekodetMelding)
                }

                is InntektsmeldingAvvist -> {
                    dokumentkoblingService.lagreInntektsmeldingAvvist(dekodetMelding)
                }
            }
        }.getOrElse { e ->
            sikkerLogger.error("Klarte ikke opprette/oppdatere dialog. Avbryter.", e)
            throw e
        }
    }

    private fun dekodMelding(melding: String): Dokumentkobling = melding.fromJson(Dokumentkobling.serializer())
}
