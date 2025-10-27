package no.nav.helsearbeidsgiver.dialogporten

// Kan brukes av LPS-systemer til å gjenkjenne transmissions med ulike typer vedlegg (f.eks. sykmelding, søknad om sykepenger eller forespørsel om inntektsmelding).
enum class LpsApiExtendedType {
    SYKMELDING,
    SYKEPENGESOEKNAD,
    INNTEKTSMELDINGFORESPOERSEL,
    INNTEKTSMELDING,
}
