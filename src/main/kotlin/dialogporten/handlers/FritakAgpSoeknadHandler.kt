package no.nav.helsearbeidsgiver.dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpDokType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr

class FritakAgpSoeknadHandler(
    private val dialogportenClient: DialogportenClient,
    private val fritakDialogRepository: FritakDialogRepository,
) {
    fun behandleSoeknadDialog(soeknadMelding: FritakSoeknadMelding) {
        when (soeknadMelding) {
            is GravidSoeknadMelding -> opprettDialogForGravidSoeknad(soeknadMelding)
            is KroniskSoeknadMelding -> opprettDialogForKroniskSoeknad(soeknadMelding)
        }
    }

    private fun opprettDialogForKroniskSoeknad(soeknadMelding: KroniskSoeknadMelding) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = soeknadMelding.orgnr,
                        externalReference = "fritak-agp",
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

            fritakDialogRepository.lagreSoeknadDialog(
                dialogId = dialogId,
                soeknadId = soeknadMelding.id,
                soeknadType = FritakAgpDokType.KRONISK_SOEKNAD,
                fnr = soeknadMelding.fnr,
                orgnr = soeknadMelding.orgnr.verdi,
            )
        }
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
                                    displayName = "Søknad on fritak fra arbeidsgiverperioden",
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
            fritakDialogRepository.lagreSoeknadDialog(
                dialogId = dialogId,
                soeknadId = gravidSoeknadMelding.id,
                soeknadType = FritakAgpDokType.GRAVID_SOEKNAD,
                fnr = gravidSoeknadMelding.fnr,
                orgnr = gravidSoeknadMelding.orgnr.verdi,
            )
        }
    }
}
