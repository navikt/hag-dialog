package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.UtgaattForespoerselTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class UtgaattForespoerselHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = utgaattForespoersel.sykmeldingId)
                ?: run {
                    logger.warn(
                        "Fant ikke dialog for sykmeldingId ${utgaattForespoersel.sykmeldingId}. " +
                            "Klarer derfor ikke oppdatere dialogen med oppdatert inntektsmeldingforespørsel ${utgaattForespoersel.forespoerselId}.",
                    )
                    return
                }

        val relatedTransmissionId =
            dialog.transmissionByDokumentId(utgaattForespoersel.forespoerselId)?.relatedTransmissionId
                ?: run {
                    logger.warn(
                        "Fant ikke transmission for utgått forespørselId ${utgaattForespoersel.forespoerselId} " +
                            "i dialog ${dialog.dialogId} for sykmeldingId ${utgaattForespoersel.sykmeldingId}. " +
                            "Klarer derfor ikke tilknytte utgått inntektsmeldingforespørsel ${utgaattForespoersel.forespoerselId} med utgått forespørsel.",
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
                                utgaattForespoersel = utgaattForespoersel,
                                relatedTransmissionId = relatedTransmissionId,
                            ),
                    ).also {
                        dialogportenClient.removeActionsAndStatus(dialog.dialogId)
                    }
            }

        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = utgaattForespoersel.sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = utgaattForespoersel.forespoerselId,
            dokumentType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString(),
            relatedTransmissionId = relatedTransmissionId,
        )

        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${utgaattForespoersel.sykmeldingId} " +
                "med utgått forespørsel om inntektsmelding med id ${utgaattForespoersel.forespoerselId}. " +
                "Lagt til transmission $transmissionId.",
        )
    }
}

fun utgaattForespoerselTransmissionRequest(
    utgaattForespoersel: UtgaattInntektsmeldingForespoersel,
    relatedTransmissionId: UUID? = null,
): TransmissionRequest =
    UtgaattForespoerselTransmissionRequest(
        utgaattForespoersel,
        relatedTransmissionId,
        listOf(
            createApiAttachment(
                displayName = "inntektsmeldingforespoersel.json",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${utgaattForespoersel.forespoerselId}",
            ),
        ),
    )
