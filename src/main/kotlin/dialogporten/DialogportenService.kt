package no.nav.helsearbeidsgiver.dialogporten

import io.ktor.http.ContentType
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.Content
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.Transmission
import no.nav.helsearbeidsgiver.dialogporten.domene.lagContentValue
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogportenClient: DialogportenClient,
    private val dialogRepository: DialogRepository,
    private val dialogportenKlient: DialogportenKlient,
    private val ressurs: String,
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
                    dialogportenKlient.addTransmission(
                        dialogId,
                        lagVedleggTransmission(
                            transmissionTittel = "Søknad om sykepenger",
                            vedleggType = Transmission.ExtendedType.SYKEPENGESOEKNAD,
                            vedleggNavn = "soeknad-om-sykepenger.json",
                            vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}",
                            vedleggMediaType = ContentType.Application.Json.toString(),
                            vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
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
                    dialogportenKlient.addTransmission(
                        dialogId,
                        lagVedleggTransmission(
                            transmissionTittel = "Forespørsel om inntektsmelding",
                            vedleggType = Transmission.ExtendedType.INNTEKTSMELDING,
                            vedleggNavn = "Inntektsmeldingforespoersel.json",
                            vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${inntektsmeldingsforespoersel.forespoerselId}",
                            vedleggMediaType = ContentType.Application.Json.toString(),
                            vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
                        ),
                    )

                dialogportenKlient.addAction(
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
                    serviceResource = "urn:altinn:resource:$ressurs",
                    party = "urn:altinn:organization:identifier-no:${sykmelding.orgnr}",
                    externalRefererence = sykmelding.sykmeldingId.toString(),
                    status = DialogStatus.New,
                    content =
                        Content(
                            title =
                                "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
                                    .lagContentValue(),
                            summary =
                                sykmelding.sykmeldingsperioder
                                    .getSykmeldingsPerioderString()
                                    .lagContentValue(),
                        ),
                    transmissions =
                        listOf(
                            lagVedleggTransmission(
                                transmissionTittel = "Sykmelding",
                                vedleggType = Transmission.ExtendedType.SYKMELDING,
                                vedleggNavn = "Sykmelding.json",
                                vedleggUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/${sykmelding.sykmeldingId}",
                                vedleggMediaType = ContentType.Application.Json.toString(),
                                vedleggConsumerType = Transmission.AttachmentUrlConsumerType.Api,
                            ),
                        ),
                    isApiOnly = true,
                )
            dialogportenKlient.createDialog(request)
        }
}

fun lagVedleggTransmission(
    transmissionTittel: String,
    transmissionSammendrag: String? = null,
    vedleggType: Transmission.ExtendedType,
    vedleggNavn: String,
    vedleggUrl: String,
    vedleggMediaType: String,
    vedleggConsumerType: Transmission.AttachmentUrlConsumerType,
): Transmission =
    Transmission(
        type = Transmission.TransmissionType.Information,
        extendedType = vedleggType,
        sender = Transmission.Sender("ServiceOwner"),
        content =
            Content(
                title = transmissionTittel.lagContentValue(),
                summary = transmissionSammendrag?.lagContentValue(),
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
