package no.nav.helsearbeidsgiver.dokumentkobling

import dokumentkobling.DokumentkoblingService
import dokumentkobling.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration

// TODO: Lag tester for denne jobben
class InntektsmeldingJobb(
    private val dokumentkoblingService: DokumentkoblingService,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        val inntektsmeldinger = dokumentkoblingService.hentInntektsmeldingerMedStatusMotatt()
        inntektsmeldinger.forEach { inntektsmelding ->
            try {
                val kobling = dokumentkoblingService.hentKoblingMedForespoerselId(inntektsmelding.forespoerselId)
                if (kobling?.forespoerselJobbStatus == Status.BEHANDLET) {
                    dialogportenService.oppdaterDialogMedInntektsmelding(
                        forespoerselId = kobling.forespoerselId,
                        sykmeldingId = kobling.sykmeldingId,
                    )
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
