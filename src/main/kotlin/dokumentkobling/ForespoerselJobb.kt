package no.nav.helsearbeidsgiver.dokumentkobling

import dokumentkobling.DokumentkoblingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.metrikk.oppdaterMetrikkForAntallForespoerslerMedStatusMottatt
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration

class ForespoerselJobb(
    private val dokumentkoblingService: DokumentkoblingService,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

        oppdaterMetrikkForAntallForespoerslerMedStatusMottatt(nyVerdi = forespoersler.size)
            .also { logger.info("Fant ${forespoersler.size} forespoersler med status MOTTATT klar til behandling.") }

        forespoersler
            .groupBy { it.vedtaksperiodeId }
            .forEach { (vedtaksperiodeId, forespoersler) ->
                try {
                    forespoersler.forEach { forespoersel ->
                        dialogportenService.opprettTransmissionForForespoersel(forespoersel)
                        dokumentkoblingService.settForespoerselJobbTilBehandlet(forespoerselId = forespoersel.forespoerselId)
                    }
                } catch (e: Exception) {
                    "Feil ved behandling av forespørsler ${forespoersler.map { it.forespoerselId }} for vedtaksperiode $vedtaksperiodeId"
                        .also {
                            logger.error(it)
                            sikkerLogger().error(it, e)
                        }
                }
            }
    }
}

fun DialogportenService.opprettTransmissionForForespoersel(
    forespoerselSykmeldingKobling: DokumentkoblingRepository.ForespoerselSykmeldingKobling,
) {
    when (forespoerselSykmeldingKobling.forespoerselStatus) {
        ForespoerselStatus.SENDT -> {
            oppdaterDialogMedInntektsmeldingsforespoersel(
                forespoerselId = forespoerselSykmeldingKobling.forespoerselId,
                sykmeldingId = forespoerselSykmeldingKobling.sykmeldingId,
            )
        }

        ForespoerselStatus.UTGAATT -> {
            oppdaterDialogMedUtgaattForespoersel(
                forespoerselId = forespoerselSykmeldingKobling.forespoerselId,
                sykmeldingId = forespoerselSykmeldingKobling.sykmeldingId,
            )
        }
    }
}
