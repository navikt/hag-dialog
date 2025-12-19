package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import java.time.Duration
import java.time.LocalDateTime

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

class AvbrytForespoerselJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofMinutes(1).toMillis()) {
    override fun doJob() {
        val antallAvbrutteForespoersler =
            dokumentkoblingRepository.settForespoerslerMedStatusMottattTilTidsavbrutt(
                tidsavbruddgrense = LocalDateTime.now().minusMinutes(ANTALL_MINUTTER_FOER_TIDSAVBRUDD),
            )
        logger.info("Satte $antallAvbrutteForespoersler forespørsler til status TIDSAVBRUTT")
    }
}
