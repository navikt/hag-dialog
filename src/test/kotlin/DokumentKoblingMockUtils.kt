
import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InnsendingType
import dokumentkobling.InntektsmeldingGodkjent
import dokumentkobling.Sykepengesoeknad
import dokumentkobling.Sykmelding
import dokumentkobling.Sykmeldingsperiode
import dokumentkobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

val dokumentkoblingSykmelding =
    Sykmelding(
        sykmeldingId = UUID.randomUUID(),
        orgnr = Orgnr.genererGyldig(),
        foedselsdato = 1.januar,
        fulltNavn = "Ola Nordmann",
        sykmeldingsperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar)),
    )

val dokumentkoblingSoeknad =
    Sykepengesoeknad(
        soeknadId = UUID.randomUUID(),
        sykmeldingId = dokumentkoblingSykmelding.sykmeldingId,
        orgnr = dokumentkoblingSykmelding.orgnr,
    )

val dokumentkoblingVedtaksperiodeSoeknad =
    VedtaksperiodeSoeknadKobling(
        vedtaksperiodeId = UUID.randomUUID(),
        soeknadId = dokumentkoblingSoeknad.soeknadId,
    )

val dokumentkoblingForespoerselSendt =
    ForespoerselSendt(
        forespoerselId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        orgnr = dokumentkoblingSykmelding.orgnr,
    )

val dokumentkoblingForespoerselUtgaatt =
    ForespoerselUtgaatt(
        forespoerselId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        orgnr = dokumentkoblingSykmelding.orgnr,
    )

val dokumentkoblingInntektsmeldingGodkjent =
    InntektsmeldingGodkjent(
        inntektsmeldingId = UUID.randomUUID(),
        forespoerselId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        orgnr = dokumentkoblingSykmelding.orgnr,
        innsendingType = InnsendingType.FORESPURT_EKSTERN,
    )
