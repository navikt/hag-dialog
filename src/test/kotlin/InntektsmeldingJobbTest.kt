import dokumentkobling.DokumentkoblingService
import dokumentkobling.InnsendingType
import dokumentkobling.Status
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.InntektsmeldingStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.InntektsmeldingJobb
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import java.util.UUID

class InntektsmeldingJobbTest :
    FunSpec({

        val dialogportenService = mockk<DialogportenService>(relaxed = true)
        val dokumentkoblingService = mockk<DokumentkoblingService>(relaxed = true)
        val unleashFeatureToggles = UnleashFeatureToggles()
        val innteksmeldingResultat =
            DokumentkoblingRepository.InntektsmeldingResultat(
                inntektsmeldingId = DokumentKoblingMockUtils.inntektsmeldingId,
                forespoerselId = DokumentKoblingMockUtils.forespoerselId,
                vedtaksperiodeId = DokumentKoblingMockUtils.vedtaksperiodeId,
                inntektsmeldingStatus = InntektsmeldingStatus.GODKJENT,
                innsendingType = InnsendingType.FORESPURT_EKSTERN,
            )

        val inntektsmeldingJobb =
            InntektsmeldingJobb(
                dokumentkoblingService = dokumentkoblingService,
                dialogportenService = dialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            )

        val sykmeldingId: UUID = DokumentKoblingMockUtils.sykmeldingId

        beforeTest {
            clearAllMocks()
            every { dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt() } returns
                listOf(
                    innteksmeldingResultat,
                )
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

        test("inntektsmeldingJobb skal opprette transmission for inntektsmelding #2 selv om inntektsmelding #1 feiler") {
            val exceptionInntektsmelding = innteksmeldingResultat.copy(inntektsmeldingId = UUID.randomUUID())

            every {
                dialogportenService.oppdaterDialogMedInntektsmelding(
                    match { it.innsendingId == exceptionInntektsmelding.inntektsmeldingId },
                )
            } throws
                NotFoundException("Feil ved henting")
            every { dokumentkoblingService.hentInntektsmeldingerMedStatusMottatt() } returns
                listOf(exceptionInntektsmelding, innteksmeldingResultat)
            inntektsmeldingJobb.doJob()
            verify(exactly = 2) { dokumentkoblingService.hentKoblingMedForespoerselId(DokumentKoblingMockUtils.forespoerselId) }
            verify(exactly = 2) { dialogportenService.oppdaterDialogMedInntektsmelding(match { it.sykmeldingId == sykmeldingId }) }
            verify(exactly = 1) { dokumentkoblingService.settInntektsmeldingJobbTilBehandlet(DokumentKoblingMockUtils.inntektsmeldingId) }
        }
    })
