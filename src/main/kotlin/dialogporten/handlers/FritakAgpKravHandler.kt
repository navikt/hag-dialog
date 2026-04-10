package dialogporten.handlers

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.database.finnTypeForFritakKrav
import no.nav.helsearbeidsgiver.dialogporten.DialogportenClient
import no.nav.helsearbeidsgiver.dialogporten.FritakGravidKravTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.FritakKroniskKravTransmissionRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.Action
import no.nav.helsearbeidsgiver.dialogporten.domene.ContentValueItem
import no.nav.helsearbeidsgiver.dialogporten.domene.CreateDialogRequest
import no.nav.helsearbeidsgiver.dialogporten.domene.GuiAction
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravStatus
import no.nav.helsearbeidsgiver.kafka.GravidKravMelding
import no.nav.helsearbeidsgiver.kafka.KroniskKravMelding
import no.nav.helsearbeidsgiver.kafka.foedselsdatoFraFnr

class FritakAgpKravHandler(
    val dialogportenClient: DialogportenClient,
    val fritakDialogRepository: FritakDialogRepository,
) {
    fun behandleKravDialog(kravmelding: FritakKravMelding) {
        when (kravmelding) {
            is KroniskKravMelding -> behandleKroniskKravDialog(kravmelding)
            is GravidKravMelding -> behandleGravidKravDialog(kravmelding)
        }
    }

    private fun behandleKroniskKravDialog(kravmelding: KroniskKravMelding) {
        when (kravmelding.status) {
            FritakKravStatus.OPPRETTET -> opprettDialogForKroniskKrav(kravmelding)
            FritakKravStatus.ENDRET -> oppdaterDialogForKroniskKrav(kravmelding)
            FritakKravStatus.SLETTET -> oppdaterDialogForKroniskKrav(kravmelding)
        }
    }

    private fun behandleGravidKravDialog(kravmelding: GravidKravMelding) {
        when (kravmelding.status) {
            FritakKravStatus.OPPRETTET -> opprettDialogForGravidKrav(kravmelding)
            FritakKravStatus.ENDRET -> oppdaterDialogForGravidKrav(kravmelding)
            FritakKravStatus.SLETTET -> oppdaterDialogForGravidKrav(kravmelding)
        }
    }

    private fun opprettDialogForKroniskKrav(kravmelding: KroniskKravMelding) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = kravmelding.orgnr,
                        externalReference = "fritak-agp",
                        idempotentKey = kravmelding.id.toString(),
                        title =
                            "Krav om fritak fra arbeidsgiverperioden grunnet kronisk sykdom." +
                                " ${kravmelding.navn} (f. ${foedselsdatoFraFnr(kravmelding.fnr)})",
                        summary =
                            "Kvittering for mottatt krav om fritak fra" +
                                " arbeidsgiverperioden grunnet kronisk sykdom.",
                        transmissions = emptyList(),
                        isApiOnly = false,
                    ),
                )
            dialogportenClient.addGuiAction(
                dialogId = dialogId,
                guiAction =
                    GuiAction(
                        name = "Endre krav",
                        url = "${Env.Nav.arbeidsgiverGuiBaseUrl}/fritak-agp/nb/gravid/krav/${kravmelding.id}",
                        action = Action.READ.value,
                        title = listOf(ContentValueItem("Send inn krav")),
                        priority = GuiAction.Priority.Primary,
                    ),
            )
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId,
                    FritakKroniskKravTransmissionRequest(
                        kravMelding = kravmelding,
                    ).toTransmission(),
                )

            fritakDialogRepository.lagreKravDialog(
                dialogId = dialogId,
                transmissionId = transmissionId,
                kravId = kravmelding.id,
                kravType = finnTypeForFritakKrav(kravmelding),
                fnr = kravmelding.fnr,
                orgnr = kravmelding.orgnr.verdi,
            )
        }
    }

    private fun oppdaterDialogForKroniskKrav(kravmelding: KroniskKravMelding) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravIdOgKravType(
                kravId = kravmelding.id,
                kravType = FritakAgpType.KRONISK_KRAV_OPPRETTET,
            )

        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakKroniskKravTransmissionRequest(
                            kravMelding = kravmelding,
                        ).toTransmission(),
                    )

                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = kravmelding.id,
                    kravType = finnTypeForFritakKrav(kravmelding),
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }
    }

    private fun oppdaterDialogForGravidKrav(kravmelding: GravidKravMelding) {
        val opprinneligeKrav =
            fritakDialogRepository.finnDialogMedKravIdOgKravType(
                kravmelding.id,
                FritakAgpType.GRAVID_KRAV_OPPRETTET,
            )
        runBlocking {
            if (opprinneligeKrav != null) {
                val transmissionId =
                    dialogportenClient.addTransmission(
                        dialogId = opprinneligeKrav.dialogId,
                        FritakGravidKravTransmissionRequest(
                            kravMelding = kravmelding,
                        ).toTransmission(),
                    )

                fritakDialogRepository.lagreKravDialog(
                    dialogId = opprinneligeKrav.dialogId,
                    transmissionId = transmissionId,
                    kravId = kravmelding.id,
                    kravType = finnTypeForFritakKrav(kravmelding),
                    fnr = kravmelding.fnr,
                    orgnr = kravmelding.orgnr.verdi,
                )
            }
        }
    }

    private fun opprettDialogForGravidKrav(gravidKravMelding: GravidKravMelding) {
        runBlocking {
            val dialogId =
                dialogportenClient.createDialog(
                    CreateDialogRequest(
                        orgnr = gravidKravMelding.orgnr,
                        externalReference = "fritak-agp",
                        idempotentKey = gravidKravMelding.id.toString(),
                        title =
                            "Krav om fritak fra arbeidsgiverperioden grunnet graviditet." +
                                " ${gravidKravMelding.navn} (f. ${foedselsdatoFraFnr(gravidKravMelding.fnr)})",
                        summary =
                            "Kvittering for mottatt krav om fritak fra" +
                                " arbeidsgiverperioden grunnet risiko for høyt sykefravær knyttet til graviditet.",
                        transmissions = emptyList(),
                        isApiOnly = false,
                    ),
                )
            val transmissionId =
                dialogportenClient.addTransmission(
                    dialogId,
                    FritakGravidKravTransmissionRequest(
                        kravMelding = gravidKravMelding,
                    ).toTransmission(),
                )
            fritakDialogRepository.lagreKravDialog(
                dialogId = dialogId,
                transmissionId = transmissionId,
                kravId = gravidKravMelding.id,
                kravType = finnTypeForFritakKrav(gravidKravMelding),
                fnr = gravidKravMelding.fnr,
                orgnr = gravidKravMelding.orgnr.verdi,
            )
        }
    }
}
