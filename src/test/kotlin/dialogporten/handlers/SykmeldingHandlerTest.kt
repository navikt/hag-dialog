package dialogporten.handlers

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
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.NotifikasjonRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClientException
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import org.junit.jupiter.api.assertThrows
import sykmelding
import java.util.UUID

class SykmeldingHandlerTest :
    FunSpec({
        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val notifikasjonRepositoryMock = mockk<NotifikasjonRepository>(relaxed = true)
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val sykmeldingHandler =
            SykmeldingHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
                notifikasjonRepositoryMock,
                unleashFeatureTogglesMock,
            )
        beforeTest {
            clearAllMocks()
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
        }

        test("skal opprette og lagre dialog med riktige data") {
            val dialogId = UUID.randomUUID()
            val requestSlot = slot<CreateDialogRequest>()

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns null
            coEvery { dialogportenClientMock.createDialog(capture(requestSlot)) } returns dialogId
            every {
                dialogRepositoryMock.lagreDialogMedTransmission(any(), any(), any(), any(), any(), any())
            } just Runs
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { dialogRepositoryMock.oppdaterDialogMedTransmission(any(), any(), any(), any()) } just Runs

            sykmeldingHandler.opprettOgLagreDialog(sykmelding)

            val capturedRequest = requestSlot.captured
            capturedRequest.orgnr shouldBe sykmelding.orgnr
            capturedRequest.externalReference shouldBe sykmelding.sykmeldingId.toString()
            capturedRequest.title shouldBe "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
            capturedRequest.summary shouldBe sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString()
            capturedRequest.isApiOnly shouldBe false

            val transmissionId = capturedRequest.transmissions.single().id

            verify(exactly = 1) {
                dialogRepositoryMock.lagreDialogMedTransmission(
                    dialogId = dialogId,
                    sykmeldingId = sykmelding.sykmeldingId,
                    transmissionId = transmissionId,
                    dokumentId = sykmelding.sykmeldingId,
                    dokumentType = LpsApiExtendedType.SYKMELDING.toString(),
                )
            }
            verify(exactly = 1) {
                notifikasjonRepositoryMock.opprettNotifikasjon(
                    dokumentId = sykmelding.sykmeldingId,
                    tjeneste = Tjeneste.SYKMELDING,
                )
            }
        }

        test("skal ikke legge notifikasjon i kø hvis opprettelse av dialog feiler") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns null
            coEvery { dialogportenClientMock.createDialog(any()) } throws DialogportenClientException("Dialogporten feil")

            assertThrows<DialogportenClientException> {
                sykmeldingHandler.opprettOgLagreDialog(sykmelding)
            }

            verify(exactly = 0) { dialogRepositoryMock.lagreDialogMedTransmission(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 0) { notifikasjonRepositoryMock.opprettNotifikasjon(any(), any()) }
        }

        test("skal hoppe over opprettelse av dialog hvis den allerede finnes, men fortsatt legge notifikasjon i kø") {
            val eksisterendeDialog = mockk<DialogEntity>()
            every { eksisterendeDialog.id.value } returns UUID.randomUUID()
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns eksisterendeDialog

            sykmeldingHandler.opprettOgLagreDialog(sykmelding)

            coVerify(exactly = 0) { dialogportenClientMock.createDialog(any()) }
            verify(exactly = 0) { dialogRepositoryMock.lagreDialogMedTransmission(any(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) {
                notifikasjonRepositoryMock.opprettNotifikasjon(
                    dokumentId = sykmelding.sykmeldingId,
                    tjeneste = Tjeneste.SYKMELDING,
                )
            }
        }
    })
