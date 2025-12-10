import dokumentkobling.SykmeldingJobb
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import java.util.UUID

class SykmeldingJobbTest :
    FunSpec({

        val repository = mockk<DokumentkoblingRepository>()
        val dialogportenService = mockk<DialogportenService>(relaxed = true)

        val sykmeldingJobb =
            SykmeldingJobb(
                dokumentkoblingRepository = repository,
                dialogportenService = dialogportenService,
            )

        val sykmeldingId: UUID = DokumentKoblingMock.sykmelding.sykmeldingId

        beforeTest {
            clearAllMocks()
            every { repository.henteSykemeldingerMedStatusMottatt() } returns listOf(DokumentKoblingMock.sykmelding)
            every { dialogportenService.opprettOgLagreDialog(any()) } just runs
            every { repository.settSykmeldingJobbTilBehandlet(any()) } just runs
        }

        test("sykmeldingjobb henter sykmelding med status mottatt, oppretter dialog og setter status til BEHANDLET") {

            sykmeldingJobb.doJob()

            verify(exactly = 1) { repository.henteSykemeldingerMedStatusMottatt() }
            verify(exactly = 1) { dialogportenService.opprettOgLagreDialog(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { repository.settSykmeldingJobbTilBehandlet(sykmeldingId) }
        }

        test("sykmeldingjobb skal oprette dialog for sykmelding #2 selv om sykmelding #1 feiler") {

            val exceptionSykmeldingId = UUID.randomUUID()
            val exceptionSykmelding = DokumentKoblingMock.sykmelding.copy(exceptionSykmeldingId)
            every { dialogportenService.opprettOgLagreDialog(match { it.sykmeldingId == exceptionSykmeldingId }) } throws
                NotFoundException("Feil ved henting")

            every { repository.henteSykemeldingerMedStatusMottatt() } returns
                listOf(
                    exceptionSykmelding,
                    DokumentKoblingMock.sykmelding,
                )

            sykmeldingJobb.doJob()
            verify(exactly = 1) { repository.henteSykemeldingerMedStatusMottatt() }
            verify(exactly = 2) { dialogportenService.opprettOgLagreDialog(any()) }
            verify(exactly = 1) { repository.settSykmeldingJobbTilBehandlet(sykmeldingId) }
        }
    })
