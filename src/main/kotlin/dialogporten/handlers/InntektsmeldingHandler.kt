package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.InntektsmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.toExtendedType
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class InntektsmeldingHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(inntektsmelding: Inntektsmelding) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(inntektsmelding.sykmeldingId) ?: run {
                logger.warn(
                    "Fant ikke dialog for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                        "Klarer derfor ikke oppdatere dialogen med inntektsmelding ${inntektsmelding.innsendingId}.",
                )
                return
            }

        val forespoerselTransmission =
            dialog.transmissionByDokumentId(inntektsmelding.forespoerselId)
                ?: run {
                    logger.warn(
                        "Fant ikke transmission for forespørselId ${inntektsmelding.forespoerselId} " +
                            "i dialog ${dialog.dialogId} for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                            "Klarer derfor ikke tilknytte inntektsmelding ${inntektsmelding.innsendingId} med forespørsel.",
                    )

                    return
                }

        val transmissionId =
            runBlocking {
                dialogportenClient
                    .addTransmission(
                        dialog.dialogId,
                        inntektsmeldingTransmissionRequest(
                            inntektsmelding = inntektsmelding,
                            relatedTransmissionId = forespoerselTransmission.relatedTransmissionId,
                        ),
                    ).also {
                        dialogportenClient.setDialogStatus(dialog.dialogId, DialogStatus.NotApplicable)
                        if (inntektsmelding.status == Inntektsmelding.Status.GODKJENT) {
                            val attachmentNavn = "Se og endre inntektsmelding"
                            dialogportenClient.replaceAttachmentsAndActions(
                                dialogId = dialog.dialogId,
                                attachments = emptyList(),
                                apiActions = emptyList(),
                                guiActions =
                                    listOf(
                                        GuiAction(
                                            name = attachmentNavn,
                                            url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/im-dialog/${inntektsmelding.forespoerselId}",
                                            action = Action.READ.value,
                                            title = listOf(ContentValueItem(attachmentNavn)),
                                            priority = GuiAction.Priority.Secondary,
                                        ),
                                    ),
                            )
                        }
                    }
            }

        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = inntektsmelding.sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = inntektsmelding.innsendingId,
            dokumentType = inntektsmelding.status.toExtendedType(),
            relatedTransmissionId = forespoerselTransmission.relatedTransmissionId,
        )

        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${inntektsmelding.sykmeldingId}" +
                " med inntektsmelding ${inntektsmelding.innsendingId}. " +
                "Lagt til transmission $transmissionId.",
        )
    }
}

fun inntektsmeldingTransmissionRequest(
    inntektsmelding: Inntektsmelding,
    relatedTransmissionId: UUID?,
): TransmissionRequest {
    val apiAttachment =
        createApiAttachment(
            displayName = "inntektsmelding.json",
            url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/inntektsmelding/${inntektsmelding.innsendingId}",
        )
    val guiAttachment =
        createGuiAttachment(
            displayName = "Se og endre inntektsmelding",
            url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/im-dialog/${inntektsmelding.forespoerselId}",
        )
    val attachments =
        if (inntektsmelding.status == Inntektsmelding.Status.FEILET) {
            listOf(apiAttachment) // feilet så vises bare for API, ikke i GUI.
        } else {
            listOf(apiAttachment, guiAttachment)
        }
    return InntektsmeldingTransmissionRequest(
        inntektsmelding = inntektsmelding,
        relatedTransmissionId = relatedTransmissionId,
        attachments = attachments,
    )
}
