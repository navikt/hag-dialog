package utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.REFUSJON_TITTEL
import no.nav.helsearbeidsgiver.utils.tittelMedPeriodeFomOgTom
import refusjon
import java.time.LocalDate

class LagTittelForRefusjonTest :
    FunSpec({

        test("lagTittel") {
            val refusjon = refusjon
            refusjon.tittelMedPeriodeFomOgTom() shouldBe "$REFUSJON_TITTEL 01.01.2021 – 31.01.2021"
        }

        test("lagTittel for revurdering") {
            val refusjon =
                refusjon.copy(
                    fom = LocalDate.of(2026, 5, 27),
                    tom = LocalDate.of(2026, 7, 2),
                    erRevurdering = true,
                )
            refusjon.tittelMedPeriodeFomOgTom() shouldBe "Refusjon av sykepenger 27.05.2026 – 02.07.2026 (revurdering)"
        }
    })
