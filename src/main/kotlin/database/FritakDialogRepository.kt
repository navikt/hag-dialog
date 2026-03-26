package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class FritakDialogRepository(
    private val db: Database,
) {
    fun lagreDialog(
        dialogId: UUID,
        dokumentId: UUID,
        dokumentType: FritakAgpDokType,
        fnr: String,
    ) {
        try {
            transaction(db) {
                FritakAgpDialogTable.insert {
                    it[this.dialogId] = dialogId
                    it[this.dokumentId] = dokumentId
                    it[this.dokumentType] = dokumentType
                    it[this.fnr] = fnr
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre fritak-dialog med dialogId $dialogId i databasen", e)
            throw e
        }
    }

    fun finnDialogMedDokumentId(dokumentId: UUID): FritakAgpDialogEntity? =
        transaction(db) {
            FritakAgpDialogEntity
                .find { FritakAgpDialogTable.dokumentId eq dokumentId }
                .firstOrNull()
        }
}

