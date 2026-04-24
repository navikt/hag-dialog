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
import no.nav.helsearbeidsgiver.database.FritakAgpDokType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadOpprettet
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class FritakAgpSoeknadHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>(relaxed = true)
        val fritakDialogRepositoryMock = mockk<FritakDialogRepository>()

        val handler =
            no.nav.helsearbeidsgiver.dialogporten.handlers.FritakAgpSoeknadHandler(
                dialogportenClientMock,
                fritakDialogRepositoryMock,
            )

        val orgnr = Orgnr.genererGyldig()
        val dialogId = UUID.randomUUID()

        beforeTest {
            clearAllMocks(answers = false)
            coEvery { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) } returns dialogId
            every { fritakDialogRepositoryMock.lagreSoeknadDialog(any(), any(), any(), any(), any()) } just Runs
        }

        test("skal opprette dialog for KroniskSoeknadOpprettet") {
            val soeknadId = UUID.randomUUID()
            val soeknadMelding =
                KroniskSoeknadOpprettet(
                    id = soeknadId,
                    orgnr = orgnr,
                    navn = "Ola Nordmann",
                    fnr = "010190123456",
                )

            handler.behandleSoeknadDialog(soeknadMelding)

            coVerify(exactly = 1) { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addGuiAction(dialogId, any<GuiAction>()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreSoeknadDialog(
                    dialogId = dialogId,
                    soeknadId = soeknadId,
                    soeknadType = FritakAgpDokType.KRONISK_SOEKNAD,
                    fnr = soeknadMelding.fnr,
                    orgnr = soeknadMelding.orgnr.verdi,
                )
            }
        }

        test("skal opprette dialog for GravidSoeknadOpprettet") {
            val soeknadId = UUID.randomUUID()
            val soeknadMelding =
                GravidSoeknadOpprettet(
                    id = soeknadId,
                    orgnr = orgnr,
                    navn = "Kari Nordmann",
                    fnr = "020290123456",
                )

            handler.behandleSoeknadDialog(soeknadMelding)

            coVerify(exactly = 1) { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addGuiAction(dialogId, any<GuiAction>()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreSoeknadDialog(
                    dialogId = dialogId,
                    soeknadId = soeknadId,
                    soeknadType = FritakAgpDokType.GRAVID_SOEKNAD,
                    fnr = soeknadMelding.fnr,
                    orgnr = soeknadMelding.orgnr.verdi,
                )
            }
        }

        test("skal ikke lagre soeknad dersom createDialog feiler") {
            coEvery { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) } throws RuntimeException("Dialogporten utilgjengelig")

            val soeknadMelding =
                KroniskSoeknadOpprettet(
                    id = UUID.randomUUID(),
                    orgnr = orgnr,
                    navn = "Ola Nordmann",
                    fnr = "010190123456",
                )

            runCatching { handler.behandleSoeknadDialog(soeknadMelding) }

            verify(exactly = 0) { fritakDialogRepositoryMock.lagreSoeknadDialog(any(), any(), any(), any(), any()) }
        }
    })
