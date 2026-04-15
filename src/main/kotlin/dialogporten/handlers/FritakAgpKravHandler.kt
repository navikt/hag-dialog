package dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.database.finnTypeForFritakKrav
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.FritakKravTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.GravidKrav
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskKrav
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr

class FritakAgpKravHandler(
    val dialogportenClient: DialogportenClient,
    val fritakDialogRepository: FritakDialogRepository,
) {
    fun behandleKravDialog(kravmelding: FritakKravMelding) {
        when (kravmelding) {
            is KroniskKrav -> opprettDialogForKroniskKrav(kravmelding)
            is KroniskKravEndret -> oppdaterDialogForKroniskKrav(kravmelding)
            is KroniskKravSlettet -> oppdaterDialogForKroniskravSlettet(kravmelding)
            is GravidKrav -> opprettDialogForGravidKrav(kravmelding)
            is GravidKravEndret -> oppdaterDialogForGravidKrav(kravmelding)
            is GravidKravSlettet -> oppdaterDialogForGravidKravSlettet(kravmelding)
        }
    }

    private fun opprettDialogForKroniskKrav(kroniskKravMelding: KroniskKrav) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = kroniskKravMelding.orgnr,
                        externalReference = "fritak-agp",
                        idempotentKey = kroniskKravMelding.id.toString(),
                        title =
                            "Krav om fritak fra arbeidsgiverperioden grunnet kronisk sykdom." +
                                " ${kroniskKravMelding.navn} (f. ${foedselsdatoFraFnr(kroniskKravMelding.fnr)})",
                        summary =
                            "Kvittering for mottatt krav om fritak fra" +
                                " arbeidsgiverperioden grunnet kronisk sykdom.",
                        transmissions = emptyList(),
                        isApiOnly = false,
                        attachments =
                            listOf(
                                createApiAttachment(
                                    displayName = "Krav på fritak fra arbeidsgiverperioden",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/krav/${kroniskKravMelding.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                                createGuiAttachment(
                                    displayName = "Krav på fritak fra arbeidsgiverperioden",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/krav/${kroniskKravMelding.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                            ),
                    ),
                )

            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId,
                    FritakKravTransmissionRequest(
                        kravMelding = kroniskKravMelding,
                    ).toTransmission(),
                )

            dialogportenClient.addGuiAction(
                dialogId = dialogId,
                guiAction =
                    GuiAction(
                        name = "Endre krav",
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/kronisk/krav/${kroniskKravMelding.id}",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem("Endre krav")),
                        priority = GuiAction.Priority.Primary,
                    ),
            )

            fritakDialogRepository.lagreKravDialog(
                dialogId = dialogId,
                transmissionId = transmissionId,
                kravId = kroniskKravMelding.id,
                kravType = finnTypeForFritakKrav(kroniskKravMelding),
                fnr = kroniskKravMelding.fnr,
                orgnr = kroniskKravMelding.orgnr.verdi,
            )
        }
    }

    private fun oppdaterDialogForKroniskKrav(kroniskKravEndret: KroniskKravEndret) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                kravId = kroniskKravEndret.forrigeKrav,
            )

        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakKravTransmissionRequest(
                            kravMelding = kroniskKravEndret,
                        ).toTransmission(),
                    )
                dialogportenClient.replaceAttachment(
                    opprinneligeKrav.dialogId,
                    listOf(
                        createApiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/krav/${kroniskKravEndret.id}/pdf",
                            mediaType = "application/pdf",
                        ),
                        createGuiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/kronisk/krav/${kroniskKravEndret.id}/pdf",
                            mediaType = "application/pdf",
                        ),
                    ),
                )

                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = kroniskKravEndret.id,
                    kravType = finnTypeForFritakKrav(kroniskKravEndret),
                    fnr = kroniskKravEndret.fnr,
                    orgnr = kroniskKravEndret.orgnr.verdi,
                )
            }
        }
    }

    private fun opprettDialogForGravidKrav(gravidKrav: GravidKrav) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = gravidKrav.orgnr,
                        externalReference = "fritak-agp",
                        idempotentKey = gravidKrav.id.toString(),
                        title =
                            "Krav om fritak fra arbeidsgiverperioden grunnet graviditet." +
                                " ${gravidKrav.navn} (f. ${foedselsdatoFraFnr(gravidKrav.fnr)})",
                        summary =
                            "Kvittering for mottatt krav om fritak fra" +
                                " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                        transmissions = emptyList(),
                        isApiOnly = false,
                        attachments =
                            listOf(
                                createApiAttachment(
                                    displayName = "Krav på fritak fra arbeidsgiverperioden",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/krav/${gravidKrav.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                                createGuiAttachment(
                                    displayName = "Krav på fritak fra arbeidsgiverperioden",
                                    url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/krav/${gravidKrav.id}/pdf",
                                    mediaType = "application/pdf",
                                ),
                            ),
                    ),
                )
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId,
                    FritakKravTransmissionRequest(
                        kravMelding = gravidKrav,
                    ).toTransmission(),
                )
            dialogportenClient.addGuiAction(
                dialogId = dialogId,
                guiAction =
                    GuiAction(
                        name = "Endre krav",
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/gravid/krav/${gravidKrav.id}",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem("Endre krav")),
                        priority = GuiAction.Priority.Primary,
                    ),
            )
            fritakDialogRepository.lagreKravDialog(
                dialogId = dialogId,
                transmissionId = transmissionId,
                kravId = gravidKrav.id,
                kravType = finnTypeForFritakKrav(gravidKrav),
                fnr = gravidKrav.fnr,
                orgnr = gravidKrav.orgnr.verdi,
            )
        }
    }

    private fun oppdaterDialogForGravidKrav(gravidKravEndret: GravidKravEndret) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                gravidKravEndret.forrigeKrav,
            )
        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakKravTransmissionRequest(
                            kravMelding = gravidKravEndret,
                        ).toTransmission(),
                    )
                dialogportenClient.replaceAttachment(
                    opprinneligeKrav.dialogId,
                    listOf(
                        createApiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/krav/${gravidKravEndret.id}/pdf",
                            mediaType = "application/pdf",
                        ),
                        createGuiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = "${Env.Nav.dokumentProxyBaseUrl}/v1/fritakagp/gravid/krav/${gravidKravEndret.id}/pdf",
                            mediaType = "application/pdf",
                        ),
                    ),
                )
                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = gravidKravEndret.id,
                    kravType = finnTypeForFritakKrav(gravidKravEndret),
                    fnr = gravidKravEndret.fnr,
                    orgnr = gravidKravEndret.orgnr.verdi,
                )
            }
        }
    }

    private fun oppdaterDialogForKroniskravSlettet(kravmelding: KroniskKravSlettet) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                kravmelding.id,
            )
        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakKravTransmissionRequest(
                            kravMelding = kravmelding,
                        ).toTransmission(),
                    )
                dialogportenClient.removeActionsAndStatus(
                    dialogId = opprinneligeKrav.dialogId,
                )
                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = kravmelding.id,
                    kravType = FritakAgpType.KRONISK_KRAV_SLETTET,
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }
    }

    private fun oppdaterDialogForGravidKravSlettet(gravidKravSlettet: GravidKravSlettet) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                gravidKravSlettet.id,
            )
        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakKravTransmissionRequest(
                            kravMelding = gravidKravSlettet,
                        ).toTransmission(),
                    )
                dialogportenClient.removeActionsAndStatus(
                    dialogId = opprinneligeKrav.dialogId,
                )
                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = gravidKravSlettet.id,
                    kravType = FritakAgpType.GRAVID_KRAV_SLETTET,
                    fnr = gravidKravSlettet.fnr,
                    orgnr = gravidKravSlettet.orgnr.verdi,
                )
            }
        }
    }
}
