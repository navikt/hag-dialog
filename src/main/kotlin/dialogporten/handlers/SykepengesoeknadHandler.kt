package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.database.NotifikasjonRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.SykepengesoknadTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class SykepengesoeknadHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val notifikasjonRepository: NotifikasjonRepository,
    private val unleashFeatureToggles: UnleashFeatureToggles,
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

        val eksisterendeTransmission = dialog.transmissionByDokumentId(sykepengesoeknad.soeknadId)

        if (eksisterendeTransmission != null) {
            logger.info(
                "Transmission for sykepengesøknad ${sykepengesoeknad.soeknadId} " +
                    "finnes allerede i dialog ${dialog.dialogId}, hopper over opprettelse.",
            )
        } else {
            val korrigertSoknadTransmissionId = hentKorrigertSoknadTransmissionId(sykepengesoeknad)
            val transmissionId =
                runBlocking {
                    dialogportenClient.removeApiOnly(dialog.dialogId)
                    dialogportenClient.addTransmission(
                        dialogId = dialog.dialogId,
                        transmissionRequest =
                            sykepengesoknadTransmission(
                                soeknadId = sykepengesoeknad.soeknadId,
                                korrigertTransmissionId = korrigertSoknadTransmissionId,
                            ),
                    )
                }

            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykepengesoeknad.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = sykepengesoeknad.soeknadId,
                dokumentType = LpsApiExtendedType.SYKEPENGESOEKNAD.toString(),
                relatedTransmissionId = korrigertSoknadTransmissionId,
            )

            logger.info(
                "Oppdaterte dialog ${dialog.dialogId} for sykmelding ${sykepengesoeknad.sykmeldingId} " +
                    "med sykepengesøknad ${sykepengesoeknad.soeknadId}. " +
                    "Lagt til transmission $transmissionId.",
            )
        }

        if (unleashFeatureToggles.skalOppretteNotifikasjoner()) {
            notifikasjonRepository.opprettNotifikasjon(
                dokumentId = sykepengesoeknad.soeknadId,
                tjeneste = Tjeneste.SOEKNAD,
            )
        }
    }

    private fun hentKorrigertSoknadTransmissionId(sykepengesoeknad: Sykepengesoeknad): UUID? {
        val korrigerer = sykepengesoeknad.korrigerer ?: return null

        return dialogRepository.hentTransmissionMedDokumentId(korrigerer)?.let { korrigertTransmission ->
            logger.info("soknaden ${sykepengesoeknad.soeknadId} har korrigert søknad $korrigerer")
            korrigertTransmission.id.value
        }
    }
}

fun sykepengesoknadTransmission(
    soeknadId: UUID,
    korrigertTransmissionId: UUID? = null,
    isSilentUpdate: Boolean = false, // TODO kan fjernes etter engangsjobb patcher transmission
): TransmissionRequest {
    val attachments =
        listOf(
            createApiAttachment(
                "sykepengesoeknad.json",
                "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/$soeknadId",
            ),
            createApiAttachment(
                displayName = "sykepengesoeknad.pdf",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykepengesoeknad/$soeknadId/pdf",
                mediaType = "application/pdf",
            ),
            createGuiAttachment(
                displayName = "sykepengesoeknad",
                url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf",
                mediaType = "application/pdf",
            ),
        )

    return if (korrigertTransmissionId == null) {
        SykepengesoknadTransmissionRequest(
            soeknadId = soeknadId,
            attachments = attachments,
            isSilentUpdate = isSilentUpdate,
        )
    } else {
        SykepengesoknadTransmissionRequest(
            soeknadId = soeknadId,
            attachments = attachments,
            isSilentUpdate = isSilentUpdate,
            tittel = "Søknad om sykepenger er endret",
            relatedTransmissionId = korrigertTransmissionId,
        )
    }
}
