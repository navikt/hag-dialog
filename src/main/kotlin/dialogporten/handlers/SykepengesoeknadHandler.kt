package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.SykepengesoknadTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.logger

class SykepengesoeknadHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(sykepengesoeknad: Sykepengesoeknad) {
        // TODO: Hvis ikke finnes - lage???
        val dialog =
            dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = sykepengesoeknad.sykmeldingId)
                ?: run {
                    logger.warn(
                        "Fant ikke dialog for sykmeldingId ${sykepengesoeknad.sykmeldingId}. " +
                            "Klarer derfor ikke oppdatere dialogen med sykepengesøknad ${sykepengesoeknad.soeknadId}.",
                    )
                    return
                }

        val transmissionId =
            runBlocking {
                dialogportenClient.removeApiOnly(dialog.dialogId)
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmissionRequest = sykepengesoknadTransmission(sykepengesoeknad = sykepengesoeknad),
                )
            }

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

fun sykepengesoknadTransmission(sykepengesoeknad: Sykepengesoeknad): TransmissionRequest =
    SykepengesoknadTransmissionRequest(
        sykepengesoeknad,
        listOf(
            createApiAttachment(
                "sykepengesoeknad.json",
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}",
            ),
            createApiAttachment(
                displayName = "sykepengesoeknad.pdf",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/${sykepengesoeknad.soeknadId}/pdf",
                mediaType = "application/pdf",
            ),
            createGuiAttachment(
                displayName = "sykepengesoeknad",
                url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/${sykepengesoeknad.soeknadId}.pdf",
                mediaType = "application/pdf",
            ),
        ),
    )
