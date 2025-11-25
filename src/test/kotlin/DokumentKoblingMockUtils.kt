import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

val dokumentKoblingSykmelding = Sykmelding(
    sykmeldingId = UUID.randomUUID(),
    orgnr = Orgnr.genererGyldig(),
    foedselsdato = 1.januar,
    fulltNavn = "Ola Nordmann",
    sykmeldingsperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar)),
)