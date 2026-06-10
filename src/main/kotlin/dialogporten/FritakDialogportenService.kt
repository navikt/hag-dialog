package no.nav.helsearbeidsgiver.dialogporten

import dialogporten.handlers.FritakAgpKravHandler
import no.nav.helsearbeidsgiver.database.FritakAgpKravEntity
import no.nav.helsearbeidsgiver.database.FritakAgpType
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.domene.toTransmission
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

    suspend fun oppdaterKravTransmission() {
        val alleKrav =
            fritakDialogRepository
                .hentKravEldreEnnTidspunkt(LocalDateTime.of(2026, 5, 27, 11, 0))
        logger().info(dialogPrefiksLogg("Fant ${alleKrav.size} krav som skal oppdateres"))
        var antallOppdaterteKrav = 0
        var antallFeiledKrav = 0
        val feiledKrav = mutableListOf<Pair<UUID, UUID>>()
        alleKrav.forEach { krav ->
            logger().info(
                dialogPrefiksLogg(
                    "Oppdaterer krav med dialogId ${krav.dialogId} med kravId ${krav.kravId} og transmissionId ${krav.transmissionId}",
                ),
            )
            try {
                dialogportenClient.replaceTransmission(
                    krav.dialogId,
                    krav.transmissionId,
                    FritakKravReplaceTransmissionRequest(krav.toFritakKravMelding()).toTransmission(),
                )
                antallOppdaterteKrav++
            } catch (e: Exception) {
                antallFeiledKrav++
                feiledKrav.add(krav.dialogId to krav.transmissionId)
                logger().error(
                    dialogPrefiksLogg(
                        "Klarte ikke å oppdatere krav med dialogId ${krav.dialogId} med kravId ${krav.kravId} og transmissionId ${krav.transmissionId}",
                    ),
                    e,
                )
            }
        }
        logger().info(dialogPrefiksLogg("Oppdatert $antallOppdaterteKrav krav"))
        if (antallFeiledKrav > 0) {
            logger().warn(
                dialogPrefiksLogg(
                    "Feilet å oppdatere $antallFeiledKrav krav: ${feiledKrav.joinToString(
                        ", ",
                    ) { (dialogId, transmissionId) -> "dialogId=$dialogId, transmissionId=$transmissionId" }}",
                ),
            )
        }
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

private fun dialogPrefiksLogg(melding: String) = "dialog-fiks: $melding"
