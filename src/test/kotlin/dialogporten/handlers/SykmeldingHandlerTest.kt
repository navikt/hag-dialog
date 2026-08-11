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
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.database.DialogEntity
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClientException
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
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val agNotifikasjonKlientMock = mockk<ArbeidsgiverNotifikasjonKlient>()
        val sykmeldingHandler =
            SykmeldingHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
                unleashFeatureTogglesMock,
                agNotifikasjonKlientMock,
            )
        beforeTest {
            clearAllMocks()
        }

        test("skal opprette og lagre dialog med riktige data") {
            val dialogId = UUID.randomUUID()
            val requestSlot = slot<CreateDialogRequest>()

            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns null
            coEvery { dialogportenClientMock.createDialog(capture(requestSlot)) } returns dialogId
            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppretteDialogKunForApi() } returns
                true
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
            coEvery { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                UUID.randomUUID().toString()
            coEvery {
                agNotifikasjonKlientMock.opprettNyBeskjed(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns
                UUID.randomUUID().toString()

            sykmeldingHandler.opprettOgLagreDialog(sykmelding)

            val capturedRequest = requestSlot.captured
            capturedRequest.orgnr shouldBe sykmelding.orgnr
            capturedRequest.externalReference shouldBe sykmelding.sykmeldingId.toString()
            capturedRequest.title shouldBe "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
            capturedRequest.summary shouldBe sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString()
            capturedRequest.isApiOnly shouldBe true

            verify(exactly = 1) {
                dialogRepositoryMock.lagreDialog(
                    dialogId = dialogId,
                    sykmeldingId = sykmelding.sykmeldingId,
                )
            }
        }

        test("skal ikke opprette sak eller beskjed hvis opprettelse av dialog feiler") {
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns null
            coEvery { dialogportenClientMock.createDialog(any()) } throws DialogportenClientException("Dialogporten feil")
            every { unleashFeatureTogglesMock.skalOppretteDialogKunForApi() } returns false
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true

            assertThrows<DialogportenClientException> {
                sykmeldingHandler.opprettOgLagreDialog(sykmelding)
            }

            verify(exactly = 0) { dialogRepositoryMock.lagreDialog(any(), any()) }
            coVerify(exactly = 0) { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) {
                agNotifikasjonKlientMock.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
        test("skal hoppe over opprettelse av dialog hvis den allerede finnes, men fortsatt opprette notifikasjoner") {
            val eksisterendeDialog = mockk<DialogEntity>()
            every { eksisterendeDialog.id.value } returns UUID.randomUUID()
            every { dialogRepositoryMock.finnDialogMedSykemeldingId(sykmelding.sykmeldingId) } returns eksisterendeDialog
            every { unleashFeatureTogglesMock.skalOppretteNotifikasjoner() } returns true
            coEvery { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns
                UUID.randomUUID().toString()
            coEvery {
                agNotifikasjonKlientMock.opprettNyBeskjed(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns
                UUID.randomUUID().toString()

            sykmeldingHandler.opprettOgLagreDialog(sykmelding)

            coVerify(exactly = 0) { dialogportenClientMock.createDialog(any()) }
            verify(exactly = 0) { dialogRepositoryMock.lagreDialog(any(), any()) }
            coVerify(exactly = 1) { agNotifikasjonKlientMock.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) {
                agNotifikasjonKlientMock.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    })
