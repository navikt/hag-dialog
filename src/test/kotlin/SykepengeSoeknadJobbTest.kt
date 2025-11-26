import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
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
import no.nav.helsearbeidsgiver.dokumentKobling.opprettTransmissionForSoeknad
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

        val sykmeldingId: UUID = dokumentKoblingSykmelding.sykmeldingId
        val soeknadId: UUID = dokumentKoblingSoeknad.soeknadId

        val sykmeldingEntity =
            mockk<SykmeldingEntity> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { data } returns dokumentKoblingSykmelding
            }

        beforeTest {
            clearAllMocks()
            every { repository.settSykepengeSoeknadStatusTilBehandlet(any()) } just runs
            every { repository.henteSykepengeSoeknaderMedStatusMotatt() } returns listOf(dokumentKoblingSoeknad)
            every { repository.hentSykmelding(sykmeldingId) } returns sykmeldingEntity
            every { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) } just runs
        }

        test("sykepengesoeknadJobb skal opprette transmission når sykmelding er behandlet") {

            every { sykmeldingEntity.status } returns Status.BEHANDLET

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMotatt() }
            verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoeknad(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { repository.settSykepengeSoeknadStatusTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal ikke opprette transmission når sykmelding ikke er behandlet") {

            every { sykmeldingEntity.status } returns Status.MOTATT

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMotatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { repository.settSykepengeSoeknadStatusTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal ikke opprette transmission når sykmelding ikke eksisterer") {

            every { repository.hentSykmelding(sykmeldingId) } returns null

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMotatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { repository.settSykepengeSoeknadStatusTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal fortsatt opprette transmission på søknad #2 når henting av søknad #1 kaster exception") {

            every { sykmeldingEntity.status } returns Status.BEHANDLET

            val exceptionSoeknad = dokumentKoblingSoeknad.copy(soeknadId = UUID.randomUUID())

            every { repository.henteSykepengeSoeknaderMedStatusMotatt() } returns listOf(exceptionSoeknad, dokumentKoblingSoeknad)
            every { dialogportenService.opprettTransmissionForSoeknad(exceptionSoeknad) } throws
                NotFoundException("Fant ikke sykmelding for søknad")

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMotatt() }
            verify(exactly = 2) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 1) { repository.settSykepengeSoeknadStatusTilBehandlet(soeknadId) }
        }
    })
