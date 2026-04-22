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
import no.nav.helsearbeidsgiver.database.FritakAgpKravEntity
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravOpprettet
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class FritakAgpKravHandlerTest :
    FunSpec({

        val dialogportenClientMock = mockk<DialogportenClient>(relaxed = true)
        val fritakDialogRepositoryMock = mockk<FritakDialogRepository>()

        val handler = FritakAgpKravHandler(dialogportenClientMock, fritakDialogRepositoryMock)

        val orgnr = Orgnr.genererGyldig()
        val dialogId = UUID.randomUUID()
        val transmissionId = UUID.randomUUID()

        beforeTest {
            clearAllMocks(answers = false)
            coEvery { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) } returns dialogId
            coEvery { dialogportenClientMock.addTransmission(any<UUID>(), any<Transmission>()) } returns transmissionId
            every { fritakDialogRepositoryMock.lagreKravDialog(any(), any(), any(), any(), any(), any()) } just Runs
        }

        test("skal opprette dialog for KroniskKrav") {
            val kravId = UUID.randomUUID()
            val kravmelding = KroniskKravOpprettet(id = kravId, orgnr = orgnr, navn = "Ola Nordmann", fnr = "010190123456")

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addGuiAction(dialogId, any<GuiAction>()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = kravId,
                    kravType = FritakAgpType.KRONISK_KRAV_OPPRETTET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }

        test("skal oppdatere dialog for GravidKravEndret") {
            val forrigeKravId = UUID.randomUUID()
            val nyttKravId = UUID.randomUUID()
            val kravmelding =
                GravidKravEndret(
                    id = nyttKravId,
                    orgnr = orgnr,
                    navn = "Kari Nordmann",
                    fnr = "020290123456",
                    forrigeKrav = forrigeKravId,
                )
            val eksisterendeKrav =
                mockk<FritakAgpKravEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { fritakDialogRepositoryMock.finnDialogMedKravId(forrigeKravId) } returns eksisterendeKrav

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.replaceAttachmentsAndActions(dialogId, any(), any(), any()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = nyttKravId,
                    kravType = FritakAgpType.GRAVID_KRAV_ENDRET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }

        test("skal oppdatere dialog for KroniskKravEndret ") {
            val forrigeKravId = UUID.randomUUID()
            val nyttKravId = UUID.randomUUID()
            val kravmelding =
                KroniskKravEndret(
                    id = nyttKravId,
                    orgnr = orgnr,
                    navn = "Ola Nordmann",
                    fnr = "010190123456",
                    forrigeKrav = forrigeKravId,
                )
            val eksisterendeKrav =
                mockk<FritakAgpKravEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { fritakDialogRepositoryMock.finnDialogMedKravId(forrigeKravId) } returns eksisterendeKrav

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.replaceAttachmentsAndActions(dialogId, any(), any(), any()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = nyttKravId,
                    kravType = FritakAgpType.KRONISK_KRAV_ENDRET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }

        test("skal oppdatere dialog for KroniskKravSlettet ") {
            val kravId = UUID.randomUUID()
            val kravmelding =
                KroniskKravSlettet(id = kravId, orgnr = orgnr, navn = "Ola Nordmann", fnr = "010190123456")
            val eksisterendeKrav =
                mockk<FritakAgpKravEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { fritakDialogRepositoryMock.finnDialogMedKravId(kravId) } returns eksisterendeKrav

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.removeActionsAndStatus(dialogId) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = kravId,
                    kravType = FritakAgpType.KRONISK_KRAV_SLETTET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }

        test("skal opprette dialog for GravidKravOpprettet") {
            val kravId = UUID.randomUUID()
            val kravmelding = GravidKravOpprettet(id = kravId, orgnr = orgnr, navn = "Kari Nordmann", fnr = "020290123456")

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.createDialog(any<CreateDialogRequest>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.addGuiAction(dialogId, any<GuiAction>()) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = kravId,
                    kravType = FritakAgpType.GRAVID_KRAV_OPPRETTET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }

        test("skal oppdatere dialog for GravidKravSlettet") {
            val kravId = UUID.randomUUID()
            val kravmelding =
                GravidKravSlettet(id = kravId, orgnr = orgnr, navn = "Kari Nordmann", fnr = "020290123456")
            val eksisterendeKrav =
                mockk<FritakAgpKravEntity> {
                    every { this@mockk.dialogId } returns dialogId
                }

            every { fritakDialogRepositoryMock.finnDialogMedKravId(kravId) } returns eksisterendeKrav

            handler.behandleKravDialog(kravmelding)

            coVerify(exactly = 1) { dialogportenClientMock.addTransmission(dialogId, any<Transmission>()) }
            coVerify(exactly = 1) { dialogportenClientMock.removeActionsAndStatus(dialogId) }
            verify(exactly = 1) {
                fritakDialogRepositoryMock.lagreKravDialog(
                    dialogId = dialogId,
                    transmissionId = transmissionId,
                    kravId = kravId,
                    kravType = FritakAgpType.GRAVID_KRAV_SLETTET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }
    })
