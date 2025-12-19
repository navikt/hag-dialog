package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import java.time.Duration
import java.time.LocalDateTime

private const val ANTALL_MINUTTER_MELLOM_KJOERINGER = 1L
private const val ANTALL_MINUTTER_FOER_TIDSAVBRUDD = 60L

abstract class AvbrytDokumentJobb(
    private val dokumentNavn: String,
    private val antallMinutterFoerTidsavbrudd: Long,
    private val settTilTidsavbrutt: (LocalDateTime) -> Int,
) : RecurringJob(
        coroutineScope = CoroutineScope(context = Dispatchers.IO),
        waitMillisBetweenRuns = Duration.ofMinutes(ANTALL_MINUTTER_MELLOM_KJOERINGER).toMillis(),
    ) {
    override fun doJob() {
        val tidsavbruddgrense = LocalDateTime.now().minusMinutes(antallMinutterFoerTidsavbrudd)
        val antallAvbrutte = settTilTidsavbrutt(tidsavbruddgrense)
        logger.info("Satte $antallAvbrutte $dokumentNavn til status TIDSAVBRUTT")
    }
}

class AvbrytForespoerselJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "forespørsler",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settForespoerslerMedStatusMottattTilTidsavbrutt,
    )

class AvbrytInntektsmeldingJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "inntektsmeldinger",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settInntektsmeldingerMedStatusMottattTilTidsavbrutt,
    )

class AvbrytSykepengeSoeknadJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "sykepengesøknader",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settSykepengeSoeknaderMedStatusMottattTilTidsavbrutt,
    )

class AvbrytSykmeldingJobb(
    dokumentkoblingRepository: DokumentkoblingRepository,
) : AvbrytDokumentJobb(
        dokumentNavn = "sykmeldinger",
        antallMinutterFoerTidsavbrudd = ANTALL_MINUTTER_FOER_TIDSAVBRUDD,
        settTilTidsavbrutt = dokumentkoblingRepository::settSykmeldingerMedStatusMottattTilTidsavbrutt,
    )
