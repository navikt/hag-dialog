package dialogporten.handlers

import inntektsmelding_mottatt_endring
import inntektsmelding_mottatt_ny
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
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
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
                    every { transmissionByDokumentId(inntektsmelding_mottatt_ny.forespoerselId) } returns forespoerselTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_ny.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_mottatt_ny)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any()) }
            coVerify(exactly = 1) { dialogportenClientMock.setDialogStatus(dialogId, DialogStatus.NotApplicable) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = inntektsmelding_mottatt_ny.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = inntektsmelding_mottatt_ny.innsendingId,
                    dokumentType = inntektsmelding_mottatt_ny.status.toExtendedType(),
                    relatedTransmissionId = forespoerselTransmissionId,
                )
            }
        }
        test("skal oppdatere dialog med korrigert inntektsmelding") {
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
                    every { transmissionByDokumentId(inntektsmelding_mottatt_endring.forespoerselId) } returns forespoerselTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_endring.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any()) } returns transmissionId
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_mottatt_endring)

            coVerify(exactly = 2) { dialogportenClientMock.addTransmission(dialogId, any()) }
            coVerify(exactly = 1) { dialogportenClientMock.setDialogStatus(dialogId, DialogStatus.NotApplicable) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = inntektsmelding_mottatt_endring.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = inntektsmelding_mottatt_endring.innsendingId,
                    dokumentType = LpsApiExtendedType.INNTEKTSMELDING_KORRIGERT.toString(),
                    relatedTransmissionId = forespoerselTransmissionId,
                )
            }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = inntektsmelding_mottatt_endring.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = inntektsmelding_mottatt_endring.innsendingId,
                    dokumentType = inntektsmelding_mottatt_endring.status.toExtendedType(),
                    relatedTransmissionId = forespoerselTransmissionId,
                )
            }
        }
        test("skal ikke oppdatere når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_ny.sykmeldingId) } returns null

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_mottatt_ny)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_ny.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
        }

        test("skal ikke oppdatere når forespørsel transmission ikke finnes") {
            val dialogId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(inntektsmelding_mottatt_ny.forespoerselId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_ny.sykmeldingId) } returns dialogEntity

            inntektsmeldingHandler.oppdaterDialog(inntektsmelding_mottatt_ny)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(inntektsmelding_mottatt_ny.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }
    })
