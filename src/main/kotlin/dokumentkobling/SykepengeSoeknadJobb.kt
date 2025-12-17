package dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.metrikk.oppdaterMetrikkForAntallSykepengesoeknaderMedStatusMottatt
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration

class SykepengeSoeknadJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val dialogportenService: DialogportenService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        if (!unleashFeatureToggles.skalOppretteDialoger()) {
            logger.warn("Oppretter ikke dialoger for sykepengesøknader da det er deaktivert i Unleash.")
            return
        }
        val soeknader = dokumentkoblingRepository.henteSykepengeSoeknaderMedStatusMottatt()

        oppdaterMetrikkForAntallSykepengesoeknaderMedStatusMottatt(nyVerdi = soeknader.size)
            .also { logger.info("Fant ${soeknader.size} sykepengesøknader med status MOTTATT klar til behandling.") }

        soeknader.forEach { soeknad ->
            try {
                val sykmelding = dokumentkoblingRepository.hentSykmeldingEntitet(soeknad.sykmeldingId)

                if (sykmelding?.status == Status.BEHANDLET) {
                    dialogportenService.opprettTransmissionForSoeknad(soeknad)
                    dokumentkoblingRepository.settSykepengeSoeknadJobbTilBehandlet(soeknad.soeknadId)
                } else {
                    logger.info(
                        "Sykmelding med id ${soeknad.sykmeldingId} er ikke behandlet enda, kan ikke sende søknad med id ${soeknad.soeknadId} til Dialogporten.",
                    )
                }
            } catch (e: Exception) {
                "Feil ved behandling av søknad med id ${soeknad.soeknadId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }
}

fun DialogportenService.opprettTransmissionForSoeknad(soeknad: Sykepengesoeknad) {
    oppdaterDialogMedSykepengesoeknad(
        no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad(
            soeknadId = soeknad.soeknadId,
            sykmeldingId = soeknad.sykmeldingId,
            orgnr = soeknad.orgnr,
        ),
    )
}
