import dokumentkobling.Status
import dokumentkobling.SykepengeSoeknadJobb
import dokumentkobling.opprettTransmissionForSoeknad
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import java.util.UUID

class SykepengeSoeknadJobbTest :
    FunSpec({

        val repository = mockk<DokumentkoblingRepository>()
        val dialogportenService = mockk<DialogportenService>(relaxed = true)

        val sykepengeSoeknadJobb =
            SykepengeSoeknadJobb(
                dokumentkoblingRepository = repository,
                dialogportenService = dialogportenService,
            )

        val sykmeldingId: UUID = dokumentkoblingSykmelding.sykmeldingId
        val soeknadId: UUID = dokumentkoblingSoeknad.soeknadId

        val sykmeldingEntity =
            mockk<SykmeldingEntity> {
                every { this@mockk.sykmeldingId } returns sykmeldingId
                every { data } returns dokumentkoblingSykmelding
            }

        beforeTest {
            clearAllMocks()
            every { repository.settSykepengeSoeknadJobbTilBehandlet(any()) } just runs
            every { repository.henteSykepengeSoeknaderMedStatusMottatt() } returns listOf(dokumentkoblingSoeknad)
            every { repository.hentSykmeldingEntitet(sykmeldingId) } returns sykmeldingEntity
            every { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) } just runs
        }

        test("sykepengesoeknadJobb skal opprette transmission når sykmelding er behandlet") {

            every { sykmeldingEntity.status } returns Status.BEHANDLET

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMottatt() }
            verify(exactly = 1) { dialogportenService.oppdaterDialogMedSykepengesoeknad(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal ikke opprette transmission når sykmelding ikke er behandlet") {

            every { sykmeldingEntity.status } returns Status.MOTTATT

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMottatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal ikke opprette transmission når sykmelding ikke eksisterer") {

            every { repository.hentSykmeldingEntitet(sykmeldingId) } returns null

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMottatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId) }
        }

        test("sykepengesoeknadJobb skal fortsatt opprette transmission på søknad #2 når henting av søknad #1 kaster exception") {

            every { sykmeldingEntity.status } returns Status.BEHANDLET

            val exceptionSoeknad = dokumentkoblingSoeknad.copy(soeknadId = UUID.randomUUID())

            every { repository.henteSykepengeSoeknaderMedStatusMottatt() } returns listOf(exceptionSoeknad, dokumentkoblingSoeknad)
            every { dialogportenService.opprettTransmissionForSoeknad(exceptionSoeknad) } throws
                NotFoundException("Fant ikke sykmelding for søknad")

            sykepengeSoeknadJobb.doJob()

            verify(exactly = 1) { repository.henteSykepengeSoeknaderMedStatusMottatt() }
            verify(exactly = 2) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 1) { repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId) }
        }
    })
