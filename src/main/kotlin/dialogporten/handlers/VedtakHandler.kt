package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.VedtakTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Vedtak
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class VedtakHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(vedtak: Vedtak) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = vedtak.sykmeldingId)
        if (dialog == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${vedtak.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med vedtak ${vedtak.vedtakId}.",
            )
            return
        }

        val eksisterendeTransmission = dialog.transmissionByDokumentId(vedtak.vedtakId)
        if (eksisterendeTransmission != null) {
            logger.info(
                "Transmission for vedtak ${vedtak.vedtakId} finnes allerede i dialog ${dialog.dialogId}, hopper over opprettelse.",
            )
            return
        }

        val inntektsmeldingTransmissionId = dialog.transmissionByDokumentId(vedtak.inntektsmeldingId)?.transmissionId
        if (inntektsmeldingTransmissionId == null) {
            logger.warn(
                "Fant ikke transmission for inntektsmeldingId ${vedtak.inntektsmeldingId} " +
                    "i dialog ${dialog.dialogId} for sykmeldingId ${vedtak.sykmeldingId}. " +
                    "Oppretter transmission for vedtak ${vedtak.vedtakId} uten relatedTransmissionId.",
            )
        }

        val transmissionId =
            runBlocking {
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmissionRequest =
                        vedtakTransmissionRequest(
                            vedtakId = vedtak.vedtakId,
                            relatedTransmissionId = inntektsmeldingTransmissionId,
                        ),
                )
            }

        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = vedtak.sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = vedtak.vedtakId,
            dokumentType = LpsApiExtendedType.VEDTAK.toString(),
            relatedTransmissionId = inntektsmeldingTransmissionId,
        )

        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${vedtak.sykmeldingId} " +
                "med vedtak ${vedtak.vedtakId}. Lagt til transmission $transmissionId.",
        )
    }
}

fun vedtakTransmissionRequest(
    vedtakId: UUID,
    relatedTransmissionId: UUID?,
): TransmissionRequest =
    VedtakTransmissionRequest(
        vedtakId = vedtakId,
        relatedTransmissionId = relatedTransmissionId,
        attachments =
            listOf(
//                TODO: Leggg til dette når refusjon er på plass i LPS API
//                createApiAttachment(
//                    "refusjon.json",
//                    "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/refusjon/$vedtakId",
//                ),
//                createApiAttachment(
//                    displayName = "refusjon.pdf",
//                    url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/refusjon/$vedtakId/pdf",
//                    mediaType = "application/pdf",
//                ),
                createGuiAttachment(
                    displayName = "refusjon",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/refusjon/$vedtakId.pdf",
                    mediaType = "application/pdf",
                ),
            ),
    )
