package no.nav.helsearbeidsgiver.dialogporten.handlers

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpSoeknadType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.toPdfUrl
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadOpprettet
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr
import no.nav.helsearbeidsgiver.utils.log.logger

class FritakAgpSoeknadHandler(
    private val dialogportenClient: DialogportenClient,
    private val fritakDialogRepository: FritakDialogRepository,
) {
    suspend fun behandleSoeknadDialog(soeknadMelding: FritakSoeknadMelding) {
        when (soeknadMelding) {
            is GravidSoeknadOpprettet -> opprettDialogForGravidSoeknad(soeknadMelding)
            is KroniskSoeknadOpprettet -> opprettDialogForKroniskSoeknad(soeknadMelding)
        }
    }

    private suspend fun opprettDialogForKroniskSoeknad(soeknadMelding: KroniskSoeknadOpprettet) {
        val dialogId =
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = soeknadMelding.orgnr,
                    externalReference = soeknadMelding.id.toString(),
                    idempotentKey = soeknadMelding.id.toString(),
                    title =
                        "Søknad om fritak fra arbeidsgiverperioden grunnet kronisk sykdom." +
                            " ${soeknadMelding.navn} (f. ${foedselsdatoFraFnr(soeknadMelding.fnr)})",
                    summary =
                        "Kvittering for mottatt søknad om fritak fra" +
                            " arbeidsgiverperioden grunnet kronisk sykdom.",
                    transmissions = emptyList(),
                    isApiOnly = false,
                    attachments =
                        listOf(
                            createApiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = soeknadMelding.toPdfUrl(),
                                mediaType = "application/pdf",
                            ),
                            createGuiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = soeknadMelding.toPdfUrl(),
                                mediaType = "application/pdf",
                            ),
                        ),
                ),
            )
        if (fritakDialogRepository.hentSoeknadMedIdogDialogId(soeknadMelding.id, dialogId) != null) {
            logger().info("Dialog for kronisk søknad med id ${soeknadMelding.id} og dialogId $dialogId finnes allerede.")
            return
        }

        dialogportenClient.addGuiAction(
            dialogId = dialogId,
            guiAction =
                GuiAction(
                    name = "Send inn krav",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/kronisk/krav",
                    action = Action.READ.value,
                    title = listOf(ContentValueItem("Send inn krav")),
                    priority = GuiAction.Priority.Primary,
                ),
        )

        fritakDialogRepository.lagreSoeknadDialog(
            dialogId = dialogId,
            soeknadId = soeknadMelding.id,
            soeknadType = FritakAgpSoeknadType.KRONISK_SOEKNAD,
            fnr = soeknadMelding.fnr,
            orgnr = soeknadMelding.orgnr.verdi,
        )
    }

    private suspend fun opprettDialogForGravidSoeknad(gravidSoeknadOpprettet: GravidSoeknadOpprettet) {
        val dialogId =
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = gravidSoeknadOpprettet.orgnr,
                    externalReference = gravidSoeknadOpprettet.id.toString(),
                    idempotentKey = gravidSoeknadOpprettet.id.toString(),
                    title =
                        "Søknad om fritak fra arbeidsgiverperioden grunnet graviditet." +
                            " ${gravidSoeknadOpprettet.navn} (f. ${foedselsdatoFraFnr(gravidSoeknadOpprettet.fnr)})",
                    summary =
                        "Kvittering for mottatt søknad om fritak fra" +
                            " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                    transmissions = emptyList(),
                    isApiOnly = false,
                    attachments =
                        listOf(
                            createApiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = gravidSoeknadOpprettet.toPdfUrl(),
                                mediaType = "application/pdf",
                            ),
                            createGuiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = gravidSoeknadOpprettet.toPdfUrl(),
                                mediaType = "application/pdf",
                            ),
                        ),
                ),
            )
        if (fritakDialogRepository.hentSoeknadMedIdogDialogId(gravidSoeknadOpprettet.id, dialogId) != null) {
            logger().info("Dialog for gravid søknad med id ${gravidSoeknadOpprettet.id} og dialogId $dialogId finnes allerede. ")
            return
        }
        dialogportenClient.addGuiAction(
            dialogId = dialogId,
            guiAction =
                GuiAction(
                    name = "Send inn krav",
                    url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/gravid/krav",
                    action = Action.READ.value,
                    title = listOf(ContentValueItem("Send inn krav")),
                    priority = GuiAction.Priority.Primary,
                ),
        )

        fritakDialogRepository.lagreSoeknadDialog(
            dialogId = dialogId,
            soeknadId = gravidSoeknadOpprettet.id,
            soeknadType = FritakAgpSoeknadType.GRAVID_SOEKNAD,
            fnr = gravidSoeknadOpprettet.fnr,
            orgnr = gravidSoeknadOpprettet.orgnr.verdi,
        )
    }
}
