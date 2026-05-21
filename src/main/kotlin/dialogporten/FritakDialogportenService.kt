package no.nav.helsearbeidsgiver.dialogporten

import dialogporten.handlers.FritakAgpKravHandler
import no.nav.helsearbeidsgiver.database.FritakDialogRepository
import no.nav.helsearbeidsgiver.dialogporten.handlers.FritakAgpSoeknadHandler
import no.nav.helsearbeidsgiver.kafka.DialogMelding
import no.nav.helsearbeidsgiver.kafka.FritakKravMelding
import no.nav.helsearbeidsgiver.kafka.FritakSoeknadMelding

class FritakDialogportenService(
    fritakDialogRepository: FritakDialogRepository,
    dialogportenClient: DialogportenClient,
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
