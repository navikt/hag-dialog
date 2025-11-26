package no.nav.helsearbeidsgiver.dokumentKobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration
import no.nav.helsearbeidsgiver.kafka.Sykmelding as SykmeldingGammel
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode as SykmeldingSperiodeGammel

class SykmeldingJobb(
    private val dokumentKoblingRepository: DokumentKoblingRepository,
    private val dialogportenService: DialogportenService,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(10).toMillis()) {
    override fun doJob() {
        val sykmeldinger = dokumentKoblingRepository.henteSykemeldingerMedStatusMottatt()
        sykmeldinger.forEach { (sykmelding, status) ->
            try {
                dialogportenService.opprettDialogForSykmelding(sykmelding)
                dokumentKoblingRepository.settSykmeldingStatusTilBehandlet(sykmelding.sykmeldingId)
            } catch (e: Exception) {
                "Feil ved behandling av sykmelding med id ${sykmelding.sykmeldingId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }
}

fun DialogportenService.opprettDialogForSykmelding(sykmelding: Sykmelding) {
    opprettOgLagreDialog(
        SykmeldingGammel(
            sykmeldingId = sykmelding.sykmeldingId,
            foedselsdato = sykmelding.foedselsdato,
            fulltNavn = sykmelding.fulltNavn,
            orgnr = sykmelding.orgnr,
            sykmeldingsperioder = sykmelding.sykmeldingsperioder.map { SykmeldingSperiodeGammel(it.fom, it.tom) },
        ),
    )
}
