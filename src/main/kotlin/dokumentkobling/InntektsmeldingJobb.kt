package no.nav.helsearbeidsgiver.dokumentkobling

import dokumentkobling.DokumentkoblingService
import dokumentkobling.InnsendingType
import dokumentkobling.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.InntektsmeldingStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.Duration
import java.util.UUID

class InntektsmeldingJobb(
    private val dokumentkoblingService: DokumentkoblingService,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        val inntektsmeldinger = dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt()
        inntektsmeldinger.forEach { inntektsmelding ->
            try {
                val kobling = dokumentkoblingService.hentKoblingMedForespoerselId(inntektsmelding.forespoerselId)
                val orgnr = kobling?.let { dokumentkoblingService.hentSykmeldingOrgnr(it.sykmeldingId) }
                if (kobling?.forespoerselJobbStatus == Status.BEHANDLET && orgnr != null) {
                    dialogportenService.opprettTransmissionForInntektsmelding(
                        sykmeldingId = kobling.sykmeldingId,
                        forespoerselId = kobling.forespoerselId,
                        inntektsmeldingId = inntektsmelding.id.value,
                        orgnr = orgnr,
                        inntektsmeldingStatus = inntektsmelding.inntektsmeldingStatus,
                        innsendingType = inntektsmelding.innsendingType,
                    )
                    dokumentkoblingService.settInntektsmeldingJobbTilBehandlet(inntektsmelding.id.value)
                }
            } catch (e: Exception) {
                "Feil ved behandling av inntektsmelding med forespoersel id ${inntektsmelding.forespoerselId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }
}

fun DialogportenService.opprettTransmissionForInntektsmelding(
    sykmeldingId: UUID,
    forespoerselId: UUID,
    inntektsmeldingId: UUID,
    orgnr: Orgnr,
    inntektsmeldingStatus: InntektsmeldingStatus,
    innsendingType: InnsendingType,
) {
    oppdaterDialogMedInntektsmelding(
        Inntektsmelding(
            forespoerselId = forespoerselId,
            sykmeldingId = sykmeldingId,
            innsendingId = inntektsmeldingId,
            orgnr = orgnr,
            status = inntektsmeldingStatus.tilDialogportenStatus(),
            kanal = innsendingType.tilKanal(),
        ),
    )
}

fun InntektsmeldingStatus.tilDialogportenStatus(): Inntektsmelding.Status =
    when (this) {
        InntektsmeldingStatus.GODKJENT -> Inntektsmelding.Status.GODKJENT
        InntektsmeldingStatus.AVVIST -> Inntektsmelding.Status.FEILET
    }

fun InnsendingType.tilKanal() =
    when (this) {
        InnsendingType.FORESPURT_EKSTERN -> Inntektsmelding.Kanal.HR_SYSTEM_API
        InnsendingType.FORESPURT, InnsendingType.ARBEIDSGIVER_INITIERT -> Inntektsmelding.Kanal.NAV_NO
    }
