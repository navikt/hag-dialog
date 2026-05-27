package no.nav.helsearbeidsgiver.dialogporten

import dialogporten.handlers.FritakAgpKravHandler
import no.nav.helsearbeidsgiver.database.FritakAgpSoeknadEntity
import no.nav.helsearbeidsgiver.database.FritakAgpSoeknadType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Attachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.handlers.FritakAgpSoeknadHandler
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidSoeknadOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskSoeknadOpprettet
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class FritakDialogportenService(
    val fritakDialogRepository: FritakDialogRepository,
    val dialogportenClient: DialogportenClient,
) {
    private val fritakAgpSoeknadHandler = FritakAgpSoeknadHandler(dialogportenClient, fritakDialogRepository)
    private val fritakAgpKravHandler = FritakAgpKravHandler(dialogportenClient, fritakDialogRepository)

    suspend fun opprettDialogForFritakAgp(dialogMelding: DialogMelding) {
        when (dialogMelding) {
            is FritakKravMelding -> fritakAgpKravHandler.behandleKravDialog(dialogMelding)
            is FritakSoeknadMelding -> fritakAgpSoeknadHandler.behandleSoeknadDialog(dialogMelding)
        }
    }

    suspend fun replaceAttachmentsForSoekander() {
        val alleSoeknader =
            fritakDialogRepository
                .hentAlleSoeknader()
        logger().info("Starter å erstatte vedlegg for alle søknader totalt ${alleSoeknader.size}")
        var antallErstatteVedlegg = 0
        alleSoeknader
            // TODO Fjern denne når vi har testet at dette fungerer
            .filter { it.dialogId == UUID.fromString("019e68c9-ae9d-725b-897f-73912226ba39") }
            .forEach {
                if (it.soeknadType == FritakAgpSoeknadType.GRAVID_SOEKNAD) {
                    replaceAttachmentForDialog(
                        it.dialogId,
                        soeknadMelding =
                            GravidSoeknadOpprettet(
                                id = it.soeknadId,
                                orgnr = Orgnr(it.orgnr),
                                navn = "N/A",
                                fnr = "N/A",
                            ),
                    )
                    logger().info("Erstattet vedlegg for gravid søknad med dialogId ${it.dialogId}")
                    antallErstatteVedlegg++
                } else {
                    if (it.soeknadType == FritakAgpSoeknadType.KRONISK_SOEKNAD) {
                        replaceAttachmentForDialog(
                            it.dialogId,
                            soeknadMelding =
                                KroniskSoeknadOpprettet(
                                    id = it.soeknadId,
                                    orgnr = Orgnr(it.orgnr),
                                    navn = "N/A",
                                    fnr = "N/A",
                                ),
                        )
                        logger().info("Erstattet vedlegg for kronisk søknad med dialogId ${it.dialogId}")
                    }
                    antallErstatteVedlegg++
                }
            }
        logger().info("Ferdig med å erstatte vedlegg for alle søknader totalt $antallErstatteVedlegg vedlegg erstattet")
    }

    private suspend fun replaceAttachmentForDialog(
        dialogId: UUID,
        soeknadMelding: FritakSoeknadMelding,
    ) {
        dialogportenClient.replaceAttachments(
            dialogId,
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
        )
    }
}
