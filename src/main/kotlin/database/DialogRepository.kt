package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class DialogRepository(
    private val db: Database,
) {
    fun lagreDialog(
        dialogId: UUID,
        sykmeldingId: UUID,
    ) {
        try {
            transaction(db) {
                DialogTable.insert {
                    it[id] = dialogId
                    it[this.sykmeldingId] = sykmeldingId
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre dialog med id $dialogId i databasen", e)
            throw e
        }
    }

    fun finnDialogMedSykemeldingId(sykmeldingId: UUID): DialogEntity? =
        transaction(db) {
            DialogEntity
                .find { DialogTable.sykmeldingId eq sykmeldingId }
                .firstOrNull()
        }

    fun oppdaterDialogMedTransmission(
        sykmeldingId: UUID,
        transmissionId: UUID,
        dokumentId: UUID,
        dokumentType: String,
        relatedTransmissionId: UUID? = null,
    ) {
        try {
            transaction(db) {
                val dialogId =
                    DialogTable
                        .selectAll()
                        .where(DialogTable.sykmeldingId eq sykmeldingId)
                        .firstOrNull()
                        ?.get(DialogTable.id)
                        ?.value
                        ?: throw IllegalArgumentException("Dialog med sykmeldingId $sykmeldingId finnes ikke")

                TransmissionTable.insert {
                    it[id] = transmissionId
                    it[this.dialogId] = dialogId
                    it[this.dokumentId] = dokumentId
                    it[this.dokumentType] = dokumentType
                    it[this.relatedTransmissionId] = relatedTransmissionId
                    it[opprettet] = LocalDateTime.now()
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å oppdatere dialog med sykmeldingId $sykmeldingId i databasen", e)
            throw e
        }
    }
}
