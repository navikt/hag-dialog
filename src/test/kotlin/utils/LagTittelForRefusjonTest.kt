package utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.utils.REFUSJON_TITTEL
import no.nav.helsearbeidsgiver.utils.tittelMedPeriodeFomOgTom
import refusjon

class LagTittelForRefusjonTest :
    FunSpec({

        test("lagTittel") {
            val refusjon = refusjon
            refusjon.tittelMedPeriodeFomOgTom() shouldBe "$REFUSJON_TITTEL 01.01.2021 – 31.01.2021"
        }
    })
