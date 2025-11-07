import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.dialogporten.toExtendedType
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenServiceTest :
    FunSpec({
        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val dialogportenService =
            DialogportenService(
                dialogRepositoryMock,
                dialogportenClientMock,
                unleashFeatureTogglesMock,
            )

        beforeTest {
            clearAllMocks()
        }

        context("opprettOgLagreDialog") {
            test("skal opprette og lagre dialog med riktige data") {
                val dialogId = UUID.randomUUID()
                val requestSlot = slot<CreateDialogRequest>()

                coEvery { dialogportenClientMock.createDialog(capture(requestSlot)) } returns dialogId
                every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs
                every { unleashFeatureTogglesMock.skalOppretteDialogKunForApi() } returns true

                dialogportenService.opprettOgLagreDialog(sykmelding)

                val capturedRequest = requestSlot.captured
                capturedRequest.orgnr shouldBe sykmelding.orgnr
                capturedRequest.externalReference shouldBe sykmelding.sykmeldingId.toString()
                capturedRequest.title shouldBe "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
                capturedRequest.summary shouldBe sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString()
                capturedRequest.isApiOnly shouldBe true

                verify(exactly = 1) {
                    dialogRepositoryMock.lagreDialog(
                        dialogId = dialogId,
                        sykmeldingId = sykmelding.sykmeldingId,
                    )
                }
            }
        }

        context("oppdaterDialogMedSykepengesoeknad") {
            test("skal oppdatere dialog med sykepengesøknad") {
                val dialogId = UUID.randomUUID()
                val transmissionId = UUID.randomUUID()
                val dialogEntity =
                    mockk<DialogEntity> {
                        every { this@mockk.dialogId } returns dialogId
                    }

                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
                coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
                every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

                dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

                verify(exactly = 1) { dialogRepositoryMock.finnDialogIdMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
                coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any()) }
                verify(exactly = 1) {
                    dialogRepositoryMock.oppdaterDialogMedTransmission(
                        sykmeldingId = sykepengesoeknad.sykmeldingId,
                        transmissionId = transmissionId,
                        dokumentId = sykepengesoeknad.soeknadId,
                        dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
                    )
                }
            }

            test("skal ikke oppdatere dialog når dialog ikke finnes") {
                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns null

                dialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

                verify(exactly = 1) { dialogRepositoryMock.finnDialogIdMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
                coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
                verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
            }
        }

        context("oppdaterDialogMedInntektsmeldingsforespoersel") {
            test("skal oppdatere dialog med inntektsmeldingsforespørsel og legge til action") {
                val dialogId = UUID.randomUUID()
                val transmissionId = UUID.randomUUID()
                val dialogEntity =
                    mockk<DialogEntity> {
                        every { this@mockk.dialogId } returns dialogId
                    }

                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) } returns dialogEntity
                coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
                coEvery { dialogportenClientMock.addAction(any(), any(), any<GuiAction>()) } just Runs
                every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

                dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

                coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any()) }
                coVerify(exactly = 1) { dialogportenClientMock.addAction(dialogId, any<ApiAction>(), any<GuiAction>()) }
                verify(exactly = 1) {
                    dialogRepositoryMock.oppdaterDialogMedTransmission(
                        sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId,
                        transmissionId = transmissionId,
                        dokumentId = inntektsmeldingsforespoersel.forespoerselId,
                        dokumentType = LpsApiExtendedType.FORESPOERSEL_AKTIV.toString(),
                        relatedTransmission = transmissionId,
                    )
                }
            }

            test("skal ikke oppdatere dialog når dialog ikke finnes") {
                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) } returns null

                dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

                verify(exactly = 1) { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) }
                coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
                coVerify(exactly = 0) { dialogportenClientMock.addAction(any(), any(), any<GuiAction>()) }
            }
        }

        context("oppdaterDialogMedInntektsmelding") {
            test("skal oppdatere dialog med inntektsmelding") {
                val dialogId = UUID.randomUUID()
                val forespoerselTransmissionId = UUID.randomUUID()
                val transmissionId = UUID.randomUUID()
                val forespoerselTransmission =
                    mockk<TransmissionEntity> {
                        every { relatedTransmission } returns forespoerselTransmissionId
                    }
                val dialogEntity =
                    mockk<DialogEntity> {
                        every { this@mockk.dialogId } returns dialogId
                        every { transmissionByDokumentId(inntektsmelding_mottatt.forespoerselId) } returns forespoerselTransmission
                    }

                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmelding_mottatt.sykmeldingId) } returns dialogEntity
                coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
                every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

                dialogportenService.oppdaterDialogMedInntektsmelding(inntektsmelding_mottatt)

                coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any()) }
                verify(exactly = 1) {
                    dialogRepositoryMock.oppdaterDialogMedTransmission(
                        sykmeldingId = inntektsmelding_mottatt.sykmeldingId,
                        transmissionId = transmissionId,
                        dokumentId = inntektsmelding_mottatt.forespoerselId,
                        dokumentType = inntektsmelding_mottatt.status.toExtendedType(),
                        relatedTransmission = inntektsmelding_mottatt.forespoerselId,
                    )
                }
            }

            test("skal ikke oppdatere når dialog ikke finnes") {
                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmelding_mottatt.sykmeldingId) } returns null

                dialogportenService.oppdaterDialogMedInntektsmelding(inntektsmelding_mottatt)

                verify(exactly = 1) { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmelding_mottatt.sykmeldingId) }
                coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
            }

            test("skal ikke oppdatere når forespørsel transmission ikke finnes") {
                val dialogId = UUID.randomUUID()
                val dialogEntity =
                    mockk<DialogEntity> {
                        every { this@mockk.dialogId } returns dialogId
                        every { transmissionByDokumentId(inntektsmelding_mottatt.forespoerselId) } returns null
                    }

                every { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmelding_mottatt.sykmeldingId) } returns dialogEntity

                dialogportenService.oppdaterDialogMedInntektsmelding(inntektsmelding_mottatt)

                verify(exactly = 1) { dialogRepositoryMock.finnDialogIdMedSykemeldingId(inntektsmelding_mottatt.sykmeldingId) }
                coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
                verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
            }
        }
    })
