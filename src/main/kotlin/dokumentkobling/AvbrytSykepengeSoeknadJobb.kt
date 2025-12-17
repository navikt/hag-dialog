package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import java.time.Duration
import java.time.LocalDateTime

private const val ANTALL_MINUTTER_FOER_TIDSAVBRUTT = 10L

class AvbrytSykepengeSoeknadJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofMinutes(1).toMillis()) {
    override fun doJob() {
        val antallAvbrutteSoeknader =
            dokumentkoblingRepository.tidsavbrytSykepengeSoeknaderMedStatusMottattOpprettetFoer(
                opprettetFoer = LocalDateTime.now().minusMinutes(ANTALL_MINUTTER_FOER_TIDSAVBRUTT),
            )
        logger.info("Satt $antallAvbrutteSoeknader til status TIDSAVBRUTT")
    }
}
