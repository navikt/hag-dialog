import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.Melding
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.toJson

class MeldingTolkerTest :
    FunSpec({
        test("Behandle gyldig sykmelding") {
            val melding = sykmelding.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.opprettOgLagreDialog(any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppretteDialogVedMottattSykmelding(sykmelding.orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.opprettOgLagreDialog(sykmelding) }
        }

        test("Behandle gyldig sykepengesøknad") {
            val sykepengesoeknad = sykepengesoeknad
            val melding = sykepengesoeknad.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.oppdaterDialog(any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppdatereDialogVedMottattSoeknad(sykepengesoeknad.orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.oppdaterDialog(sykepengesoeknad) }
        }
    })
