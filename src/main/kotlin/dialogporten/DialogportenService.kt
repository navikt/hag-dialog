package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Inntektsmelding
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId = opprettNyDialogMedSykmelding(sykmelding)
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        val dialog =
            dialogRepository.finnDialogIdMedSykemeldingId(sykmeldingId = sykepengesoeknad.sykmeldingId)
                ?: run {
                    logger.warn(
                        "Fant ikke dialog for sykmeldingId ${sykepengesoeknad.sykmeldingId}. " +
                            "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
                    )
                    return
                }

        runBlocking {
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmission =
                        lagTransmissionMedVedlegg(
                            SykepengesoknadTransmissionRequest(sykepengesoeknad),
                        ),
                )

            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykepengesoeknad.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = sykepengesoeknad.soeknadId,
                dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
            )

            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${sykepengesoeknad.sykmeldingId} " +
                    "med sykepengesøknad ${sykepengesoeknad.soeknadId}. " +
                    "Lagt til transmission $transmissionId.",
            )
        }
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        val dialog =
            dialogRepository.finnDialogIdMedSykemeldingId(sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId)
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
                relatedTransmission = transmissionId,
            )
            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${inntektsmeldingsforespoersel.sykmeldingId} " +
                    "med forespørsel om inntektsmelding med id ${inntektsmeldingsforespoersel.forespoerselId}." +
                    "Lagt til transmission $transmissionId.",
            )
        }
    }

    fun oppdaterDialogMedInntektsmelding(inntektsmelding: Inntektsmelding) {
        val dialog =
            dialogRepository.finnDialogIdMedSykemeldingId(inntektsmelding.sykmeldingId) ?: run {
                logger.warn(
                    "Fant ikke dialog for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                        "Klarer derfor ikke oppdatere dialogen med inntektsmelding ${inntektsmelding.innsendingId}.",
                )
                return
            }
        runBlocking {
            // TODO: Vurderer om vi skla også sjekke om foresporselen ikke er utgått
            val forespoerselTransmission =
                dialog.transmissionByDokumentId(inntektsmelding.forespoerselId)

            forespoerselTransmission ?: run {
                logger.warn(
                    "Fant ikke transmission for forespørselId ${inntektsmelding.forespoerselId} " +
                        "i dialog ${dialog.dialogId} for sykmeldingId ${inntektsmelding.sykmeldingId}. " +
                        "Klarer derfor ikke tilknytte inntektsmelding ${inntektsmelding.innsendingId} med forespørsel.",
                )
                // TODO: Vurdere om vi skal lagre inntektsmeldingen uten å knytte den til en forespørsel
                return@runBlocking
            }

            val transmissionId =
                dialogportenClient.addTransmission(
                    dialog.dialogId,
                    lagTransmissionMedVedlegg(
                        InntektsmeldingTransmissionRequest(
                            inntektsmelding = inntektsmelding,
                            relatedTransmissionId = forespoerselTransmission.relatedTransmission,
                        ),
                    ),
                )
            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = inntektsmelding.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = inntektsmelding.innsendingId,
                dokumentType = inntektsmelding.status.toExtendedType(),
                relatedTransmission = forespoerselTransmission.relatedTransmission,
            )
            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${inntektsmelding.sykmeldingId}" +
                    " med inntektsmelding ${inntektsmelding.innsendingId}. " +
                    "Lagt til transmission $transmissionId.",
            )
        }
    }

    private fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): UUID =
        runBlocking {
            val request =
                CreateDialogRequest(
                    orgnr = sykmelding.orgnr,
                    externalReference = sykmelding.sykmeldingId.toString(),
                    title =
                        "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                    summary =
                        sykmelding.sykmeldingsperioder
                            .getSykmeldingsPerioderString(),
                    transmissions =
                        listOf(
                            lagTransmissionMedVedlegg(
                                SykmeldingTransmissionRequest(sykmelding),
                            ),
                        ),
                    isApiOnly = unleashFeatureToggles.skalOppretteDialogKunForApi(),
                )
            dialogportenClient.createDialog(request)
        }
}

fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
    when (size) {
        1 -> "Sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
        else ->
            "Sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
    }
