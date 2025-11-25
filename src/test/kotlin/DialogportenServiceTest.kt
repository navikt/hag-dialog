package no.nav.helsearbeidsgiver.dialogporten

import forespoersel_utgaatt
import inntektsmelding_mottatt_ny
import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import sykepengesoeknad
import sykmelding

class DialogportenServiceTest :
    FunSpec({
        beforeTest {
            clearAllMocks()
        }
        val dialogRepository = mockk<DialogRepository>(relaxed = true)
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)
        val unleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)

        test("opprettOgLagreDialog skal kalle sykmeldingHandler") {
            val service = DialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.opprettOgLagreDialog(sykmelding)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedSykepengesoeknad skal kalle sykepengesoeknadHandler") {
            val service = DialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmeldingsforespoersel skal kalle forespoerselHandler") {
            val service = DialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmelding skal kalle inntektsmeldingHandler") {
            val service = DialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedInntektsmelding(inntektsmelding_mottatt_ny)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedUtgaattForespoersel skal kalle utgaattForespoerselHandler") {
            val service = DialogportenService(dialogRepository, dialogportenClient, unleashFeatureToggles)

            service.oppdaterDialogMedUtgaattForespoersel(forespoersel_utgaatt)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }
    })
