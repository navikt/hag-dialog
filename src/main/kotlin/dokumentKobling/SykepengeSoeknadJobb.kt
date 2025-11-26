package no.nav.helsearbeidsgiver.dokumentKobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.Status
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import java.time.Duration

class SykepengeSoeknadJobb(
    private val dokumentKoblingRepository: DokumentKoblingRepository,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(10).toMillis()) {
    override fun doJob() {
        val soeknader = dokumentKoblingRepository.henteSykepengeSoeknaderMedStatusMotatt()
        soeknader.forEach { soeknad ->
            val sykmelding = dokumentKoblingRepository.hentSykmelding(soeknad.sykmeldingId)
            if (sykmelding?.status == Status.BEHANDLET) {
                dialogportenService.opprettTransmissionForSoeknad(soeknad)
                dokumentKoblingRepository.settSykepengeSoeknadStatusTilBehandlet(soeknad.soeknadId)
            } else {
                logger.info(
                    "Sykmelding med id ${soeknad.sykmeldingId} er ikke behandlet enda, kan ikke sende søknad med id ${soeknad.soeknadId} til Dialogporten.",
                )
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
