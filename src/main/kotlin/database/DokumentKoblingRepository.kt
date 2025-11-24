package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DokumentKoblingRepository(
    private val db: Database,
) {

    fun opprettSykmelding(
        sykmeldingId: UUID,
        status: Status,
    ) =
        try {
            transaction(db) {
                SykmeldingTable.insert {
                    it[id] = sykmeldingId
                    it[SykepengesoeknadTable.status] = status
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette sykmelding med id $sykmeldingId i databasen", e)
            throw e
        }

    fun hentSykmelding(
        sykmeldingId: UUID,
    ): SykmeldingEntity? =
        transaction(db) {
            SykmeldingEntity.findById(sykmeldingId)
        }

    fun opprettSykepengesoeknad(
        soeknadId: UUID,
        sykmelding: SykmeldingEntity,
        status: Status,
    ) =
        try {
            transaction(db) {
                SykepengesoeknadTable.insert {
                    it[id] = soeknadId
                    it[this.sykmeldingId] = sykmelding.id
                    it[SykepengesoeknadTable.status] = status
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette sykepengesoeknad med id $soeknadId i databasen", e)
            throw e
        }

    fun hentSykepengesoeknad(
        soeknadId: UUID,
    ): SykepengesoeknadEntity? =
        transaction(db) {
            SykepengesoeknadEntity.findById(soeknadId)
        }
}
