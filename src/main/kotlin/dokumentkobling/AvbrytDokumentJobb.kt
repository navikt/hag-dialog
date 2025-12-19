package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import java.time.Duration
import java.time.LocalDateTime

private const val ANTALL_MINUTTER_MELLOM_KJOERINGER = 1L

abstract class AvbrytDokumentJobb(
    private val dokumentNavn: String,
    private val antallMinutterFoerTidsavbrudd: Long,
    private val settTilTidsavbrutt: (LocalDateTime) -> Int,
) : RecurringJob(
        coroutineScope = CoroutineScope(context = Dispatchers.IO),
        waitMillisBetweenRuns = Duration.ofMinutes(ANTALL_MINUTTER_MELLOM_KJOERINGER).toMillis(),
    ) {
    override fun doJob() {
        val antallAvbrutte = settTilTidsavbrutt(LocalDateTime.now().minusMinutes(antallMinutterFoerTidsavbrudd))
        logger.info("Satte $antallAvbrutte $dokumentNavn til status TIDSAVBRUTT")
    }
}
