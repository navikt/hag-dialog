
import no.nav.helsearbeidsgiver.dokumentKobling.ForespoerselSendt
import no.nav.helsearbeidsgiver.dokumentKobling.ForespoerselUtgaatt
import no.nav.helsearbeidsgiver.dokumentKobling.Sykepengesoeknad
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.dokumentKobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

val dokumentKoblingSykmelding =
    Sykmelding(
        sykmeldingId = UUID.randomUUID(),
        orgnr = Orgnr.genererGyldig(),
        foedselsdato = 1.januar,
        fulltNavn = "Ola Nordmann",
        sykmeldingsperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar)),
    )

val dokumentKoblingSoeknad =
    Sykepengesoeknad(
        soeknadId = UUID.randomUUID(),
        sykmeldingId = dokumentKoblingSykmelding.sykmeldingId,
        orgnr = dokumentKoblingSykmelding.orgnr,
    )

val dokumentKoblingVedtaksperiodeSoeknad =
    VedtaksperiodeSoeknadKobling(
        vedtaksperiodeId = UUID.randomUUID(),
        soeknadId = dokumentKoblingSoeknad.soeknadId,
    )

val dokumentKoblingForespoerselSendt =
    ForespoerselSendt(
        forespoerselId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        orgnr = dokumentKoblingSykmelding.orgnr,
    )

val dokumentKoblingForespoerselUtgaatt =
    ForespoerselUtgaatt(
        forespoerselId = UUID.randomUUID(),
        vedtaksperiodeId = UUID.randomUUID(),
        orgnr = dokumentKoblingSykmelding.orgnr,
    )
