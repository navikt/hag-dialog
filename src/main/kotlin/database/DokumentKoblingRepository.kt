package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.dokumentKobling.Sykepengesoeknad
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.UUID

class DokumentKoblingRepository(
    private val db: Database,
) {
    fun opprettSykmelding(sykmelding: Sykmelding) =
        try {
            transaction(db) {
                SykmeldingTable.insert {
                    it[id] = sykmelding.sykmeldingId
                    it[SykmeldingTable.status] = Status.MOTATT
                    it[SykmeldingTable.data] = sykmelding
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette sykmelding med id ${sykmelding.sykmeldingId} i databasen", e)
            throw e
        }

    fun hentSykmelding(sykmeldingId: UUID): SykmeldingEntity? =
        transaction(db) {
            SykmeldingEntity.findById(sykmeldingId)
        }

    fun opprettSykepengesoeknad(soeknad: Sykepengesoeknad) =
        try {
            transaction(db) {
                SykepengesoeknadTable.insert {
                    it[id] = soeknad.soeknadId
                    it[SykepengesoeknadTable.sykmeldingId] = soeknad.sykmeldingId
                    it[SykepengesoeknadTable.orgnr] = soeknad.orgnr.verdi
                    it[SykepengesoeknadTable.status] = Status.MOTATT
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette sykepengesoeknad med id ${soeknad.soeknadId} i databasen", e)
            throw e
        }

    fun hentSykepengesoeknad(soeknadId: UUID): SykepengesoeknadEntity? =
        transaction(db) {
            SykepengesoeknadEntity.findById(soeknadId)
        }

    fun henteSykemeldingerMedStatusMotatt(): List<Pair<Sykmelding, Status>> =
        transaction(db) {
            SykmeldingEntity.find { SykmeldingTable.status eq Status.MOTATT }.map { sykmelding ->
                Pair(sykmelding.data, sykmelding.status)
            }
        }

    fun henteSykepengeSoeknaderMedStatusMotatt(): List<Sykepengesoeknad> =
        transaction(db) {
            SykepengesoeknadEntity.find { SykmeldingTable.status eq Status.MOTATT }.map {
                Sykepengesoeknad(
                    soeknadId = it.id.value,
                    sykmeldingId = it.sykmeldingId,
                    orgnr = Orgnr(it.orgnr),
                )
            }
        }

    fun settSykmeldingStatusTilBehandlet(sykmeldingId: UUID): Unit =
        transaction(db) {
            SykmeldingTable.update({ SykmeldingTable.id eq sykmeldingId }) {
                it[SykmeldingTable.status] = Status.BEHANDLET
            }
        }

    fun settSykepengeSoeknadStatusTilBehandlet(soeknadId: UUID): Unit =
        transaction(db) {
            SykepengesoeknadTable.update({ SykepengesoeknadTable.id eq soeknadId }) {
                it[SykepengesoeknadTable.status] = Status.BEHANDLET
            }
        }
}
