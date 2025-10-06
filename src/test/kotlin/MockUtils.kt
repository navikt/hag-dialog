import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

fun main() {
    println(sykmelding.toJson(Sykmelding.serializer()).toString())
    println(sykepengesoeknad.toJson(Sykepengesoeknad.serializer()).toString())
    println(
        inntektsmeldingsforespoersel.toJson(Inntektsmeldingsforespoersel.serializer()).toString(),
    )
}

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

val sykepengesoeknad =
    Sykepengesoeknad(
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        soeknadId = UUID.randomUUID(),
    )

val inntektsmeldingsforespoersel =
    Inntektsmeldingsforespoersel(
        forespoerselId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
    )
