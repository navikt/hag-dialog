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
        xtest("tolker melding om sykmelding og opprett dialog") {
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

        xtest("tolker sykepengesøknad og oppdaterer dialog") {
            val melding = sykepengesoeknad.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.oppdaterDialogMedSykepengesoeknad(any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppdatereDialogVedMottattSoeknad(sykepengesoeknad.orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad) }
        }

        xtest("tolker inntektsmeldingsforespoersel og oppdaterer dialog") {
            val melding = inntektsmeldingsforespoersel.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.oppdaterDialogMedInntektsmeldingsforespoersel(any()) } just Runs
            every {
                unleashFeatureTogglesMock.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(
                    sykepengesoeknad.orgnr,
                )
            } returns
                true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel) }
        }
    })
