package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.notifikasjon.NotifikasjonStatus
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class NotifikasjonRepository(
    private val db: Database,
    private val maksAntallPerHenting: Int,
) {
    fun opprettNotifikasjon(
        dokumentId: UUID,
        tjeneste: Tjeneste,
    ) = try {
        transaction(db) {
            NotifikasjonTable.insertIgnore {
                it[id] = UUID.randomUUID()
                it[NotifikasjonTable.dokumentId] = dokumentId
                it[NotifikasjonTable.tjeneste] = tjeneste
                it[NotifikasjonTable.status] = NotifikasjonStatus.NY
            }
        }
    } catch (e: ExposedSQLException) {
        sikkerLogger().error(
            "Klarte ikke å opprette notifikasjon for tjeneste $tjeneste og dokument $dokumentId i databasen",
            e,
        )
        throw e
    }

    fun hentNyeNotifikasjoner(): List<Notifikasjon> =
        transaction(db) {
            NotifikasjonEntity
                .find { NotifikasjonTable.status eq NotifikasjonStatus.NY }
                .orderBy(NotifikasjonTable.opprettet to SortOrder.ASC)
                .limit(maksAntallPerHenting)
                .map {
                    Notifikasjon(
                        notifikasjonId = it.id.value,
                        dokumentId = it.dokumentId,
                        tjeneste = it.tjeneste,
                    )
                }
        }

    fun settNotifikasjonSendt(notifikasjonId: UUID): Unit =
        transaction(db) {
            NotifikasjonTable.update({ NotifikasjonTable.id eq notifikasjonId }) {
                it[NotifikasjonTable.status] = NotifikasjonStatus.SENDT
            }
        }
}
