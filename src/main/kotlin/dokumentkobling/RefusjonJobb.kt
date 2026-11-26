package dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.SykepengerDialogportenService
import no.nav.helsearbeidsgiver.kafka.Refusjon
import no.nav.helsearbeidsgiver.metrikk.oppdaterMetrikkForAntallRefusjonMedStatusMottatt
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class RefusjonJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val sykepengerDialogportenService: SykepengerDialogportenService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        if (!unleashFeatureToggles.skalOppretteVedtakTransmissions()) {
            logger.warn("Oppretter ikke transmissions for refusjon siden det er deaktivert i Unleash.")
            return
        }

        val refusjonKlareForBehandling = dokumentkoblingRepository.hentRefusjonKlareForBehandling()

        oppdaterMetrikkForAntallRefusjonMedStatusMottatt(nyVerdi = refusjonKlareForBehandling.size)
            .also { logger.info("Fant ${refusjonKlareForBehandling.size} refusjon med status MOTTATT klar til behandling.") }

        refusjonKlareForBehandling.forEach { refusjon ->
            try {
                val sykmelding = dokumentkoblingRepository.hentSykmeldingEntitet(refusjon.sykmeldingId)

                if (sykmelding?.status == Status.BEHANDLET) {
                    sykepengerDialogportenService.opprettTransmissionForRefusjon(
                        refusjonUtfallId = refusjon.refusjonUtfallId,
                        sykmeldingId = refusjon.sykmeldingId,
                        inntektsmeldingId = refusjon.inntektsmeldingId,
                        orgnr = refusjon.orgnr,
                        fom = refusjon.fom,
                        tom = refusjon.tom,
                    )
                    dokumentkoblingRepository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId)
                } else {
                    logger.info(
                        "Sykmelding med id ${refusjon.sykmeldingId} er ikke behandlet enda, " +
                            "kan ikke sende refusjon med id ${refusjon.refusjonUtfallId} til Dialogporten.",
                    )
                }
            } catch (e: Exception) {
                "Feil ved behandling av refusjon med id ${refusjon.refusjonUtfallId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }
}

fun SykepengerDialogportenService.opprettTransmissionForRefusjon(
    refusjonUtfallId: UUID,
    sykmeldingId: UUID,
    inntektsmeldingId: UUID?,
    orgnr: Orgnr,
    fom: LocalDate,
    tom: LocalDate,
) {
    oppdaterDialogMedRefusjon(
        Refusjon(
            refusjonUtfallId = refusjonUtfallId,
            sykmeldingId = sykmeldingId,
            inntektsmeldingId = inntektsmeldingId,
            orgnr = orgnr,
            fom = fom,
            tom = tom,
        ),
    )
}
