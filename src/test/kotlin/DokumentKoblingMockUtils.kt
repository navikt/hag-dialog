import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InnsendingType
import dokumentkobling.InntektsmeldingAvvist
import dokumentkobling.InntektsmeldingGodkjent
import dokumentkobling.Status
import dokumentkobling.Sykepengesoeknad
import dokumentkobling.Sykmelding
import dokumentkobling.Sykmeldingsperiode
import dokumentkobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDateTime
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

    val forespoerselSykmeldingKobling =
        DokumentkoblingRepository.ForespoerselSykmeldingKobling(
            forespoerselId = forespoerselId,
            forespoerselStatus = ForespoerselStatus.SENDT,
            forespoerselOpprettet = LocalDateTime.now(),
            vedtaksperiodeId = vedtaksperiodeId,
            soeknadId = soeknadId,
            sykmeldingId = sykmeldingId,
            sykmeldingOpprettet = LocalDateTime.now(),
            sykmeldingStatus = Status.BEHANDLET,
            soeknadStatus = Status.BEHANDLET,
            forespoerselJobbStatus = Status.BEHANDLET,
        )
}
