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
import no.nav.helsearbeidsgiver.dialogporten.handlers.RefusjonHandler
import refusjon
import java.util.UUID

class RefusjonHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()

        val refusjonHandler =
            RefusjonHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
            )

        beforeTest {
            clearAllMocks()
        }

        test("skal oppdatere dialog for refusjonens sykmelding uten kobling til inntektsmelding") {
            val dialogId = UUID.randomUUID()
            val refusjonTransmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(refusjon.refusjonUtfallId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns refusjonTransmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            refusjonHandler.oppdaterDialog(refusjon)

            coVerify(exactly = 1) {
                dialogportenClientMock.addTransmission(dialogId, match<TransmissionRequest> { it.relatedTransmissionId == null })
            }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = refusjon.sykmeldingId,
                    transmissionId = refusjonTransmissionId,
                    dokumentId = refusjon.refusjonUtfallId,
                    dokumentType = LpsApiExtendedType.REFUSJON.toString(),
                    relatedTransmissionId = null,
                )
            }
        }

        test("skal ikke oppdatere når dialog ikke finnes") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) } returns null

            refusjonHandler.oppdaterDialog(refusjon)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
        }

        test("skal ikke opprette transmission når den allerede finnes for refusjonen") {
            val dialogId = UUID.randomUUID()
            val eksisterendeTransmission = mockk<TransmissionEntity>()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(refusjon.refusjonUtfallId) } returns eksisterendeTransmission
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) } returns dialogEntity

            refusjonHandler.oppdaterDialog(refusjon)

            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }

        test("skal opprette transmission når inntektsmeldingId er null") {
            val refusjon = refusjon.copy(inntektsmeldingId = null)
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(refusjon.refusjonUtfallId) } returns null
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            refusjonHandler.oppdaterDialog(refusjon)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(refusjon.sykmeldingId) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
            verify(exactly = 1) {
                dialogRepositoryMock.oppdaterDialogMedTransmission(
                    sykmeldingId = refusjon.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = refusjon.refusjonUtfallId,
                    dokumentType = LpsApiExtendedType.REFUSJON.toString(),
                    relatedTransmissionId = null,
                )
            }
        }
    })
