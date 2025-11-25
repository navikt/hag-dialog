package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class DokumentKoblingRepository(
    private val db: Database,
) {
    fun opprettSykmelding(
        sykmeldingId: UUID,
        status: Status,
    ) = try {
        transaction(db) {
            SykmeldingTable.insert {
                it[id] = sykmeldingId
                it[SykmeldingTable.status] = status
            }
        }
    } catch (e: ExposedSQLException) {
        sikkerLogger().error("Klarte ikke å opprette sykmelding med id $sykmeldingId i databasen", e)
        throw e
    }

    fun hentSykmelding(sykmeldingId: UUID): SykmeldingEntity? =
        transaction(db) {
            SykmeldingEntity.findById(sykmeldingId)
        }

    fun opprettSykepengesoeknad(
        soeknadId: UUID,
        sykmeldingId: UUID,
        status: Status,
    ) = try {
        transaction(db) {
            SykepengesoeknadTable.insert {
                it[id] = soeknadId
                it[SykepengesoeknadTable.sykmeldingId] = sykmeldingId
                it[SykepengesoeknadTable.status] = status
            }
        }
    } catch (e: ExposedSQLException) {
        sikkerLogger().error("Klarte ikke å opprette sykepengesoeknad med id $soeknadId i databasen", e)
        throw e
    }

    fun hentSykepengesoeknad(soeknadId: UUID): SykepengesoeknadEntity? =
        transaction(db) {
            SykepengesoeknadEntity.findById(soeknadId)
        }

    fun henteSykemeldingerMedStatusMotatt(): List<SykmeldingEntity> =
        transaction(db) {
            SykmeldingEntity.find { SykmeldingTable.status eq Status.MOTATT }.toList()
        }

    fun settSykmeldingStatusTilBehandlet(sykmeldingId: UUID): Unit =
        transaction(db) {
            SykmeldingTable.update({ SykmeldingTable.id eq sykmeldingId }) {
                it[SykmeldingTable.status] = Status.BEHANDLET
            }
        }
}
