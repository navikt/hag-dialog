package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpDokType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr

class FritakAgpHandler(
    private val dialogportenClient: DialogportenClient,
    private val fritakDialogRepository: FritakDialogRepository,
) {
    fun opprettOgLagreDialog(dialogMelding: DialogMelding) {
        when (dialogMelding) {
            is GravidSoeknadMelding -> opprettDialogForGravidSoeknad(dialogMelding)
            is GravidKravMelding -> opprettDialogForGravidKrav(dialogMelding)
        }
    }

    private fun opprettDialogForGravidKrav(dialogMelding: GravidKravMelding) {
        TODO("Not yet implemented")
    }

    private fun opprettDialogForGravidSoeknad(gravidSoeknadMelding: GravidSoeknadMelding) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = gravidSoeknadMelding.orgnr,
                        externalReference = "fritak-agp",
                        idempotentKey = gravidSoeknadMelding.id.toString(),
                        title =
                            "Søknad om fritak fra arbeidsgiverperioden grunnet graviditet." +
                                " ${gravidSoeknadMelding.navn} (f. ${foedselsdatoFraFnr(gravidSoeknadMelding.fnr)})",
                        summary =
                            "Kvittering for mottatt søknad om fritak fra" +
                                " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                        transmissions = emptyList(),
                        isApiOnly = false,
                        attachments =
                            listOf(
                                createApiAttachment(
                                    displayName = "sykmelding.pdf",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/soeknad/${gravidSoeknadMelding.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                                createGuiAttachment(
                                    displayName = "Søknad on fritak fra arbeidsgiverperioden",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/soeknad/${gravidSoeknadMelding.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                            ),
                    ),
                )
            fritakDialogRepository.lagreDialog(
                dialogId = dialogId,
                dokumentId = gravidSoeknadMelding.id,
                dokumentType = FritakAgpDokType.GRAVID_SOEKNAD,
                fnr = gravidSoeknadMelding.fnr,
            )
        }
    }
}
