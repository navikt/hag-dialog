package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.ApiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val dialogId = opprettNyDialogMedSykmelding(sykmelding)
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }

    fun oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        val dialogId = dialogRepository.finnDialogId(sykmeldingId = sykepengesoeknad.sykmeldingId)
        if (dialogId == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${sykepengesoeknad.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
            )
        } else {
            runBlocking {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId,
                        lagTransmissionMedVedlegg(
                            SykepengesoknadTransmissionRequest(sykepengesoeknad),
                        ),
                    )
                logger.info(
                    "Oppdaterte dialog $dialogId for sykmelding ${sykepengesoeknad.sykmeldingId}" +
                        " med sykepengesøknad ${sykepengesoeknad.soeknadId}. " +
                        "Lagt til transmission $transmissionId.",
                )
            }
        }
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(inntektsmeldingsforespoersel: Inntektsmeldingsforespoersel) {
        val dialogId = dialogRepository.finnDialogId(sykmeldingId = inntektsmeldingsforespoersel.sykmeldingId)
        if (dialogId == null) {
            logger.warn(
                "Fant ikke dialog for sykmeldingId ${inntektsmeldingsforespoersel.sykmeldingId}. " +
                    "Klarer derfor ikke oppdatere dialogen med inntektsmeldingforespørsel ${inntektsmeldingsforespoersel.forespoerselId}.",
            )
        } else {
            runBlocking {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId,
                        lagTransmissionMedVedlegg(
                            InntektsmeldingTransmissionRequest(inntektsmeldingsforespoersel),
                        ),
                    )

                dialogportenClient.addAction(
                    dialogId,
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
                        action = ApiAction.Action.WRITE.value,
                    ),
                )
                logger.info(
                    "Oppdaterte dialog $dialogId for sykmelding ${inntektsmeldingsforespoersel.sykmeldingId} " +
                        "med forespørsel om inntektsmelding med id ${inntektsmeldingsforespoersel.forespoerselId}." +
                        "Lagt til transmission $transmissionId.",
                )
            }
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
                    isApiOnly = true,
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
