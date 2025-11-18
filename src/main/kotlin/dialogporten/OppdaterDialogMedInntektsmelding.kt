package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding

val status = DialogStatus.NotApplicable

fun DialogportenService.oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
    val dialog =
        dialogRepository.finnDialogMedSykemeldingId(inntektsmelding.sykmeldingId) ?: run {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med inntektsmelding ${inntektsmelding.innsendingId}.",
            )
            return
        }

    // TODO: Vurderer om vi skal også sjekke om forespørselen ikke er utgått
    val forespoerselTransmission =
        dialog.transmissionByDokumentId(inntektsmelding.forespoerselId)
            ?: run {
                logger.warn(
                    "Fant ikke transmission for forespørselId ${inntektsmelding.forespoerselId} " +
                        "i dialog ${dialog.dialogId} for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                        "Klarer derfor ikke tilknytte inntektsmelding ${inntektsmelding.innsendingId} med forespørsel.",
                )
                // TODO: Vurdere om vi skal lagre inntektsmeldingen uten å knytte den til en forespørsel, dersom forespørselen ikke finnes i databasen.
                return
            }

    val transmissionId =
        runBlocking {
            dialogportenClient
                .addTransmission(
                    dialog.dialogId,
                    lagTransmissionMedVedlegg(
                        InntektsmeldingTransmissionRequest(
                            inntektsmelding = inntektsmelding,
                            relatedTransmissionId = forespoerselTransmission.relatedTransmissionId,
                        ),
                    ),
                ).also {
                    dialogportenClient.setDialogStatus(dialog.dialogId, status)
                }
        }

    dialogRepository.oppdaterDialogMedTransmission(
        sykmeldingId = inntektsmelding.sykmeldingId,
        transmissionId = transmissionId,
        dokumentId = inntektsmelding.innsendingId,
        dokumentType = inntektsmelding.status.toExtendedType(),
        relatedTransmission = forespoerselTransmission.relatedTransmissionId,
    )

    logger.info(
        "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${inntektsmelding.sykmeldingId}" +
            " med inntektsmelding ${inntektsmelding.innsendingId}. " +
            "Lagt til transmission $transmissionId.",
    )
}
