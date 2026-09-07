package dialogporten.handlers

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
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.VedtakHandler
import vedtak
import java.util.UUID

class VedtakHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()

        val vedtakHandler =
            VedtakHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
            )

        beforeTest {
            clearAllMocks()
        }

        test("skal oppdatere dialog med vedtak når inntektsmelding-transmission finnes") {
            val dialogId = UUID.randomUUID()
            val inntektsmeldingTransmissionId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val inntektsmeldingTransmission =
                mockk<TransmissionEntity> {
                    every { relatedTransmissionId } returns inntektsmeldingTransmissionId
                }
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(vedtak.vedtakId) } returns null
                    every { transmissionByDokumentId(vedtak.inntektsmeldingId) } returns inntektsmeldingTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            vedtakHandler.oppdaterDialog(vedtak)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = vedtak.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = vedtak.vedtakId,
                    dokumentType = LpsApiExtendedType.VEDTAK.toString(),
                    relatedTransmissionId = inntektsmeldingTransmissionId,
                )
            }
        }

        test("skal ikke oppdatere når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) } returns null

            vedtakHandler.oppdaterDialog(vedtak)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
        }

        test("skal ikke opprette transmission når den allerede finnes for vedtaket") {
            val dialogId = UUID.randomUUID()
            val eksisterendeTransmission = mockk<TransmissionEntity>()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(vedtak.vedtakId) } returns eksisterendeTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) } returns dialogEntity

            vedtakHandler.oppdaterDialog(vedtak)

            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }

        test("skal opprette transmission uten relatedTransmissionId når inntektsmelding-transmission ikke finnes enda") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(vedtak.vedtakId) } returns null
                    every { transmissionByDokumentId(vedtak.inntektsmeldingId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            vedtakHandler.oppdaterDialog(vedtak)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(vedtak.sykmeldingId) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = vedtak.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = vedtak.vedtakId,
                    dokumentType = LpsApiExtendedType.VEDTAK.toString(),
                    relatedTransmissionId = null,
                )
            }
        }
    })
