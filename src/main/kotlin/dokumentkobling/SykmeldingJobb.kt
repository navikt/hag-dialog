package dokumentkobling

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.metrikk.oppdaterMetrikkForAntallSykmeldingerMedStatusMottatt
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.Duration
import no.nav.helsearbeidsgiver.kafka.Sykmelding as SykmeldingGammel
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode as SykmeldingSperiodeGammel

class SykmeldingJobb(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val dialogportenService: DialogportenService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        if (!unleashFeatureToggles.skalOppretteDialoger()) {
            logger.warn("Oppretter ikke dialoger for sykmeldinger da det er deaktivert i Unleash.")
            return
        }
        val sykmeldinger = dokumentkoblingRepository.henteSykemeldingerMedStatusMottatt()

        oppdaterMetrikkForAntallSykmeldingerMedStatusMottatt(nyVerdi = sykmeldinger.size)
            .also { logger.info("Fant ${sykmeldinger.size} sykmeldinger med status MOTTATT klar til behandling.") }

        sykmeldinger.forEach { sykmelding ->
            try {
                dialogportenService.opprettDialogForSykmelding(sykmelding)
                dokumentkoblingRepository.settSykmeldingJobbTilBehandlet(sykmelding.sykmeldingId)
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
