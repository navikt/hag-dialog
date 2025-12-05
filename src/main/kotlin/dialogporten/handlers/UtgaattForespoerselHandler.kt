package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.UtgaattForespoerselTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class UtgaattForespoerselHandler(
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
                            "Klarer derfor ikke oppdatere dialogen med oppdatert inntektsmeldingforespørsel $forespoerselId.",
                    )
                    return
                }

        val relatedTransmissionId =
            dialog.transmissionByDokumentId(forespoerselId)?.relatedTransmissionId
                ?: run {
                    logger.warn(
                        "Fant ikke transmission for utgått forespørselId $forespoerselId " +
                            "i dialog ${dialog.dialogId} for sykmeldingId $sykmeldingId. " +
                            "Klarer derfor ikke tilknytte utgått inntektsmeldingforespørsel $forespoerselId med utgått forespørsel.",
                    )
                    null
                }

        val transmissionId =
            runBlocking {
                dialogportenClient
                    .addTransmission(
                        dialogId = dialog.dialogId,
                        transmissionRequest =
                            utgaattForespoerselTransmissionRequest(
                                forespoerselId = forespoerselId,
                                relatedTransmissionId = relatedTransmissionId,
                            ),
                    ).also {
                        dialogportenClient.removeActionsAndStatus(dialog.dialogId)
                    }
            }

        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = forespoerselId,
            dokumentType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString(),
            relatedTransmissionId = relatedTransmissionId,
        )

        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding $sykmeldingId " +
                "med utgått forespørsel om inntektsmelding med id $forespoerselId. " +
                "Lagt til transmission $transmissionId.",
        )
    }
}

fun utgaattForespoerselTransmissionRequest(
    forespoerselId: UUID,
    relatedTransmissionId: UUID? = null,
): TransmissionRequest =
    UtgaattForespoerselTransmissionRequest(
        forespoerselId = forespoerselId,
        relatedTransmissionId,
        listOf(
            createApiAttachment(
                displayName = "inntektsmeldingforespoersel.json",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/$forespoerselId",
            ),
        ),
    )
