package dialogporten.handlers

import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.ForespoerselHandler
import java.util.UUID

class ForespoerselHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()

        val forespoerselHandler =
            ForespoerselHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
            )

        beforeTest {
            clearAllMocks()
        }
        test("skal oppdatere dialog med inntektsmeldingsforespørsel og legge til action") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            coEvery { dialogportenClientMock.addAction(any(), any(), any<GuiAction>()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            forespoerselHandler.oppdaterDialog(inntektsmeldingsforespoersel)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addAction(dialogId, any<ApiAction>(), any<GuiAction>()) }

            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = inntektsmeldingsforespoersel.forespoerselId,
                    dokumentType = LpsApiExtendedType.FORESPOERSEL_AKTIV.toString(),
                    relatedTransmissionId = transmissionId,
                )
            }
        }

        test("skal ikke oppdatere dialog når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) } returns null

            forespoerselHandler.oppdaterDialog(inntektsmeldingsforespoersel)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmeldingsforespoersel.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            coVerify(exactly = 0) { dialogportenClientMock.addAction(any(), any(), any<GuiAction>()) }
        }
    })
