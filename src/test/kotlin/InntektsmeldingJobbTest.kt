import dokumentkobling.DokumentkoblingService
import dokumentkobling.Status
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.InntektsmeldingEntity
import no.nav.helsearbeidsgiver.database.InntektsmeldingStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.InntektsmeldingJobb
import java.util.UUID

class InntektsmeldingJobbTest :
    FunSpec({

        val dialogportenService = mockk<DialogportenService>(relaxed = true)
        val dokumentkoblingService = mockk<DokumentkoblingService>(relaxed = true)

        val inntektsmeldingJobb =
            InntektsmeldingJobb(
                dokumentkoblingService = dokumentkoblingService,
                dialogportenService = dialogportenService,
            )

        val sykmeldingId: UUID = DokumentKoblingMockUtils.sykmeldingId

        beforeTest {
            clearAllMocks()
            val inntektsmeldingEntity =
                mockk<InntektsmeldingEntity> {
                    every { forespoerselId } returns DokumentKoblingMockUtils.forespoerselId
                    every { id.value } returns DokumentKoblingMockUtils.inntektsmeldingId
                    every { innsendingType } returns DokumentKoblingMockUtils.inntektsmeldingGodkjent.innsendingType
                    every { inntektsmeldingStatus } returns InntektsmeldingStatus.GODKJENT
                }
            every { dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt() } returns listOf(inntektsmeldingEntity)
            every { dokumentkoblingService.hentKoblingMedForespoerselId(DokumentKoblingMockUtils.forespoerselId) } returns
                DokumentKoblingMockUtils.forespoerselSykmeldingKobling
            every { dokumentkoblingService.hentSykmeldingOrgnr(sykmeldingId) } returns DokumentKoblingMockUtils.orgnr
            every { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) } just runs
        }

        test("inntektsmeldingJobb skal opprette transmission når forespoersel er behandlet") {
            inntektsmeldingJobb.doJob()
            verify(exactly = 1) { dokumentkoblingService.hentKoblingMedForespoerselId(DokumentKoblingMockUtils.forespoerselId) }
            verify(exactly = 1) { dialogportenService.oppdaterDialogMedInntektsmelding(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { dokumentkoblingService.settInntektsmeldingJobbTilBehandlet(DokumentKoblingMockUtils.inntektsmeldingId) }
        }

        test("inntektsmeldingJobb skal ikke opprette transmission når forespoersel ikke er behandlet") {
            every { dokumentkoblingService.hentKoblingMedForespoerselId(DokumentKoblingMockUtils.forespoerselId) } returns
                DokumentKoblingMockUtils.forespoerselSykmeldingKobling.copy(forespoerselJobbStatus = Status.MOTTATT)

            inntektsmeldingJobb.doJob()

            verify(exactly = 1) { dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { dokumentkoblingService.settInntektsmeldingJobbTilBehandlet(DokumentKoblingMockUtils.inntektsmeldingId) }
        }

        test("inntektsmeldingJobb skal ikke opprette transmission når forespoersel ikke eksisterer") {

            every { dokumentkoblingService.hentKoblingMedForespoerselId(DokumentKoblingMockUtils.forespoerselId) } returns null

            inntektsmeldingJobb.doJob()

            verify(exactly = 1) { dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedSykepengesoeknad(any()) }
            verify(exactly = 0) { dokumentkoblingService.settInntektsmeldingJobbTilBehandlet(DokumentKoblingMockUtils.inntektsmeldingId) }
        }
    })
