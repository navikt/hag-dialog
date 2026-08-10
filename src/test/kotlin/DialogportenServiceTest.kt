package no.nav.helsearbeidsgiver.dialogporten

import forespoersel_utgaatt
import inntektsmelding_godkjent
import inntektsmeldingsforespoersel
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DialogRepository
import sykepengesoeknad
import sykmelding

class DialogportenServiceTest :
    FunSpec({
        beforeTest {
            clearAllMocks()
        }
        val dialogRepository = mockk<DialogRepository>(relaxed = true)
        val dialogportenClient = mockk<DialogportenClient>(relaxed = true)

        test("opprettOgLagreDialog skal kalle sykmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient)

            service.opprettOgLagreDialog(sykmelding)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedSykepengesoeknad skal kalle sykepengesoeknadHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient)

            service.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmeldingsforespoersel skal kalle forespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient)

            service.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedInntektsmelding skal kalle inntektsmeldingHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient)

            service.oppdaterDialogMedInntektsmelding(inntektsmelding_godkjent)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }

        test("oppdaterDialogMedUtgaattForespoersel skal kalle utgaattForespoerselHandler") {
            val service = SykepengerDialogportenService(dialogRepository, dialogportenClient)

            service.oppdaterDialogMedUtgaattForespoersel(forespoersel_utgaatt)

            verify(atLeast = 0) { dialogRepository.finnDialogMedSykemeldingId(any()) }
        }
    })
