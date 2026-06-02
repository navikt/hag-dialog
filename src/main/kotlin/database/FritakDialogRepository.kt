package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class FritakDialogRepository(
    private val db: Database,
) {
    fun lagreSoeknadDialog(
        dialogId: UUID,
        soeknadId: UUID,
        soeknadType: FritakAgpSoeknadType,
        fnr: String,
        orgnr: String,
    ) {
        try {
            transaction(db) {
                FritakAgpSoeknadTable.insert {
                    it[id] = dialogId
                    it[this.soeknadId] = soeknadId
                    it[this.soeknadType] = soeknadType
                    it[this.fnr] = fnr
                    it[this.orgnr] = orgnr
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre fritak-dialog med dialogId $dialogId i databasen", e)
            throw e
        }
    }

    fun lagreKravDialog(
        dialogId: UUID,
        transmissionId: UUID,
        kravId: UUID,
        kravType: FritakAgpType,
        fnr: String,
        orgnr: String,
    ) {
        try {
            transaction(db) {
                FritakAgpKravTable.insert {
                    it[id] = transmissionId
                    it[this.dialogId] = dialogId
                    it[this.kravId] = kravId
                    it[this.kravType] = kravType
                    it[this.fnr] = fnr
                    it[this.orgnr] = orgnr
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å lagre fritak-krav-dialog med dialogId $dialogId i databasen", e)
            throw e
        }
    }

    fun finnDialogMedKravId(kravId: UUID): FritakAgpKravEntity? =
        transaction(db) {
            FritakAgpKravEntity
                .find {
                    (FritakAgpKravTable.kravId eq kravId)
                }.firstOrNull()
        }

    fun hentAlleSoeknader(): List<FritakAgpSoeknadEntity> =
        transaction(db) {
            FritakAgpSoeknadEntity.all().toList()
        }

    fun hentKravEldreEnnTidspunkt(tidspunkt: LocalDateTime): List<FritakAgpKravEntity> =
        transaction(db) {
            FritakAgpKravEntity.find { FritakAgpKravTable.opprettet lessEq tidspunkt }.toList()
        }

    fun hentAlleKravForDialog(dialogId: UUID): List<FritakAgpKravEntity> =
        transaction(db) {
            FritakAgpKravEntity
                .find { FritakAgpKravTable.dialogId eq dialogId }
                .orderBy(FritakAgpKravTable.opprettet to SortOrder.DESC)
                .toList()
        }
}
