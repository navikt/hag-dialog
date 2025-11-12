import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

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
val inntektsmelding_mottatt =
    Inntektsmelding(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        innsendingId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        status = Inntektsmelding.Status.MOTTATT,
    )
val inntektsmelding_godkjent =
    Inntektsmelding(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        innsendingId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        status = Inntektsmelding.Status.GODKJENT,
    )
val inntektsmelding_feilet =
    Inntektsmelding(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        innsendingId = UUID.randomUUID(),
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
        status = Inntektsmelding.Status.FEILET,
    )
val forespoersel_utgaatt =
    UtgaattInntektsmeldingForespoersel(
        forespoerselId = inntektsmeldingsforespoersel.forespoerselId,
        sykmeldingId = sykmelding.sykmeldingId,
        orgnr = orgnr,
    )
