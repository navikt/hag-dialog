package no.nav.helsearbeidsgiver.dialogporten

import dialogporten.handlers.FritakAgpKravHandler
import no.nav.helsearbeidsgiver.database.FritakAgpKravEntity
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.createApiAttachment
import no.nav.helsearbeidsgiver.dialogporten.domene.createGuiAttachment
import no.nav.helsearbeidsgiver.dialogporten.handlers.FritakAgpSoeknadHandler
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding
import no.nav.helsearbeidsgiver.kafka.GravidKravEndret
import no.nav.helsearbeidsgiver.kafka.GravidKravOpprettet
import no.nav.helsearbeidsgiver.kafka.GravidKravSlettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravEndret
import no.nav.helsearbeidsgiver.kafka.KroniskKravOpprettet
import no.nav.helsearbeidsgiver.kafka.KroniskKravSlettet
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDateTime
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

    suspend fun replaceAttachmentsForKrav() {
        // Henter alle krav fra dialogporten som er sendt inn før cutt-off tidspunkt
        val alleKrav =
            fritakDialogRepository
                .hentAlleKravTilTidspunkt(LocalDateTime.of(2026, 4, 9, 13, 0))
        logger().info(dialogFiksLogg("Starter å erstatte vedlegg for alle krav totalt ${alleKrav.size} krav"))
        alleKrav.forEach { krav ->
            logger().info(dialogFiksLogg("Erstatter vedlegg for krav med id ${krav.kravId} og dialogId ${krav.dialogId}"))
            // Henter siste krav for dialogId i db
            val sisteKrav = hentSisteKravIdFraTransmissions(krav.dialogId)
            if (sisteKrav == null) {
                logger().warn(
                    dialogFiksLogg("Fant ingen krav for dialogId ${krav.dialogId}, kan ikke erstatte vedlegg for krav ${krav.kravId}"),
                )
                return@forEach
            }
            // Henter dialog fra dialogporten
            dialogportenClient
                .getDialog(krav.dialogId)
                .onFailure { e ->
                    logger().error(dialogFiksLogg("Feil ved henting av dialog ${krav.dialogId} for krav ${krav.kravId}"), e)
                }.onSuccess { dialog ->
                    val attachments = dialog.attachments
                    if (attachments == null) {
                        logger().info(dialogFiksLogg("Dialog ${krav.dialogId} for krav ${krav.kravId} har ingen vedlegg"))
                        return@onSuccess
                    }
                    // Sjekker om attachment url er lik kravId
                    attachments.forEach { attachment ->
                        attachment.urls.forEach { url ->
                            logger().info(dialogFiksLogg("Dialog ${krav.dialogId} for krav ${krav.kravId} har vedlegg med url $url"))
                            url.url.hentUuidFraFritakKravPdfUrl()?.let { kravIdFraUrl ->
                                if (kravIdFraUrl == sisteKrav.kravId) {
                                    logger().info(
                                        dialogFiksLogg(
                                            "Dialog ${krav.dialogId} for krav ${krav.kravId} har vedlegg med url som matcher kravId, erstatter vedlegg",
                                        ),
                                    )
                                    // replaceAttachmentForDialog(krav.dialogId, krav.toFritakKravMelding())
                                } else {
                                    logger().warn(
                                        dialogFiksLogg(
                                            "Dialog ${krav.dialogId} for krav ${krav.kravId} har vedlegg med url som ikke matcher kravId, url: $url",
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun hentSisteKravIdFraTransmissions(dialogId: UUID): FritakAgpKravEntity? =
        fritakDialogRepository.hentAlleKravForDialog(dialogId).firstOrNull()

    private suspend fun replaceAttachmentForDialog(
        dialogId: UUID,
        kravMelding: FritakKravMelding,
    ) {
        dialogportenClient.replaceAttachments(
            dialogId,
            attachments =
                listOf(
                    createApiAttachment(
                        displayName = "Krav om fritak fra arbeidsgiverperioden",
                        url = kravMelding.toPdfUrl(),
                        mediaType = "application/pdf",
                    ),
                    createGuiAttachment(
                        displayName = "Krav om fritak fra arbeidsgiverperioden",
                        url = kravMelding.toPdfUrl(),
                        mediaType = "application/pdf",
                    ),
                ),
        )
    }

    private fun FritakAgpKravEntity.toFritakKravMelding(): FritakKravMelding =
        when (kravType) {
            FritakAgpType.GRAVID_KRAV_OPPRETTET -> {
                GravidKravOpprettet(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                )
            }

            FritakAgpType.GRAVID_KRAV_ENDRET -> {
                GravidKravEndret(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                    forrigeKrav = UUID.randomUUID(),
                )
            }

            FritakAgpType.GRAVID_KRAV_SLETTET -> {
                GravidKravSlettet(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                )
            }

            FritakAgpType.KRONISK_KRAV_OPPRETTET -> {
                KroniskKravOpprettet(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                )
            }

            FritakAgpType.KRONISK_KRAV_ENDRET -> {
                KroniskKravEndret(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                    forrigeKrav = UUID.randomUUID(),
                )
            }

            FritakAgpType.KRONISK_KRAV_SLETTET -> {
                KroniskKravSlettet(
                    id = kravId,
                    orgnr = Orgnr(orgnr),
                    navn = "N/A",
                    fnr = fnr,
                )
            }
        }
}

private fun dialogFiksLogg(melding: String) = "dialog-fiks: $melding"
