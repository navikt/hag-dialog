package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.DialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.LpsApiExtendedType
import no.nav.helsearbeidsgiver.dialogporten.SykmeldingTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.TransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.kafka.Sykmelding
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.kafka.lagDialogAdditionalInfo
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID

class SykmeldingHandler(
    private val dialogRepository: DialogRepository,
    private val dialogportenClient: DialogportenClient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun opprettOgLagreDialog(sykmelding: Sykmelding) {
        val transmission = sykmeldingTransmission(sykmelding.sykmeldingId).toTransmission(),
        val dialogId =
            runBlocking {
                val request =
                    CreateDialogRequest(
                        orgnr = sykmelding.orgnr,
                        externalReference = sykmelding.sykmeldingId.toString(),
                        idempotentKey = sykmelding.sykmeldingId.toString(),
                        title =
                            "Sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()})",
                        summary =
                            sykmelding.sykmeldingsperioder.getSykmeldingsPerioderString(),
                        additionalInfo = lagDialogAdditionalInfo(),
                        transmissions = listOf(transmission),
                        isApiOnly = unleashFeatureToggles.skalOppretteDialogKunForApi(),
                    )

                dialogportenClient.createDialog(request)
            }
        dialogRepository.lagreDialog(dialogId = dialogId, sykmeldingId = sykmelding.sykmeldingId)
        transmission.id?.let { transmissionId ->
            dialogRepository.oppdaterDialogMedTransmission(
                sykmeldingId = sykmelding.sykmeldingId,
                transmissionId = transmissionId,
                dokumentId = sykmelding.sykmeldingId,
                dokumentType = LpsApiExtendedType.SYKMELDING.toString(),
            )
        }

        logger.info("Opprettet dialog $dialogId for sykmelding ${sykmelding.sykmeldingId}.")
    }
}

fun sykmeldingTransmission(
    sykmeldingId: UUID,
    isSilentUpdate: Boolean = false, // TODO kan fjernes etter engangsjobb patcher transmission
): TransmissionRequest =
    SykmeldingTransmissionRequest(
        sykmeldingId,
        listOf(
            createApiAttachment(
                displayName = "sykmelding.json",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/$sykmeldingId",
            ),
            createApiAttachment(
                displayName = "sykmelding.pdf",
                url = "${Env.Nav.arbeidsgiverApiBaseUrl}/v1/sykmelding/$sykmeldingId/pdf",
                mediaType = "application/pdf",
            ),
            createGuiAttachment(
                displayName = "sykmelding",
                url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf",
                mediaType = "application/pdf",
            ),
        ),
        isSilentUpdate,
    )
