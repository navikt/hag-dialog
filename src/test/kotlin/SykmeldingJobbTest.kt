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
import no.nav.helsearbeidsgiver.dokumentKobling.SykmeldingJobb
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import org.jetbrains.exposed.sql.transactions.transaction
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

        test("skal kjøre sykmeldingjobb uten feil") {
            val sykmeldingId: UUID = dokumentKoblingSykmelding.sykmeldingId

            val sykmelding =
                mockk<SykmeldingEntity> {
                    every { id.value} returns sykmeldingId
                    every { status } returns Status.MOTATT
                    every { data } returns dokumentKoblingSykmelding.copy(sykmeldingId = sykmeldingId)
                }

            every { repository.henteSykemeldingerMedStatusMotatt() } returns listOf(Pair(dokumentKoblingSykmelding,Status.MOTATT))
            every { repository.settSykmeldingStatusTilBehandlet(any()) } just runs

            every { dialogportenService.opprettOgLagreDialog(any()) } just runs
            sykmeldingJobb.doJob()

            verify(exactly = 1) { repository.henteSykemeldingerMedStatusMotatt() }
            verify(exactly = 1) { repository.settSykmeldingStatusTilBehandlet(sykmeldingId) }
            verify(exactly = 1) { dialogportenService.opprettOgLagreDialog(match { it.sykmeldingId == sykmeldingId }) }
        }
    })
