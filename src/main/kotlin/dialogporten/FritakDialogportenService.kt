package no.nav.helsearbeidsgiver.dialogporten

import dialogporten.handlers.FritakAgpKravHandler
import no.nav.helsearbeidsgiver.database.FritakAgpKravEntity
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.Attachment
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
                .hentKravEldreEnnTidspunkt(LocalDateTime.of(2026, 5, 27, 11, 0))
        val unikeDialogIder = alleKrav.map { it.dialogId }.distinct()
        var antallBehandlet = 0
        val ikkeMatchDialogIder = mutableSetOf<UUID>()

        logger().info(dialogFiksLogg("Starter å erstatte vedlegg for ${unikeDialogIder.size} unike dialoger"))

        unikeDialogIder.forEach { dialogId ->
            logger().info(dialogFiksLogg("Behandler dialogId $dialogId"))
            // Henter siste krav for dialogId i db
            val sisteKrav = hentSisteKravIdFraTransmissions(dialogId)
            if (sisteKrav == null) {
                logger().warn(dialogFiksLogg("Fant ingen krav for dialogId $dialogId, kan ikke erstatte vedlegg"))
                return@forEach
            }

            // Henter dialog fra dialogporten
            dialogportenClient
                .getDialog(dialogId)
                .onFailure { e ->
                    logger().error(dialogFiksLogg("Feil ved henting av dialog $dialogId for krav ${sisteKrav.kravId}"), e)
                }.onSuccess { dialog ->
                    val attachments = dialog.attachments
                    if (attachments.isNullOrEmpty()) {
                        logger().info(dialogFiksLogg("Dialog $dialogId for krav ${sisteKrav.kravId} har ingen vedlegg"))
                        return@onSuccess
                    }

                    val guiUrl =
                        attachments
                            .flatMap { it.urls }
                            .firstOrNull { it.consumerType == Attachment.Url.AttachmentUrlConsumerType.Gui }

                    if (guiUrl == null) {
                        logger().warn(dialogFiksLogg("Dialog $dialogId mangler GUI-url i vedlegg"))
                        ikkeMatchDialogIder.add(dialogId)
                        return@onSuccess
                    }

                    val kravIdFraUrl = guiUrl.url.hentUuidFraFritakKravPdfUrl()

                    if (kravIdFraUrl == null) {
                        logger().warn(dialogFiksLogg("Dialog $dialogId har GUI-url uten UUID: ${guiUrl.url}"))
                        ikkeMatchDialogIder.add(dialogId)
                        return@onSuccess
                    }

                    if (kravIdFraUrl == sisteKrav.kravId) {
                        // TODO: Vi kjører en dry-run for å få oversikt over hva som kan bli endret.
                        //   replaceAttachmentForDialog(dialogId, sisteKrav.toFritakKravMelding())
                        antallBehandlet++
                        logger().info(
                            dialogFiksLogg(
                                "Dialog $dialogId har match på UUID ($kravIdFraUrl). Erstatter URL i vedlegg.",
                            ),
                        )
                    } else {
                        ikkeMatchDialogIder.add(dialogId)
                        logger().warn(
                            dialogFiksLogg(
                                "Dialog $dialogId har ikke match på UUID i GUI-url ($kravIdFraUrl != ${sisteKrav.kravId}). Hopper over.",
                            ),
                        )
                    }
                }
        }

        logger().info(
            dialogFiksLogg(
                "Rapport: totalt ${unikeDialogIder.size} dialoger, behandlet $antallBehandlet, ikke behandlet pga ikke match ${ikkeMatchDialogIder.size}",
            ),
        )
        if (ikkeMatchDialogIder.isNotEmpty()) {
            logger().warn(dialogFiksLogg("Dialoger uten match (ikke behandlet): ${ikkeMatchDialogIder.joinToString()}"))
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
