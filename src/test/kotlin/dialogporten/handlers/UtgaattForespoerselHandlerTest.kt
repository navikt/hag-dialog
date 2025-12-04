package dialogporten.handlers

import forespoersel_utgaatt
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.TransmissionEntity
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.UtgaattForespoerselHandler
import java.util.UUID

class UtgaattForespoerselHandlerTest :
    FunSpec({

        val dialogRepository = mockk<DialogRepository>(relaxed = true)
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)
        val utgaattForespoerselHandler = UtgaattForespoerselHandler(dialogRepository, dialogportenClient)
        beforeTest {
            clearAllMocks()
        }
        test("skal oppdatere dialog med utgått forespørsel når dialog og transmission finnes") {

            val dialogId = UUID.randomUUID()
            val relatedTransmissionId = UUID.randomUUID()
            val newTransmissionId = UUID.randomUUID()

            val transmission =
                mockk<TransmissionEntity> {
                    every { this@mockk.relatedTransmissionId } returns relatedTransmissionId
                }

            val dialog =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(forespoersel_utgaatt.forespoerselId) } returns transmission
                }

            every { dialogRepository.finnDialogMedSykemeldingId(forespoersel_utgaatt.sykmeldingId) } returns dialog
            coEvery { dialogportenClient.addTransmission(any(), any<TransmissionRequest>()) } returns newTransmissionId

            utgaattForespoerselHandler.oppdaterDialog(forespoersel_utgaatt)

            coVerify { dialogportenClient.addTransmission(dialogId, any<TransmissionRequest>()) }
            coVerify { dialogportenClient.removeActionsAndStatus(dialogId) }
            verify {
                dialogRepository.oppdaterDialogMedTransmission(
                    sykmeldingId = forespoersel_utgaatt.sykmeldingId,
                    transmissionId = newTransmissionId,
                    dokumentId = forespoersel_utgaatt.forespoerselId,
                    dokumentType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString(),
                    relatedTransmissionId = relatedTransmissionId,
                )
            }
        }

        test("skal ikke oppdatere dialog når dialog ikke finnes") {

            every { dialogRepository.finnDialogMedSykemeldingId(forespoersel_utgaatt.sykmeldingId) } returns null

            utgaattForespoerselHandler.oppdaterDialog(forespoersel_utgaatt)

            coVerify(exactly = 0) { dialogportenClient.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepository.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }

        test("skal fortsette å oppdatere selv om transmission ikke finnes") {
            val dialogId = UUID.randomUUID()
            val newTransmissionId = UUID.randomUUID()

            val dialog =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                    every { transmissionByDokumentId(forespoersel_utgaatt.forespoerselId) } returns null
                }

            every { dialogRepository.finnDialogMedSykemeldingId(forespoersel_utgaatt.sykmeldingId) } returns dialog
            coEvery { dialogportenClient.addTransmission(any(), any<TransmissionRequest>()) } returns newTransmissionId

            utgaattForespoerselHandler.oppdaterDialog(forespoersel_utgaatt)

            coVerify { dialogportenClient.addTransmission(dialogId, any<TransmissionRequest>()) }
            coVerify { dialogportenClient.removeActionsAndStatus(dialogId) }
            verify {
                dialogRepository.oppdaterDialogMedTransmission(
                    sykmeldingId = forespoersel_utgaatt.sykmeldingId,
                    transmissionId = newTransmissionId,
                    dokumentId = forespoersel_utgaatt.forespoerselId,
                    dokumentType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString(),
                    relatedTransmissionId = null,
                )
            }
        }
    })
