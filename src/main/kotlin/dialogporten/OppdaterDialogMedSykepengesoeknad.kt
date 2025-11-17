package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.dialogporten.domene.lagTransmissionMedVedlegg
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.log.logger

fun DialogportenService.oppdaterDialogMedSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
    val dialog =
        dialogRepository.finnDialogMedSykemeldingId(sykmeldingId = sykepengesoeknad.sykmeldingId)
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
