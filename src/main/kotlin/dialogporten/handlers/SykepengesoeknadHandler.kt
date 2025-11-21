package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.SykepengesoknadTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.logger

class SykepengesoeknadHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
) {
    private val logger = logger()

    fun oppdaterDialog(sykepengesoeknad: Sykepengesoeknad) {
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
                dialogportenClient.addTransmission(
                    dialogId = dialog.dialogId,
                    transmission =
                        lagTransmissionMedVedlegg(
                            SykepengesoknadTransmissionRequest(sykepengesoeknad),
                        ),
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
