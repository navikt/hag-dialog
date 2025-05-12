import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.kafka.Melding
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

class MeldingTolkerTest :
    FunSpec({
        test("Opprett dialog ved gyldig sykmelding") {
            val orgnr = Orgnr.genererGyldig()
            val sykmelding =
                Sykmelding(
                    sykmeldingId = UUID.randomUUID(),
                    orgnr = orgnr,
                    foedselsdato = LocalDate.of(1990, 1, 1),
                    fulltNavn = "OLA NORDMANN",
                    sykmeldingsperioder =
                        listOf(
                            Sykmeldingsperiode(
                                fom = LocalDate.of(2023, 1, 1),
                                tom = LocalDate.of(2023, 1, 31),
                            ),
                        ),
                )
            val melding = sykmelding.toJson(Melding.serializer()).toString()

            val dialogportenServiceMock = mockk<DialogportenService>()
            val unleashFeatureTogglesMock = mockk<UnleashFeatureToggles>()

            every { dialogportenServiceMock.opprettNyDialogMedSykmelding(any()) } returns "123"
            every { unleashFeatureTogglesMock.skalOppretteDialogVedMottattSykmelding(orgnr) } returns true

            val meldingTolker =
                MeldingTolker(
                    unleashFeatureToggles = unleashFeatureTogglesMock,
                    dialogportenService = dialogportenServiceMock,
                )

            meldingTolker.lesMelding(melding)

            verify(exactly = 1) { dialogportenServiceMock.opprettNyDialogMedSykmelding(sykmelding) }
        }
    })
