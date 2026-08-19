package no.nav.helsearbeidsgiver.dialogporten

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.finnTypeForFritakKrav
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.FORESPOERSEL_AKTIV
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.FORESPOERSEL_UTGAATT
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.INNTEKTSMELDING_AVVIST
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.INNTEKTSMELDING_GODKJENT
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.SYKEPENGESOEKNAD
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType.SYKMELDING
import no.nav.helsearbeidsgiver.dialogporten.domene.Attachment
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravOpprettet
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadOpprettet
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadOpprettet
import no.nav.helsearbeidsgiver.utils.nyUuidv7
import java.util.UUID

class SykmeldingTransmissionRequest(
    sykmeldingId: UUID,
    override val attachments: List<Attachment>,
    override val isSilentUpdate: Boolean = false,
) : TransmissionRequestMedLestFce(SYKMELDING) {
    override val dokumentId = sykmeldingId
    override val tittel = "Sykmelding"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class SykepengesoknadTransmissionRequest(
    soeknadId: UUID,
    override val attachments: List<Attachment>,
    override val isSilentUpdate: Boolean = false,
) : TransmissionRequestMedLestFce(SYKEPENGESOEKNAD) {
    override val dokumentId = soeknadId
    override val tittel = "Søknad om sykepenger"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val relatedTransmissionId = null
}

class ForespoerselTransmissionRequest(
    forespoerselId: UUID,
    override val relatedTransmissionId: UUID? = null,
    override val attachments: List<Attachment>,
) : TransmissionRequestMedLestFce(FORESPOERSEL_AKTIV) {
    override val dokumentId = forespoerselId
    override val tittel = "Forespørsel om inntektsmelding"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Request
}

class UtgaattForespoerselTransmissionRequest(
    forespoerselId: UUID,
    override val relatedTransmissionId: UUID? = null,
    override val attachments: List<Attachment>,
) : TransmissionRequestMedLestFce(FORESPOERSEL_UTGAATT) {
    override val dokumentId = forespoerselId
    override val tittel = "Forespørsel er utgått"
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
}

fun Inntektsmelding.Status.toExtendedType(): LpsApiExtendedType =
    when (this) {
        Inntektsmelding.Status.FEILET -> INNTEKTSMELDING_AVVIST
        Inntektsmelding.Status.GODKJENT -> INNTEKTSMELDING_GODKJENT
    }

fun Inntektsmelding.Status.toTittel(): String =
    when (this) {
        Inntektsmelding.Status.FEILET -> "Inntektsmelding avvist"
        Inntektsmelding.Status.GODKJENT -> "Inntektsmelding mottatt"
    }

fun Inntektsmelding.Status.toTransmissionType(): Transmission.TransmissionType =
    when (this) {
        Inntektsmelding.Status.FEILET -> Transmission.TransmissionType.Rejection
        Inntektsmelding.Status.GODKJENT -> Transmission.TransmissionType.Submission
    }

class InntektsmeldingTransmissionRequest(
    inntektsmelding: Inntektsmelding,
    override val relatedTransmissionId: UUID?,
    override val attachments: List<Attachment>,
) : TransmissionRequestMedLestFce(inntektsmelding.status.toExtendedType()) {
    override val dokumentId = inntektsmelding.innsendingId
    override val tittel = inntektsmelding.status.toTittel()
    override val sammendrag = null
    override val type = inntektsmelding.status.toTransmissionType()
}

class FritakKravTransmissionRequest(
    kravMelding: FritakKravMelding,
) : TransmissionRequestMedLestFce(finnTypeForFritakKrav(kravMelding)) {
    override val relatedTransmissionId = null
    override val dokumentId = kravMelding.id
    override val tittel = kravMelding.toTittel()
    override val sammendrag = null
    override val type = Transmission.TransmissionType.Information
    override val attachments =
        listOf(
            createApiAttachment(
                displayName = "Krav på fritak fra arbeidsgiverperioden",
                url = kravMelding.toPdfUrl(),
                mediaType = "application/pdf",
            ),
            createGuiAttachment(
                displayName = "Krav på fritak fra arbeidsgiverperioden",
                url = kravMelding.toPdfUrl(),
                mediaType = "application/pdf",
            ),
        )
}

fun FritakKravMelding.toPdfUrl(): String {
    val type =
        when (this) {
            is GravidKravOpprettet, is GravidKravEndret, is GravidKravSlettet -> "gravid-krav"
            is KroniskKravOpprettet, is KroniskKravEndret, is KroniskKravSlettet -> "kronisk-krav"
        }
    return "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/$type/$id.pdf"
}

fun FritakSoeknadMelding.toPdfUrl(): String {
    val type =
        when (this) {
            is GravidSoeknadOpprettet -> "gravid-soeknad"
            is KroniskSoeknadOpprettet -> "kronisk-soeknad"
        }
    return "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/$type/$id.pdf"
}

fun FritakKravMelding.toTittel(): String =
    when (this) {
        is GravidKravOpprettet -> "Krav om fritak for arbeidsgiverperiode ved graviditet er opprettet"
        is GravidKravEndret -> "Krav om fritak for arbeidsgiverperiode ved graviditet er endret"
        is GravidKravSlettet -> "Krav om fritak for arbeidsgiverperiode ved graviditet er annullert"
        is KroniskKravOpprettet -> "Krav om fritak for arbeidsgiverperiode ved kronisk sykdom er opprettet"
        is KroniskKravEndret -> "Krav om fritak for arbeidsgiverperiode ved kronisk sykdom er endret"
        is KroniskKravSlettet -> "Krav om fritak for arbeidsgiverperiode ved kronisk sykdom er annullert"
    }

fun lagMarkerSomLestUrl(
    transmissionId: UUID,
    extendedType: String,
): String {
    val baseUrl = Env.Nav.arbeidsgiverGuiBaseUrl
    return "$baseUrl/dokument/fce/sett-transmission-lest?transmissionId=$transmissionId&extendedType=$extendedType"
}

abstract class TransmissionRequestMedLestFce(
    extendedType: ExtendedType,
) : TransmissionRequest() {
    val transmissionId = nyUuidv7()
    final override val extendedType = extendedType.toString()
    final override val contentReferenceFceUrl = lagMarkerSomLestUrl(transmissionId = transmissionId, extendedType = this.extendedType)
    final override val id = transmissionId
}
