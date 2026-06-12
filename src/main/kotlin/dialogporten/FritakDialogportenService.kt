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
}
