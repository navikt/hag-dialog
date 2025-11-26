import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.Status
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentKobling.SykepengeSoeknadJobb
import java.util.UUID

class SykepengeSoeknadJobbTest :
    FunSpec({
        val repository = mockk<DokumentKoblingRepository>()
        val dialogportenService = mockk<DialogportenService>(relaxed = true)

        val sykepengeSoeknadJobb =
            SykepengeSoeknadJobb(
                dokumentKoblingRepository = repository,
                dialogportenService = dialogportenService,
            )

        test("skal kjøre sykepengesoeknadJobb uten feil") {
            val sykmeldingId: UUID = dokumentKoblingSykmelding.sykmeldingId
            val soeknadId: UUID = dokumentKoblingSoeknad.soeknadId

            val sykmeldingEntity =
                mockk<SykmeldingEntity> {
                    every { this@mockk.sykmeldingId } returns sykmeldingId
                    every { status } returns Status.BEHANDLET
                    every { data } returns dokumentKoblingSykmelding
                }

            every { repository.settSykepengeSoeknadStatusTilBehandlet(any()) } just runs
            every { repository.henteSykepengeSoeknaderMedStatusMotatt() } returns listOf(dokumentKoblingSoeknad)
            every { repository.hentSykmelding(sykmeldingId) } returns sykmeldingEntity

            every { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) } just runs
            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMotatt() }
            verify(exactly = 1) { repository.settSykepengeSoeknadStatusTilBehandlet(soeknadId) }
            verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoeknad(match { it.sykmeldingId == sykmeldingId }) }
        }
    })
