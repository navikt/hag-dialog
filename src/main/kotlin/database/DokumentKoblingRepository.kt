package no.nav.helsearbeidsgiver.database

import no.nav.helsearbeidsgiver.dokumentKobling.ForespoerselSendt
import no.nav.helsearbeidsgiver.dokumentKobling.ForespoerselUtgaatt
import no.nav.helsearbeidsgiver.dokumentKobling.Status
import no.nav.helsearbeidsgiver.dokumentKobling.Sykepengesoeknad
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.dokumentKobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
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
                    it[SykmeldingTable.status] = Status.MOTTATT
                    it[SykmeldingTable.data] = sykmelding
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette sykmelding med id ${sykmelding.sykmeldingId} i databasen", e)
            throw e
        }

    fun hentSykmeldingEntitet(sykmeldingId: UUID): SykmeldingEntity? =
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
                    it[SykepengesoeknadTable.status] = Status.MOTTATT
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

    fun henteSykemeldingerMedStatusMottatt(): List<Sykmelding> =
        transaction(db) {
            SykmeldingEntity.find { SykmeldingTable.status eq Status.MOTTATT }.map { sykmelding ->
                sykmelding.data
            }
        }

    fun henteSykepengeSoeknaderMedStatusMottatt(): List<Sykepengesoeknad> =
        transaction(db) {
            SykepengesoeknadEntity.find { SykepengesoeknadTable.status eq Status.MOTTATT }.map {
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

    fun opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad: VedtaksperiodeSoeknadKobling) =

        try {
            transaction(db) {
                val eksisterendeKobling =
                    VedtaksperiodeSoeknadEntity.find {
                        (VedtaksperiodeSoeknadTable.vedtaksperiodeId eq vedtaksperiodeSoeknad.vedtaksperiodeId) and
                            (VedtaksperiodeSoeknadTable.soeknadId eq vedtaksperiodeSoeknad.soeknadId)
                    }
                if (eksisterendeKobling.empty()) {
                    VedtaksperiodeSoeknadTable.insert {
                        it[vedtaksperiodeId] = vedtaksperiodeSoeknad.vedtaksperiodeId
                        it[soeknadId] = vedtaksperiodeSoeknad.soeknadId
                    }
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error(
                "Klarte ikke å opprette vedtaksperiode_soeknad kobling mellom v: ${vedtaksperiodeSoeknad.vedtaksperiodeId} <-> s: ${vedtaksperiodeSoeknad.soeknadId} i databasen",
                e,
            )
            throw e
        }

    fun hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeId: UUID): List<UUID> =
        transaction(db) {
            VedtaksperiodeSoeknadEntity.find { VedtaksperiodeSoeknadTable.vedtaksperiodeId eq vedtaksperiodeId }.map {
                it.soeknadId
            }
        }

    fun hentSoeknaderForVedtaksperiodeId(vedtaksperiodeId: UUID): List<VedtaksperiodeSoeknadEntity> =
        transaction(db) {
            VedtaksperiodeSoeknadEntity
                .find { VedtaksperiodeSoeknadTable.vedtaksperiodeId eq vedtaksperiodeId }
                .toList()
        }

    fun opprettForespoerselSendt(forespoerselSendt: ForespoerselSendt) {
        opprettForespoersel(
            forespoerselId = forespoerselSendt.forespoerselId,
            vedtaksperiodeId = forespoerselSendt.vedtaksperiodeId,
            ForespoerselStatus.SENDT,
        )
    }

    fun opprettForespoerselUtgaatt(forespoerselUtgaatt: ForespoerselUtgaatt) {
        opprettForespoersel(
            forespoerselId = forespoerselUtgaatt.forespoerselId,
            vedtaksperiodeId = forespoerselUtgaatt.vedtaksperiodeId,
            ForespoerselStatus.UTGAATT,
        )
    }

    fun opprettForespoersel(
        forespoerselId: UUID,
        vedtaksperiodeId: UUID,
        forespoerselStatus: ForespoerselStatus,
    ) {
        try {
            transaction(db) {
                ForespoerselTable.insert {
                    it[ForespoerselTable.forespoerselId] = forespoerselId
                    it[ForespoerselTable.vedtaksperiodeId] = vedtaksperiodeId
                    it[ForespoerselTable.status] = Status.MOTTATT
                    it[ForespoerselTable.forespoerselStatus] = forespoerselStatus
                }
            }
        } catch (e: ExposedSQLException) {
            sikkerLogger().error("Klarte ikke å opprette forespørsel sendt med id $forespoerselId i databasen", e)
            throw e
        }
    }

    fun hentForespoerselerMedStatusMottattEldstFoerst(): List<ForespoerselEntity> =
        transaction(db) {
            ForespoerselEntity
                .find { ForespoerselTable.status eq Status.MOTTATT }
                .orderBy(ForespoerselTable.opprettet to SortOrder.ASC)
                .toList()
        }
}
