package no.nav.helsearbeidsgiver.dialogporten

interface ExtendedType

// Kan brukes av LPS-systemer til å gjenkjenne transmissions med ulike typer vedlegg (f.eks. sykmelding, søknad om sykepenger eller forespørsel om inntektsmelding).
enum class LpsApiExtendedType : ExtendedType {
    SYKMELDING,
    SYKEPENGESOEKNAD,
    FORESPOERSEL_AKTIV,
    FORESPOERSEL_UTGAATT,
    INNTEKTSMELDING_AVVIST,
    INNTEKTSMELDING_GODKJENT,
}

enum class FritakAgpType : ExtendedType {
    GRAVID_KRAV_OPPRETTET,
    GRAVID_KRAV_ENDRET,
    GRAVID_KRAV_SLETTET,

    KRONISK_KRAV_OPPRETTET,
    KRONISK_KRAV_ENDRET,
    KRONISK_KRAV_SLETTET,
}
