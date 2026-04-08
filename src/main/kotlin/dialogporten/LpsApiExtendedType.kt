package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravStatus
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravMelding

// Kan brukes av LPS-systemer til å gjenkjenne transmissions med ulike typer vedlegg (f.eks. sykmelding, søknad om sykepenger eller forespørsel om inntektsmelding).
enum class LpsApiExtendedType {
    SYKMELDING,
    SYKEPENGESOEKNAD,
    FORESPOERSEL_AKTIV,
    FORESPOERSEL_UTGAATT,
    INNTEKTSMELDING_AVVIST,
    INNTEKTSMELDING_GODKJENT,
}
