package no.nav.helsearbeidsgiver.dialogporten.handlers

import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpDokType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknad
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknad
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr

class FritakAgpSoeknadHandler(
    private val dialogportenClient: DialogportenClient,
    private val fritakDialogRepository: FritakDialogRepository,
) {
    suspend fun behandleSoeknadDialog(soeknadMelding: FritakSoeknadMelding) {
        when (soeknadMelding) {
            is GravidSoeknad -> opprettDialogForGravidSoeknad(soeknadMelding)
            is KroniskSoeknad -> opprettDialogForKroniskSoeknad(soeknadMelding)
        }
    }

    private suspend fun opprettDialogForKroniskSoeknad(soeknadMelding: KroniskSoeknad) {
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
                                url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/soeknad/${soeknadMelding.id}/pdf",
                                mediaType = "application/pdf",
                            ),
                            createGuiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/soeknad/${soeknadMelding.id}/pdf",
                                mediaType = "application/pdf",
                            ),
                        ),
                ),
            )

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
            soeknadType = FritakAgpDokType.KRONISK_SOEKNAD,
            fnr = soeknadMelding.fnr,
            orgnr = soeknadMelding.orgnr.verdi,
        )
    }

    private suspend fun opprettDialogForGravidSoeknad(gravidSoeknad: GravidSoeknad) {
        val dialogId =
            dialogportenClient.createDialog(
                CreateDialogRequest(
                    orgnr = gravidSoeknad.orgnr,
                    externalReference = gravidSoeknad.id.toString(),
                    idempotentKey = gravidSoeknad.id.toString(),
                    title =
                        "Søknad om fritak fra arbeidsgiverperioden grunnet graviditet." +
                            " ${gravidSoeknad.navn} (f. ${foedselsdatoFraFnr(gravidSoeknad.fnr)})",
                    summary =
                        "Kvittering for mottatt søknad om fritak fra" +
                            " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                    transmissions = emptyList(),
                    isApiOnly = false,
                    attachments =
                        listOf(
                            createApiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/soeknad/${gravidSoeknad.id}/pdf",
                                mediaType = "application/pdf",
                            ),
                            createGuiAttachment(
                                displayName = "Søknad om fritak fra arbeidsgiverperioden",
                                url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/soeknad/${gravidSoeknad.id}/pdf",
                                mediaType = "application/pdf",
                            ),
                        ),
                ),
            )

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
            soeknadId = gravidSoeknad.id,
            soeknadType = FritakAgpDokType.GRAVID_SOEKNAD,
            fnr = gravidSoeknad.fnr,
            orgnr = gravidSoeknad.orgnr.verdi,
        )
    }
}
