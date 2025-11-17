package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.UtgaattInntektsmeldingForespoersel

fun DialogportenService.oppdaterDialogMedUtgaattForespoersel(utgaattForespoersel: UtgaattInntektsmeldingForespoersel) {
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
            dialogportenClient.addTransmission(
                dialogId = dialog.dialogId,
                transmission =
                    lagTransmissionMedVedlegg(
                        UtgaatForespoerselTransmissionRequest(
                            utgaattForespoersel,
                            relatedTransmissionId = relatedTransmissionId,
                        ),
                    ),
            )
        }

    dialogRepository.oppdaterDialogMedTransmission(
        sykmeldingId = utgaattForespoersel.sykmeldingId,
        transmissionId = transmissionId,
        dokumentId = utgaattForespoersel.forespoerselId,
        dokumentType = LpsApiExtendedType.FORESPOERSEL_UTGAATT.toString(),
        relatedTransmission = relatedTransmissionId,
    )

    logger.info(
        "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${utgaattForespoersel.sykmeldingId} " +
            "med oppdatert forespørsel om inntektsmelding med id ${utgaattForespoersel.forespoerselId}. " +
            "Lagt til transmission $transmissionId.",
    )
}
