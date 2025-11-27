package dialogporten.handlers

import inntektsmelding_godkjent
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
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.handlers.InntektsmeldingHandler
import no.nav.helsearbeidsgiver.dialogporten.toExtendedType
import java.util.UUID

class InntektsmeldingHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()

        val inntektsmeldingHandler =
            InntektsmeldingHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
            )

        beforeTest {
            clearAllMocks()
        }

        test("skal oppdatere dialog med ny inntektsmelding") {
            val dialogId = UUID.randomUUID()
            val forespoerselTransmissionId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val forespoerselTransmission =
                mockk<TransmissionEntity> {
                    every { relatedTransmissionId } returns forespoerselTransmissionId
                }
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(inntektsmelding_godkjent.forespoerselId) } returns forespoerselTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_godkjent.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_godkjent)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any()) }
            coVerify(exactly = 1) { dialogportenClientMock.setDialogStatus(dialogId, DialogStatus.NotApplicable) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = inntektsmelding_godkjent.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = inntektsmelding_godkjent.innsendingId,
                    dokumentType = inntektsmelding_godkjent.status.toExtendedType(),
                    relatedTransmissionId = forespoerselTransmissionId,
                )
            }
        }

        test("skal ikke oppdatere når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_godkjent.sykmeldingId) } returns null

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_godkjent)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_godkjent.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
        }

        test("skal ikke oppdatere når forespørsel transmission ikke finnes") {
            val dialogId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(inntektsmelding_godkjent.forespoerselId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_godkjent.sykmeldingId) } returns dialogEntity

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_godkjent)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_godkjent.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }
    })
