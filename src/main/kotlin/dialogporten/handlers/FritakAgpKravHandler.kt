package dialogporten.handlers

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
import no.nav.helsearbeidsgiver.dialogporten.toPdfUrl
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravOpprettet
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.nyUuidv7

private const val ENDRE_KRAV_ACTION_KNAPP = "Endre / Annuller krav"

class FritakAgpKravHandler(
    val dialogportenClient: DialogportenClient,
    val fritakDialogRepository: FritakDialogRepository,
) {
    suspend fun behandleKravDialog(kravmelding: FritakKravMelding) {
        when (kravmelding) {
            is KroniskKravOpprettet -> opprettDialogForKroniskKrav(kravmelding)
            is KroniskKravEndret -> oppdaterDialogForKroniskKrav(kravmelding)
            is KroniskKravSlettet -> oppdaterDialogForKroniskKravSlettet(kravmelding)
            is GravidKravOpprettet -> opprettDialogForGravidKrav(kravmelding)
            is GravidKravEndret -> oppdaterDialogForGravidKrav(kravmelding)
            is GravidKravSlettet -> oppdaterDialogForGravidKravSlettet(kravmelding)
        }
    }

    private suspend fun opprettDialogForKroniskKrav(kroniskKrav: FritakKravMelding) {
        val dialogId = nyUuidv7()
        dialogportenClient.createDialog(
            CreateDialogRequest(
                id = dialogId,
                orgnr = kroniskKrav.orgnr,
                externalReference = "fritak-agp",
                idempotentKey = kroniskKrav.id.toString(),
                title =
                    "Krav om fritak fra arbeidsgiverperioden grunnet kronisk sykdom." +
                        " ${kroniskKrav.navn} (f. ${foedselsdatoFraFnr(kroniskKrav.fnr)})",
                summary =
                    "Kvittering for mottatt krav om fritak fra" +
                        " arbeidsgiverperioden grunnet kronisk sykdom.",
                transmissions = emptyList(),
                isApiOnly = false,
                attachments =
                    listOf(
                        createApiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = kroniskKrav.toPdfUrl(),
                            mediaType = "application/pdf",
                        ),
                        createGuiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = kroniskKrav.toPdfUrl(),
                            mediaType = "application/pdf",
                        ),
                    ),
            ),
        )

        val transmissionId =
            dialogportenClient.addTransmission(
                dialogId,
                FritakKravTransmissionRequest(
                    kravMelding = kroniskKrav,
                ).toTransmission(),
            )

        if (kroniskKrav !is KroniskKravSlettet) {
            dialogportenClient.addGuiAction(
                dialogId = dialogId,
                guiAction =
                    GuiAction(
                        name = ENDRE_KRAV_ACTION_KNAPP,
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/kronisk/krav/${kroniskKrav.id}",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem(ENDRE_KRAV_ACTION_KNAPP)),
                        priority = GuiAction.Priority.Primary,
                    ),
            )
        }

        fritakDialogRepository.lagreKravDialog(
            dialogId = dialogId,
            transmissionId = transmissionId,
            kravId = kroniskKrav.id,
            kravType = finnTypeForFritakKrav(kroniskKrav),
            fnr = kroniskKrav.fnr,
            orgnr = kroniskKrav.orgnr.verdi,
        )
    }

    private suspend fun oppdaterDialogForKroniskKrav(kroniskKravEndret: KroniskKravEndret) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                kravId = kroniskKravEndret.forrigeKrav,
            )

        if (opprinneligeKrav != null) {
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId = opprinneligeKrav.dialogId,
                    FritakKravTransmissionRequest(
                        kravMelding = kroniskKravEndret,
                    ).toTransmission(),
                )
            dialogportenClient.replaceAttachmentsAndActions(
                opprinneligeKrav.dialogId,
                listOf(
                    createApiAttachment(
                        displayName = "Krav på fritak fra arbeidsgiverperioden",
                        url = kroniskKravEndret.toPdfUrl(),
                        mediaType = "application/pdf",
                    ),
                    createGuiAttachment(
                        displayName = "Krav på fritak fra arbeidsgiverperioden",
                        url = kroniskKravEndret.toPdfUrl(),
                        mediaType = "application/pdf",
                    ),
                ),
                apiActions = emptyList(),
                guiActions =
                    listOf(
                        GuiAction(
                            name = ENDRE_KRAV_ACTION_KNAPP,
                            url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/kronisk/krav/${kroniskKravEndret.id}",
                            action = Action.READ.value,
                            title = listOf(ContentValueItem(ENDRE_KRAV_ACTION_KNAPP)),
                            priority = GuiAction.Priority.Primary,
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
        } else {
            logger().info(
                "Klarte ikke å finne opprinnelig krav for kronisk krav endret med id ${kroniskKravEndret.id}. Oppretter ny dialog.",
            )
            opprettDialogForKroniskKrav(kroniskKravEndret)
        }
    }

    private suspend fun opprettDialogForGravidKrav(gravidKrav: FritakKravMelding) {
        val dialogId = nyUuidv7()
        dialogportenClient.createDialog(
            CreateDialogRequest(
                id = dialogId,
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
                            url = gravidKrav.toPdfUrl(),
                            mediaType = "application/pdf",
                        ),
                        createGuiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = gravidKrav.toPdfUrl(),
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

        if (gravidKrav !is GravidKravSlettet) {
            dialogportenClient.addGuiAction(
                dialogId = dialogId,
                guiAction =
                    GuiAction(
                        name = ENDRE_KRAV_ACTION_KNAPP,
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/gravid/krav/${gravidKrav.id}",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem(ENDRE_KRAV_ACTION_KNAPP)),
                        priority = GuiAction.Priority.Primary,
                    ),
            )
        }

        fritakDialogRepository.lagreKravDialog(
            dialogId = dialogId,
            transmissionId = transmissionId,
            kravId = gravidKrav.id,
            kravType = finnTypeForFritakKrav(gravidKrav),
            fnr = gravidKrav.fnr,
            orgnr = gravidKrav.orgnr.verdi,
        )
    }

    private suspend fun oppdaterDialogForGravidKrav(gravidKravEndret: GravidKravEndret) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                gravidKravEndret.forrigeKrav,
            )

        if (opprinneligeKrav != null) {
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId = opprinneligeKrav.dialogId,
                    FritakKravTransmissionRequest(
                        kravMelding = gravidKravEndret,
                    ).toTransmission(),
                )
            dialogportenClient.replaceAttachmentsAndActions(
                dialogId = opprinneligeKrav.dialogId,
                attachments =
                    listOf(
                        createApiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = gravidKravEndret.toPdfUrl(),
                            mediaType = "application/pdf",
                        ),
                        createGuiAttachment(
                            displayName = "Krav på fritak fra arbeidsgiverperioden",
                            url = gravidKravEndret.toPdfUrl(),
                            mediaType = "application/pdf",
                        ),
                    ),
                apiActions = emptyList(),
                guiActions =
                    listOf(
                        GuiAction(
                            name = ENDRE_KRAV_ACTION_KNAPP,
                            url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/gravid/krav/${gravidKravEndret.id}",
                            action = Action.READ.value,
                            title = listOf(ContentValueItem(ENDRE_KRAV_ACTION_KNAPP)),
                            priority = GuiAction.Priority.Primary,
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
        } else {
            logger().info(
                "Klarte ikke å finne opprinnelig krav for gravid krav endret med id ${gravidKravEndret.id}. Oppretter ny dialog.",
            )
            opprettDialogForGravidKrav(gravidKravEndret)
        }
    }

    private suspend fun oppdaterDialogForKroniskKravSlettet(kravmelding: KroniskKravSlettet) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                kravmelding.id,
            )

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
        } else {
            logger().info(
                "Klarte ikke å finne opprinnelig krav for kronisk krav slettet med id ${kravmelding.id}. Oppretter ny dialog.",
            )
            opprettDialogForKroniskKrav(kravmelding)
        }
    }

    private suspend fun oppdaterDialogForGravidKravSlettet(gravidKravSlettet: GravidKravSlettet) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravId(
                gravidKravSlettet.id,
            )

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
        } else {
            logger().info(
                "Klarte ikke å finne opprinnelig krav for gravid krav slettet med id ${gravidKravSlettet.id}. oppretter ny dialog.",
            )
            opprettDialogForGravidKrav(gravidKravSlettet)
        }
    }
}
