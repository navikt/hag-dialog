package dialogporten.handlers

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.handlers.SykmeldingHandler
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import sykmelding
import java.util.UUID

class SykmeldingHandlerTest :
    FunSpec({
        val dialogportenClientMock = mockk<DialogportenClient>()
        val dialogRepositoryMock = mockk<DialogRepository>()
        val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()
        val sykmeldingHandler =
            SykmeldingHandler(
                dialogRepositoryMock,
                dialogportenClientMock,
                unleashFeatureTogglesMock,
            )
        beforeTest {
            clearAllMocks()
        }

        test("skal opprette og lagre dialog med riktige data") {
            val dialogId = UUID.randomUUID()
            val requestSlot = slot<CreateDialogRequest>()

            coEvery { dialogportenClientMock.createDialog(capture(requestSlot)) } returns dialogId
            every { dialogRepositoryMock.lagreDialog(any(), any()) } just Runs
            coEvery { dialogportenClientMock.setDialogStatus(any(), any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppretteDialogKunForApi() } returns
                true
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
    })
