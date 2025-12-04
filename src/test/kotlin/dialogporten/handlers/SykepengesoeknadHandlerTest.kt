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
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykepengesoeknadHandler
import sykepengesoeknad
import java.util.UUID

class SykepengesoeknadHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val sykepengeSoeknadhandler =
            SykepengesoeknadHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
            )

        beforeTest {
            clearAllMocks()
        }
        test("skal oppdatere dialog med sykepengesøknad") {
            val dialogId = UUID.randomUUID()
            val transmissionId = UUID.randomUUID()
            val dialogEntity =
                mockk<DialogEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns dialogEntity
            coEvery { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) } returns transmissionId
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) } just Runs

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<TransmissionRequest>()) }
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
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) } returns null

            sykepengeSoeknadhandler.oppdaterDialog(sykepengesoeknad)

            verify(exactly = 1) { dialogRepositoryMock.finnDialogMedSykemeldingId(sykepengesoeknad.sykmeldingId) }
            coVerify(exactly = 0) { dialogportenClientMock.addTransmission(any(), any<TransmissionRequest>()) }
            verify(exactly = 0) { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any(), any()) }
        }
    })
