package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.database.finnTypeForFritakKrav
import no.nav.helsearbeidsgiver.dialogporten.domene.Attachment
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravMelding
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import java.util.UUID

class SykmeldingTransmissionRequest(
    sykmelding: Sykmelding,
    override val attachments: List<Attachment>,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKMELDING.toString()
    override val dokumentId = sykmelding.sykmeldingId
    override val tittel = "Sykmelding"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class SykepengesoknadTransmissionRequest(
    sykepengesoeknad: Sykepengesoeknad,
    override val attachments: List<Attachment>,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString()
    override val dokumentId = sykepengesoeknad.soeknadId
    override val tittel = "Søknad om sykepenger"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class ForespoerselTransmissionRequest(
    forespoerselId: UUID,
    override val relatedTransmissionId: UUID? = null,
    override val attachments: List<Attachment>,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.FORESPOERSEL_AKTIV.toString()
    override val dokumentId = forespoerselId
    override val tittel = "Forespørsel om inntektsmelding"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Request
}

class UtgaattForespoerselTransmissionRequest(
    forespoerselId: UUID,
    override val relatedTransmissionId: UUID? = null,
    override val attachments: List<Attachment>,
) : TransmissionRequest() {
    override val extendedType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString()
    override val dokumentId = forespoerselId
    override val tittel = "Forespørsel er utgått"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
}

fun Inntektsmelding.Status.toExtendedType(): String =
    when (this) {
        Inntektsmelding.Status.FEILET -> LpsApiExtendedType.INNTEKTSMELDING_AVVIST.toString()
        Inntektsmelding.Status.GODKJENT -> LpsApiExtendedType.INNTEKTSMELDING_GODKJENT.toString()
    }

fun Inntektsmelding.Status.toTittel(): String =
    when (this) {
        Inntektsmelding.Status.FEILET -> "Inntektsmelding avvist"
        Inntektsmelding.Status.GODKJENT -> "Inntektsmelding godkjent"
    }

fun Inntektsmelding.Status.toTransmissionType(): Transmission.TransmissionType =
    when (this) {
        Inntektsmelding.Status.FEILET -> Transmission.TransmissionType.Rejection
        Inntektsmelding.Status.GODKJENT -> Transmission.TransmissionType.Acceptance
    }

class InntektsmeldingTransmissionRequest(
    inntektsmelding: Inntektsmelding,
    override val relatedTransmissionId: UUID?,
    override val attachments: List<Attachment>,
) : TransmissionRequest() {
    override val extendedType = inntektsmelding.status.toExtendedType()
    override val dokumentId = inntektsmelding.innsendingId
    override val tittel = inntektsmelding.status.toTittel()
    override val sammendrag = null
    override val type = inntektsmelding.status.toTransmissionType()
}

class FritakGravidKravTransmissionRequest(
    kravMelding: GravidKravMelding,
) : TransmissionRequest() {
    override val relatedTransmissionId = null
    override val dokumentId = kravMelding.id
    override val extendedType = finnTypeForFritakKrav(kravMelding).toString()
    override val tittel = "Krav om fritak for arbeidsgiverperiode ved graviditet ${kravMelding.status.verdi}"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val attachments = emptyList<Attachment>()
}

class FritakKroniskKravTransmissionRequest(
    kravMelding: KroniskKravMelding,
) : TransmissionRequest() {
    override val relatedTransmissionId = null
    override val dokumentId = kravMelding.id
    override val extendedType = finnTypeForFritakKrav(kravMelding).toString()
    override val tittel = "Krav om fritak for arbeidsgiverperiode ved kronisk sykdom ${kravMelding.status.verdi}"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val attachments = emptyList<Attachment>()
}
