package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding

class SykmeldingTransmissionRequest(
    sykmelding: Sykmelding,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKMELDING.toString()
    override val tittle = "Sykmelding"
    override val summary = null
    override val vedleggNavn = "sykmelding.json"
    override val vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}"
    override val type = Transmission.TransmissionType.Information
}

class SykepengesoknadTransmissionRequest(
    sykepengesoeknad: Sykepengesoeknad,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString()
    override val tittle = "Søknad om sykepenger"
    override val summary = null
    override val vedleggNavn = "sykepengesoeknad.json"
    override val vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/soknad/${sykepengesoeknad.soeknadId}"
    override val type = Transmission.TransmissionType.Information
}

class InntektsmeldingTransmissionRequest(
    inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.INNTEKTSMELDINGFORESPOERSEL.toString()
    override val tittle = "Forespoersel om Inntektsmelding"
    override val summary = null
    override val vedleggNavn = "inntektsmeldingforespoersel.json"
    override val vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${inntektsmeldingsforespoersel.forespoerselId}"
    override val type = Transmission.TransmissionType.Request
}
