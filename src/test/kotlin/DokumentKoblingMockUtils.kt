import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InnsendingType
import dokumentkobling.InntektsmeldingAvvist
import dokumentkobling.InntektsmeldingGodkjent
import dokumentkobling.Sykepengesoeknad
import dokumentkobling.Sykmelding
import dokumentkobling.Sykmeldingsperiode
import dokumentkobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

object DokumentKoblingMockUtils {
    val inntektsmeldingId = UUID.randomUUID()
    val forespoerselId = UUID.randomUUID()
    val soeknadId = UUID.randomUUID()
    val vedtaksperiodeId = UUID.randomUUID()
    val sykmeldingId = UUID.randomUUID()
    val orgnr = Orgnr.genererGyldig()

    val sykmelding =
        Sykmelding(
            sykmeldingId = sykmeldingId,
            orgnr = orgnr,
            foedselsdato = 1.januar,
            fulltNavn = "Ola Nordmann",
            sykmeldingsperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar)),
        )

    val soeknad =
        Sykepengesoeknad(
            soeknadId = soeknadId,
            sykmeldingId = sykmeldingId,
            orgnr = orgnr,
        )

    val vedtaksperiodeSoeknadKobling =
        VedtaksperiodeSoeknadKobling(
            vedtaksperiodeId = vedtaksperiodeId,
            soeknadId = soeknadId,
        )

    val forespoerselSendt =
        ForespoerselSendt(
            forespoerselId = forespoerselId,
            vedtaksperiodeId = vedtaksperiodeId,
            orgnr = orgnr,
        )

    val forespoerselUtgaatt =
        ForespoerselUtgaatt(
            forespoerselId = forespoerselId,
            vedtaksperiodeId = vedtaksperiodeId,
            orgnr = orgnr,
        )
    val inntektsmeldingGodkjent =
        InntektsmeldingGodkjent(
            inntektsmeldingId = inntektsmeldingId,
            forespoerselId = forespoerselId,
            vedtaksperiodeId = vedtaksperiodeId,
            orgnr = orgnr,
            innsendingType = InnsendingType.FORESPURT_EKSTERN,
        )
    val inntektsmeldingAvvist =
        InntektsmeldingAvvist(
            inntektsmeldingId = inntektsmeldingId,
            forespoerselId = forespoerselId,
            vedtaksperiodeId = vedtaksperiodeId,
            orgnr = orgnr,
        )
}
