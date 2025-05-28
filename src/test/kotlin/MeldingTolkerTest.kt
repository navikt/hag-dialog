import io.kotest.core.spec.style.FunSpec
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.Melding
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.Sykepengesoknad
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class MeldingTolkerTest :
    FunSpec({
        test("Behandle gyldig sykmelding") {
            val orgnr = Orgnr.genererGyldig()
            val melding = sykmelding.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.behandleSykmelding(any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppretteDialogVedMottattSykmelding(orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.behandleSykmelding(sykmelding) }
        }

        test("Behandle gyldig sykepengesøknad") {
            val orgnr = Orgnr.genererGyldig()
            val sykepengesoknad =
                Sykepengesoknad(
                    soknadId = UUID.randomUUID(),
                    sykmeldingId = UUID.randomUUID(),
                    orgnr = orgnr,
                )
            val melding = sykepengesoknad.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.behandleSykepengesoknad(any()) } just Runs
            every { unleashFeatureTogglesMock.skalOppdatereDialogVedMottattSoknad(orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.behandleSykepengesoknad(sykepengesoknad) }
        }
    })
