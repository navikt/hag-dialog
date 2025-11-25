package no.nav.helsearbeidsgiver.dokumentKobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import java.time.Duration

class SykmeldingJobb(
    private val dokumentKoblingRepository: DokumentKoblingRepository,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(10).toMillis()) {
    override fun doJob() {
        val sykmeldinger = dokumentKoblingRepository.henteSykemeldingerMedStatusMotatt()

        sykmeldinger.forEach { sykmelding ->
            dialogportenService.opprettOgLagreDialog(sykmelding.data)
            dokumentKoblingRepository.settSykmeldingStatusTilBehandlet(sykmelding.sykmeldingId)
        }
    }
}
