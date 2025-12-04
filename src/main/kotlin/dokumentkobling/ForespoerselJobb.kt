package no.nav.helsearbeidsgiver.dokumentkobling

import dokumentkobling.ForespoerselUtgaatt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.database.ForespoerselTable.forespoerselStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration
import java.util.UUID

class ForespoerselJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        val forespoersler = dokumentkoblingRepository.hentForespoerslerMedStatusMottattKlarForBehandling()
        val forespoerslerGruppert = forespoersler.groupBy { it.vedtaksperiodeId }

        forespoerslerGruppert.forEach { (_, forespoersler) ->

            try {
                forespoersler.forEach { forespoersel ->
                    dialogportenService.opprettTransmissionForForespoersel(forespoersel)
                }
            } catch (e: Exception) {
                "Feil ved behandling av forespørsel for vedtaksperiode ${forespoersler.first().vedtaksperiodeId}".also {
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
            logger().error(
                "Mottok utgått forespørsel for forespørselId ${forespoerselSykmeldingKobling.forespoerselId} i en jobb som kan kun håndtere ForespoerselStatus.SENDT.",
            )
        }
    }
}

fun DialogportenService.opprettTransmissionForForespoerselUtgaatt(
    forespoerselSendt: ForespoerselUtgaatt,
    sykmeldingId: UUID,
) {
    oppdaterDialogMedUtgaattForespoersel(
        UtgaattInntektsmeldingForespoersel(
            forespoerselId = forespoerselSendt.forespoerselId,
            sykmeldingId = sykmeldingId,
            orgnr = forespoerselSendt.orgnr,
        ),
    )
}
