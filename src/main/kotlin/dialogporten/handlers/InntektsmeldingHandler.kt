package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.InntektsmeldingKorrigertTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.InntektsmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.dialogporten.toExtendedType
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.utils.log.logger

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
                        dialogportenClient.setDialogStatus(dialog.dialogId, DialogStatus.NotApplicable)
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

    fun oppdaterDialogMedKorrigertInntektsmelding(korrigertInntektsmelding: Inntektsmelding) {
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(korrigertInntektsmelding.sykmeldingId) ?: run {
                logger.warn(
                    "Fant ikke dialog for sykmeldingId ${korrigertInntektsmelding.sykmeldingId}. " +
                        "Klarer derfor ikke oppdatere dialogen med inntektsmelding ${korrigertInntektsmelding.innsendingId}.",
                )
                return
            }
        val forespoerselTransmission =
            dialog.transmissionByDokumentId(korrigertInntektsmelding.forespoerselId)
                ?: run {
                    logger.warn(
                        "Fant ikke transmission for forespørselId ${korrigertInntektsmelding.forespoerselId} " +
                            "i dialog ${dialog.dialogId} for sykmeldingId ${korrigertInntektsmelding.sykmeldingId}. " +
                            "Klarer derfor ikke tilknytte inntektsmelding ${korrigertInntektsmelding.innsendingId} med forespørsel.",
                    )
                    // TODO: Vurdere om vi skal lagre inntektsmeldingen uten å knytte den til en forespørsel, dersom forespørselen ikke finnes i databasen.
                    return
                }

        if (skalOppretteKorrigeringTransmission(korrigertInntektsmelding)) {
            val transmissionId =
                runBlocking {
                    dialogportenClient
                        .addTransmission(
                            dialog.dialogId,
                            lagTransmissionMedVedlegg(
                                InntektsmeldingKorrigertTransmissionRequest(
                                    inntektsmelding = korrigertInntektsmelding,
                                    relatedTransmissionId = forespoerselTransmission.relatedTransmissionId,
                                ),
                            ),
                        ).also {
                            dialogportenClient.setDialogStatus(dialog.dialogId, DialogStatus.NotApplicable)
                        }
                }
            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = korrigertInntektsmelding.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = korrigertInntektsmelding.innsendingId,
                dokumentType = LpsApiExtendedType.INNTEKTSMELDING_KORRIGERT.toString(),
                relatedTransmissionId = forespoerselTransmission.relatedTransmissionId,
            )
        } else {
            oppdaterDialog(korrigertInntektsmelding)
        }
    }
}

private fun skalOppretteKorrigeringTransmission(inntektsmelding: Inntektsmelding): Boolean =
    when (inntektsmelding.kilde) {
        Inntektsmelding.Kilde.NAV_PORTAL -> true
        Inntektsmelding.Kilde.API -> inntektsmelding.status == Inntektsmelding.Status.MOTTATT
    }
