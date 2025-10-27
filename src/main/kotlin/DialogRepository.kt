package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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
                DialogEntitet.insert {
                    it[this.dialogId] = dialogId
                    it[this.sykmeldingId] = sykmeldingId
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre dialog med id $dialogId i databasen", e)
            throw e
        }
    }

    fun finnDialogId(sykmeldingId: UUID): UUID? =
        transaction(db) {
            DialogEntitet
                .selectAll()
                .where { DialogEntitet.sykmeldingId eq sykmeldingId }
                .map { it[DialogEntitet.dialogId] }
                .firstOrNull()
        }

    fun oppdaterDialogMedForespoerselTransmissionId(
        sykmeldingId: UUID,
        forespoerselTransmissionId: UUID,
    ) {
        try {
            transaction(db) {
                DialogEntitet.update({ DialogEntitet.sykmeldingId eq sykmeldingId }) {
                    it[this.forespoerselTransmissionId] = forespoerselTransmissionId
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å oppdatere dialog med sykmeldingId $sykmeldingId i databasen", e)
            throw e
        }
    }

    fun hentDialogMedSykmeldingId(sykmeldingId: UUID): DialogExposed? =
        transaction(db) {
            DialogEntitet
                .selectAll()
                .where { DialogEntitet.sykmeldingId eq sykmeldingId }
                .map { it.toDialogExposed() }
                .firstOrNull()
        }
}
