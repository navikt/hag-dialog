package dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.SykepengerDialogportenService
import no.nav.helsearbeidsgiver.kafka.Vedtak
import no.nav.helsearbeidsgiver.metrikk.oppdaterMetrikkForAntallVedtakMedStatusMottatt
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.Duration
import java.util.UUID

class VedtakJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val sykepengerDialogportenService: SykepengerDialogportenService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        if (!unleashFeatureToggles.skalOppretteVedtakTransmissions()) {
            logger.warn("Oppretter ikke transmissions for vedtak siden det er deaktivert i Unleash.")
            return
        }

        val vedtakKlareForBehandling = dokumentkoblingRepository.hentVedtakKlareForBehandling()

        oppdaterMetrikkForAntallVedtakMedStatusMottatt(nyVerdi = vedtakKlareForBehandling.size)
            .also { logger.info("Fant ${vedtakKlareForBehandling.size} vedtak med status MOTTATT klar til behandling.") }

        vedtakKlareForBehandling.forEach { vedtak ->
            try {
                if (vedtak.inntektsmeldingJobbStatus == Status.BEHANDLET) {
                    sykepengerDialogportenService.opprettTransmissionForVedtak(
                        vedtakId = vedtak.vedtakId,
                        sykmeldingId = vedtak.sykmeldingId,
                        inntektsmeldingId = vedtak.inntektsmeldingId,
                        orgnr = vedtak.orgnr,
                    )
                    dokumentkoblingRepository.settVedtakJobbTilBehandlet(vedtak.vedtakId)
                } else {
                    logger.info(
                        "Inntektsmelding med id ${vedtak.inntektsmeldingId} er ikke behandlet enda, " +
                            "kan ikke sende vedtak med id ${vedtak.vedtakId} til Dialogporten.",
                    )
                }
            } catch (e: Exception) {
                "Feil ved behandling av vedtak med id ${vedtak.vedtakId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }
}

fun SykepengerDialogportenService.opprettTransmissionForVedtak(
    vedtakId: UUID,
    sykmeldingId: UUID,
    inntektsmeldingId: UUID,
    orgnr: Orgnr,
) {
    oppdaterDialogMedVedtak(
        Vedtak(
            vedtakId = vedtakId,
            sykmeldingId = sykmeldingId,
            inntektsmeldingId = inntektsmeldingId,
            orgnr = orgnr,
        ),
    )
}
