package no.nav.helsearbeidsgiver.dialogporten

import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.Content
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.create
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId = opprettNyDialogMedSykmelding(sykmelding)
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        val dialogId = dialogRepository.finnDialogId(sykmeldingId = sykepengesoeknad.sykmeldingId)
        if (dialogId == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${sykepengesoeknad.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
            )
        } else {
            runBlocking {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId,
                        lagTransmissionMedVedlegg(
                            transmissionTittel = "Søknad om sykepenger",
                            extendedType = LpsApiExtendedType.SYKEPENGESOEKNAD,
                            vedleggNavn = "soeknad-om-sykepenger.json",
                            vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}",
                            vedleggMediaType = ContentType.Application.Json.toString(),
                            vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
                            type = Transmission.TransmissionType.Information,
                        ),
                    )
                logger.info(
                    "Oppdaterte dialog $dialogId for sykmelding ${sykepengesoeknad.sykmeldingId}" +
                        " med sykepengesøknad ${sykepengesoeknad.soeknadId}. " +
                        "Lagt til transmission $transmissionId.",
                )
            }
        }
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        val dialogId = dialogRepository.finnDialogId(sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId)
        if (dialogId == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${inntektsmeldingsforespoersel.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med inntektsmeldingforespørsel ${inntektsmeldingsforespoersel.forespoerselId}.",
            )
        } else {
            runBlocking {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId,
                        lagTransmissionMedVedlegg(
                            transmissionTittel = "Forespørsel om inntektsmelding",
                            extendedType = LpsApiExtendedType.INNTEKTSMELDING,
                            vedleggNavn = "Inntektsmeldingforespoersel.json",
                            vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${inntektsmeldingsforespoersel.forespoerselId}",
                            vedleggMediaType = ContentType.Application.Json.toString(),
                            vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
                            type = Transmission.TransmissionType.Request,
                        ),
                    )

                dialogportenClient.addAction(
                    dialogId,
                    ApiAction(
                        name = "Send inn inntektsmelding",
                        endpoints =
                            listOf(
                                ApiAction.Endpoint(
                                    url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/inntektsmelding",
                                    httpMethod = ApiAction.HttpMethod.POST,
                                    documentationUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/swagger",
                                ),
                            ),
                        action = ApiAction.Action.WRITE.value,
                    ),
                )
                logger.info(
                    "Oppdaterte dialog $dialogId for sykmelding ${inntektsmeldingsforespoersel.sykmeldingId} " +
                        "med forespørsel om inntektsmelding med id ${inntektsmeldingsforespoersel.forespoerselId}." +
                        "Lagt til transmission $transmissionId.",
                )
            }
        }
    }

    private fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): UUID =
        runBlocking {
            val request =
                CreateDialogRequest(
                    orgnr = sykmelding.orgnr,
                    externalReference = sykmelding.sykmeldingId.toString(),
                    title =
                        "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                    summary =
                        sykmelding.sykmeldingsperioder
                            .getSykmeldingsPerioderString(),
                    transmissions =
                        listOf(
                            lagTransmissionMedVedlegg(
                                transmissionTittel = "Sykmelding",
                                extendedType = LpsApiExtendedType.SYKMELDING,
                                vedleggNavn = "Sykmelding.json",
                                vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}",
                                vedleggMediaType = ContentType.Application.Json.toString(),
                                vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
                                type = Transmission.TransmissionType.Information,
                            ),
                        ),
                    isApiOnly = true,
                )
            dialogportenClient.createDialog(request)
        }
}

fun lagTransmissionMedVedlegg(
    transmissionTittel: String,
    transmissionSammendrag: String? = null,
    extendedType: LpsApiExtendedType,
    vedleggNavn: String,
    vedleggUrl: String,
    vedleggMediaType: String,
    vedleggConsumerType: Transmission.AttachmentUrlConsumerType,
    type: Transmission.TransmissionType,
): Transmission =
    Transmission(
        type = type,
        extendedType = extendedType.toString(),
        sender = Transmission.Sender("ServiceOwner"),
        content =
            Content.create(
                title = transmissionTittel,
                summary = transmissionSammendrag,
            ),
        attachments =
            listOf(
                Transmission.Attachment(
                    displayName = listOf(ContentValueItem(vedleggNavn)),
                    urls =
                        listOf(
                            Transmission.Url(
                                url = vedleggUrl,
                                mediaType = vedleggMediaType,
                                consumerType = vedleggConsumerType,
                            ),
                        ),
                ),
            ),
    )

fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
    when (size) {
        1 -> "Sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
        else ->
            "Sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
    }
