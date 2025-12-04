package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.ForespoerselTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class ForespoerselHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(
        forespoerselId: UUID,
        sykmeldingId: UUID,
    ) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = sykmeldingId)
                ?: run {
                    logger.warn(
                        "Fant ikke dialog for sykmeldingId $sykmeldingId. " +
                            "Klarer derfor ikke oppdatere dialogen med inntektsmeldingforespørsel $forespoerselId.",
                    )
                    return
                }

        runBlocking {
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmissionRequest = forespoerselTransmissionRequest(forespoerselId),
                )

            dialogportenClient.addAction(
                dialogId = dialog.dialogId,
                apiAction =
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
                        action = Action.WRITE.value,
                    ),
                guiActions =
                    GuiAction(
                        name = "Send inn inntektsmelding",
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/im-dialog/$forespoerselId",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem("Send inn inntektsmelding")),
                        priority = GuiAction.Priority.Primary,
                    ),
            )
            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = forespoerselId,
                dokumentType = LpsApiExtendedType.FORESPOERSEL_AKTIV.toString(),
                relatedTransmissionId = transmissionId,
            )
            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding $sykmeldingId " +
                    "med forespørsel om inntektsmelding med id $forespoerselId." +
                    "Lagt til transmission $transmissionId.",
            )
        }
    }
}

fun forespoerselTransmissionRequest(forespoerselId: UUID): TransmissionRequest =
    ForespoerselTransmissionRequest(
        forespoerselId = forespoerselId,
        attachments =
            listOf(
                createApiAttachment(
                    displayName = "inntektsmeldingforespoersel.json",
                    url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/$forespoerselId",
                ),
                createGuiAttachment(
                    displayName = "Se forespørsel i Arbeidsgiverportalen",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/im-dialog/$forespoerselId",
                ),
            ),
    )
