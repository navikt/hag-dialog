package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
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
                DialogEntity.new(dialogId) {
                    this.sykmeldingId = sykmeldingId
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

                TransmissionEntity.new(transmissionId) {
                    this.dialog = dialog
                    this.dokumentId = dokumentId
                    this.dokumentType = dokumentType
                    this.relatedTransmission = relatedTransmission
                    this.opprettet = LocalDateTime.now()
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å oppdatere dialog med sykmeldingId $sykmeldingId i databasen", e)
            throw e
        }
    }
}
