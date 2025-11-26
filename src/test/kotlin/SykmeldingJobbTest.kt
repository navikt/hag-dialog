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
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentKobling.SykmeldingJobb
import java.util.UUID

class SykmeldingJobbTest :
    FunSpec({

        val repository = mockk<DokumentKoblingRepository>()
        val dialogportenService = mockk<DialogportenService>(relaxed = true)

        val sykmeldingJobb =
            SykmeldingJobb(
                dokumentKoblingRepository = repository,
                dialogportenService = dialogportenService,
            )

        val sykmeldingId: UUID = dokumentKoblingSykmelding.sykmeldingId
        val repositorySykmelding = Pair(dokumentKoblingSykmelding, Status.MOTTATT)

        beforeTest {
            clearAllMocks()
            every { repository.henteSykemeldingerMedStatusMottatt() } returns listOf(repositorySykmelding)
            every { dialogportenService.opprettOgLagreDialog(any()) } just runs
            every { repository.settSykmeldingStatusTilBehandlet(any()) } just runs
        }

        test("sykmeldingjobb henter sykmelding med status mottatt, oppretter dialog og setter status til BEHANDLET") {

            sykmeldingJobb.doJob()

            verify(exactly = 1) { repository.henteSykemeldingerMedStatusMottatt() }
            verify(exactly = 1) { dialogportenService.opprettOgLagreDialog(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { repository.settSykmeldingStatusTilBehandlet(sykmeldingId) }
        }

        test("sykmeldingjobb skal oprette dialog for sykmelding #2 selv om sykmelding #1 feiler") {

            val exceptionSykmeldingId = UUID.randomUUID()
            val exceptionSykmelding = Pair(dokumentKoblingSykmelding.copy(exceptionSykmeldingId), Status.MOTTATT)
            every { dialogportenService.opprettOgLagreDialog(match { it.sykmeldingId == exceptionSykmeldingId }) } throws
                NotFoundException("Feil ved henting")

            every { repository.henteSykemeldingerMedStatusMottatt() } returns listOf(exceptionSykmelding, repositorySykmelding)

            sykmeldingJobb.doJob()

            verify(exactly = 1) { repository.henteSykemeldingerMedStatusMottatt() }
            verify(exactly = 2) { dialogportenService.opprettOgLagreDialog(any()) }
            verify(exactly = 1) { repository.settSykmeldingStatusTilBehandlet(sykmeldingId) }
        }
    })
