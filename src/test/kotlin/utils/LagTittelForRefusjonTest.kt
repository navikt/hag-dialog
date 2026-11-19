package utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.kafka.Refusjon
import no.nav.helsearbeidsgiver.utils.REFUSJON_TITTEL
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.tittelMedPeriodeFomOgTom
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

class LagTittelForRefusjonTest :
    FunSpec({

        test("lagTittel") {
            val refusjon =
                Refusjon(
                    sykmeldingId = UUID.randomUUID(),
                    fom = LocalDate.of(2021, 1, 1),
                    tom = LocalDate.of(2021, 1, 31),
                    refusjonUtfallId = UUID.randomUUID(),
                    inntektsmeldingId = UUID.randomUUID(),
                    orgnr = Orgnr.genererGyldig(),
                )
            refusjon.tittelMedPeriodeFomOgTom() shouldBe "$REFUSJON_TITTEL 01.01.2021 – 31.01.2021"
        }
    })
