package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel

fun DialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
    val dialog =
        dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId)
            ?: run {
                logger.warn(
                    "Fant ikke dialog for sykmeldingId ${inntektsmeldingsforespoersel.sykmeldingId}. " +
                        "Klarer derfor ikke oppdatere dialogen med inntektsmeldingforespørsel ${inntektsmeldingsforespoersel.forespoerselId}.",
                )
                return
            }

    runBlocking {
        val transmissionId =
            dialogportenClient.addTransmission(
                dialogId = dialog.dialogId,
                transmission =
                    lagTransmissionMedVedlegg(
                        ForespoerselTransmissionRequest(inntektsmeldingsforespoersel),
                    ),
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
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/im-dialog/${inntektsmeldingsforespoersel.forespoerselId}",
                    action = Action.READ.value,
                    title = listOf(ContentValueItem("Send inn inntektsmelding")),
                    priority = GuiAction.Priority.Primary,
                ),
        )
        dialogRepository.oppdaterDialogMedTransmission(
            sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId,
            transmissionId = transmissionId,
            dokumentId = inntektsmeldingsforespoersel.forespoerselId,
            dokumentType = LpsApiExtendedType.FORESPOERSEL_AKTIV.toString(),
            relatedTransmissionId = transmissionId,
        )
        logger.info(
            "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${inntektsmeldingsforespoersel.sykmeldingId} " +
                "med forespørsel om inntektsmelding med id ${inntektsmeldingsforespoersel.forespoerselId}." +
                "Lagt til transmission $transmissionId.",
        )
    }
}
