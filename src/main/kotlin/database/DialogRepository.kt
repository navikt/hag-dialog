package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
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
                    it[this.id] = dialogId
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
        relatedTransmission: UUID? = null,
    ) {
        try {
            transaction(db) {
                val dialog =
                    DialogEntity.find { DialogTable.sykmeldingId eq sykmeldingId }.firstOrNull()
                        ?: throw IllegalArgumentException("Dialog med sykmeldingId $sykmeldingId finnes ikke")

                TransmissionTable.insert {
                    it[id] = transmissionId
                    it[dialogId] = dialog.id
                    it[TransmissionTable.dokumentId] = dokumentId
                    it[TransmissionTable.dokumentType] = dokumentType
                    it[TransmissionTable.relatedTransmission] = relatedTransmission
                    it[opprettet] = LocalDateTime.now()
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å oppdatere dialog med sykmeldingId $sykmeldingId i databasen", e)
            throw e
        }
    }
}
