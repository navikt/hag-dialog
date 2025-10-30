package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import java.util.UUID

class SykmeldingTransmissionRequest(
    sykmelding: Sykmelding,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKMELDING.toString()
    override val dokumentId = sykmelding.sykmeldingId
    override val tittel = "Sykmelding"
    override val sammendrag = null
    override val vedleggNavn = "sykmelding.json"
    override val vedleggBaseUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding"
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class SykepengesoknadTransmissionRequest(
    sykepengesoeknad: Sykepengesoeknad,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString()
    override val dokumentId = sykepengesoeknad.soeknadId
    override val tittel = "Søknad om sykepenger"
    override val sammendrag = null
    override val vedleggNavn = "sykepengesoeknad.json"
    override val vedleggBaseUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad"
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class ForespoerselTransmissionRequest(
    inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel,
    override val relatedTransmissionId: UUID? = null,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.INNTEKTSMELDINGFORESPOERSEL.toString()
    override val dokumentId = inntektsmeldingsforespoersel.forespoerselId
    override val tittel = "Forespørsel om inntektsmelding"
    override val sammendrag = null
    override val vedleggNavn = "inntektsmeldingforespoersel.json"
    override val vedleggBaseUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel"
    override val type = Transmission.TransmissionType.Request
}

class InntektsmeldingTransmissionRequest(
    inntektsmelding: Inntektsmelding,
    override val relatedTransmissionId: UUID?,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.INNTEKTSMELDING.toString()
    override val dokumentId = inntektsmelding.innsendingId
    override val tittel =
        when (inntektsmelding.status) {
            Inntektsmelding.Status.MOTTATT -> "Inntektsmelding mottatt"
            Inntektsmelding.Status.FEILET -> "Inntektsmelding avvist"
            Inntektsmelding.Status.GODKJENT -> "Inntektsmelding godkjent"
        }
    override val sammendrag = null
    override val vedleggNavn = "inntektsmelding.json"
    override val vedleggBaseUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/inntektsmelding"
    override val type =
        when (inntektsmelding.status) {
            Inntektsmelding.Status.MOTTATT -> Transmission.TransmissionType.Submission
            Inntektsmelding.Status.FEILET -> Transmission.TransmissionType.Rejection
            Inntektsmelding.Status.GODKJENT -> Transmission.TransmissionType.Acceptance
        }
}
