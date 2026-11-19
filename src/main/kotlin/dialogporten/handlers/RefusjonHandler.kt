package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.RefusjonTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Refusjon
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class RefusjonHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(refusjon: Refusjon) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = refusjon.sykmeldingId)
        if (dialog == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${refusjon.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med refusjon ${refusjon.refusjonUtfallId}.",
            )
            return
        }

        val eksisterendeTransmission = dialog.transmissionByDokumentId(refusjon.refusjonUtfallId)
        if (eksisterendeTransmission != null) {
            logger.info(
                "Transmission for refusjon ${refusjon.refusjonUtfallId} finnes allerede i dialog ${dialog.dialogId}, hopper over opprettelse.",
            )
            return
        }

        val inntektsmeldingTransmissionId = dialog.transmissionByDokumentId(refusjon.inntektsmeldingId)?.transmissionId
        if (inntektsmeldingTransmissionId == null) {
            logger.warn(
                "Fant ikke transmission for inntektsmeldingId ${refusjon.inntektsmeldingId} " +
                    "i dialog ${dialog.dialogId} for sykmeldingId ${refusjon.sykmeldingId}. " +
                    "Oppretter transmission for refusjon ${refusjon.refusjonUtfallId} uten relatedTransmissionId.",
            )
        }

        val transmissionId =
            runBlocking {
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmissionRequest =
                        refusjonTransmissionRequest(
                            refusjonUtfallId = refusjon.refusjonUtfallId,
                            relatedTransmissionId = inntektsmeldingTransmissionId,
                        ),
                )
            }

        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = refusjon.sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = refusjon.refusjonUtfallId,
            dokumentType = LpsApiExtendedType.REFUSJON.toString(),
            relatedTransmissionId = inntektsmeldingTransmissionId,
        )

        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${refusjon.sykmeldingId} " +
                "med refusjon ${refusjon.refusjonUtfallId}. Lagt til transmission $transmissionId.",
        )
    }
}

fun refusjonTransmissionRequest(
    refusjonUtfallId: UUID,
    relatedTransmissionId: UUID?,
): TransmissionRequest =
    RefusjonTransmissionRequest(
        refusjonUtfallId = refusjonUtfallId,
        relatedTransmissionId = relatedTransmissionId,
        attachments =
            listOf(
//                TODO: Leggg til dette når refusjon er på plass i LPS API
//                createApiAttachment(
//                    "refusjon.json",
//                    "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/refusjon/$refusjonUtfallId",
//                ),
//                createApiAttachment(
//                    displayName = "refusjon.pdf",
//                    url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/refusjon/$refusjonUtfallId/pdf",
//                    mediaType = "application/pdf",
//                ),
                createGuiAttachment(
                    displayName = "refusjon av sykepenger",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/refusjon/$refusjonUtfallId.pdf",
                    mediaType = "application/pdf",
                ),
            ),
    )
