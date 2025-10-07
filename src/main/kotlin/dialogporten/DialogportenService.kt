package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.DialogRepository
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.dialogporten.domene.Content
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.DialogStatus
import no.nav.helsearbeidsgiver.dialogporten.domene.lagContentValue
import no.nav.helsearbeidsgiver.kafka.Inntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class DialogportenService(
    private val dialogportenClient: DialogportenClient,
    private val dialogRepository: DialogRepository,
    private val dialogportenKlient: DialogportenKlient,
    private val ressurs: String,
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
                dialogportenClient.oppdaterDialogMedSykepengesoeknad(
                    dialogId = dialogId,
                    soeknadJsonUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}",
                )
            }
            logger.info(
                "Oppdaterte dialog $dialogId tilhørende sykmelding ${sykepengesoeknad.sykmeldingId} med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
            )
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
                dialogportenClient.oppdaterDialogMedInntektsmeldingsforespoersel(
                    dialogId = dialogId,
                    forespoerselUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/forespoersel/${inntektsmeldingsforespoersel.forespoerselId}",
                    forespoerselDokumentasjonUrl = "${Env.Nav.arbeidsgiverApiBaseUrl}/swagger",
                )
            }
            logger.info(
                "Oppdaterte dialog $dialogId tilhørende sykmelding ${inntektsmeldingsforespoersel.sykmeldingId} " +
                    "med forespørsel om inntektsmelding med id ${inntektsmeldingsforespoersel.forespoerselId}.",
            )
        }
    }

    private fun opprettNyDialogMedSykmelding(sykmelding: Sykmelding): UUID =
        runBlocking {
            val request =
                CreateDialogRequest(
                    serviceResource = ressurs,
                    party = "urn:altinn:organization:identifier-no:${sykmelding.orgnr}",
                    externalRefererence = sykmelding.sykmeldingId.toString(),
                    status = DialogStatus.New,
                    content =
                        Content(
                            title =
                                "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})"
                                    .lagContentValue(),
                            summary =
                                sykmelding.sykmeldingsperioder
                                    .getSykmeldingsPerioderString()
                                    .lagContentValue(),
                        ),
                    transmissions = emptyList(),
                    isApiOnly = true,
                )
            dialogportenKlient.createDialog(request)
        }

    private fun List<Sykmeldingsperiode>.getSykmeldingsPerioderString(): String =
        when (size) {
            1 -> "Sykmeldingsperiode ${first().fom.tilNorskFormat()} – ${first().tom.tilNorskFormat()}"
            else ->
                "Sykmeldingsperioder ${first().fom.tilNorskFormat()} – (...) – ${last().tom.tilNorskFormat()}"
        }
}
