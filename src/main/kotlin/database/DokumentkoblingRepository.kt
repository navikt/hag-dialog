package no.nav.helsearbeidsgiver.database

import dokumentkobling.ForespoerselSendt
import dokumentkobling.ForespoerselUtgaatt
import dokumentkobling.InnsendingType
import dokumentkobling.InntektsmeldingAvvist
import dokumentkobling.InntektsmeldingGodkjent
import dokumentkobling.Status
import dokumentkobling.Sykepengesoeknad
import dokumentkobling.Sykmelding
import dokumentkobling.VedtaksperiodeSoeknadKobling
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.innerJoin
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID

class DokumentkoblingRepository(
    private val db: Database,
    private val maksAntallPerHenting: Int,
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
            SykmeldingEntity
                .find { SykmeldingTable.status eq Status.MOTTATT }
                .orderBy(SykmeldingTable.opprettet to SortOrder.ASC)
                .limit(maksAntallPerHenting)
                .map { it.data }
        }

    fun henteSykepengeSoeknaderMedStatusMottatt(): List<Sykepengesoeknad> =
        transaction(db) {
            SykepengesoeknadEntity
                .find { SykepengesoeknadTable.status eq Status.MOTTATT }
                .orderBy(SykepengesoeknadTable.opprettet to SortOrder.ASC)
                .limit(maksAntallPerHenting)
                .map {
                    Sykepengesoeknad(
                        soeknadId = it.id.value,
                        sykmeldingId = it.sykmeldingId,
                        orgnr = Orgnr(it.orgnr),
                    )
                }
        }

    fun settSykmeldingJobbTilBehandlet(sykmeldingId: UUID): Unit =
        transaction(db) {
            SykmeldingTable.update({ SykmeldingTable.id eq sykmeldingId }) {
                it[SykmeldingTable.status] = Status.BEHANDLET
            }
        }

    fun settSykepengeSoeknadJobbTilBehandlet(soeknadId: UUID): Unit =
        transaction(db) {
            SykepengesoeknadTable.update({ SykepengesoeknadTable.id eq soeknadId }) {
                it[SykepengesoeknadTable.status] = Status.BEHANDLET
            }
        }

    fun settForespoerselJobbTilBehandlet(forespoerselId: UUID): Unit =
        transaction(db) {
            ForespoerselTable.update({ ForespoerselTable.forespoerselId eq forespoerselId }) {
                it[ForespoerselTable.status] = Status.BEHANDLET
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

    fun hentForespoerslerMedStatusMottattEldstFoerst(): List<ForespoerselEntity> =
        transaction(db) {
            ForespoerselEntity
                .find { ForespoerselTable.status eq Status.MOTTATT }
                .orderBy(ForespoerselTable.opprettet to SortOrder.ASC)
                .toList()
        }

    fun hentForespoerselSykmeldingKoblinger(): List<ForespoerselSykmeldingKobling> =
        hentForespoerselSykmeldingKoblinger(status = Status.MOTTATT)

    fun hentKoblingMedForespoerselId(forespoerselId: UUID): List<ForespoerselSykmeldingKobling> =
        hentForespoerselSykmeldingKoblinger(forespoerselId = forespoerselId)

    private fun hentForespoerselSykmeldingKoblinger(
        forespoerselId: UUID? = null,
        status: Status? = null,
    ): List<ForespoerselSykmeldingKobling> =
        transaction(db) {
            ForespoerselTable
                .innerJoin(
                    VedtaksperiodeSoeknadTable,
                    { ForespoerselTable.vedtaksperiodeId },
                    { VedtaksperiodeSoeknadTable.vedtaksperiodeId },
                ).innerJoin(
                    SykepengesoeknadTable,
                    { VedtaksperiodeSoeknadTable.soeknadId },
                    { SykepengesoeknadTable.soeknadId },
                ).innerJoin(
                    SykmeldingTable,
                    { SykepengesoeknadTable.sykmeldingId },
                    { SykmeldingTable.sykmeldingId },
                ).selectAll()
                .where { (ForespoerselTable.status eq Status.MOTTATT) }
                .orderBy(ForespoerselTable.opprettet to SortOrder.ASC)
                .apply {
                    forespoerselId?.let { andWhere { ForespoerselTable.forespoerselId eq it } }
                    status?.let { andWhere { ForespoerselTable.status eq it } }
                }
                .limit(maksAntallPerHenting)
                .map {
                    ForespoerselSykmeldingKobling(
                        forespoerselId = it[ForespoerselTable.forespoerselId],
                        forespoerselStatus = it[ForespoerselTable.forespoerselStatus],
                        forespoerselOpprettet = it[ForespoerselTable.opprettet],
                        vedtaksperiodeId = it[VedtaksperiodeSoeknadTable.vedtaksperiodeId],
                        soeknadId = it[SykepengesoeknadTable.id].value,
                        sykmeldingId = it[SykmeldingTable.id].value,
                        sykmeldingOpprettet = it[SykmeldingTable.opprettet],
                        sykmeldingStatus = it[SykmeldingTable.status],
                        soeknadStatus = it[SykepengesoeknadTable.status],
                        forespoerselJobbStatus = it[ForespoerselTable.status],
                    )
                }
        }

    data class ForespoerselSykmeldingKobling(
        val forespoerselId: UUID,
        val forespoerselStatus: ForespoerselStatus,
        val forespoerselOpprettet: LocalDateTime,
        val vedtaksperiodeId: UUID,
        val soeknadId: UUID,
        val sykmeldingId: UUID,
        val sykmeldingOpprettet: LocalDateTime,
        val sykmeldingStatus: Status,
        val soeknadStatus: Status,
        val forespoerselJobbStatus: Status,
    )

    fun opprettInntektmeldingGodkjent(inntektsmeldingGodkjent: InntektsmeldingGodkjent) {
        transaction(db) {
            InntektsmeldingTable.insert {
                it[id] = inntektsmeldingGodkjent.inntektsmeldingId
                it[InntektsmeldingTable.forespoerselId] = inntektsmeldingGodkjent.forespoerselId
                it[InntektsmeldingTable.vedtaksperiodeId] = inntektsmeldingGodkjent.vedtaksperiodeId
                it[InntektsmeldingTable.status] = Status.MOTTATT
                it[InntektsmeldingTable.inntektsmeldingStatus] = InntektsmeldingStatus.GODKJENT
                it[InntektsmeldingTable.innsendingType] = inntektsmeldingGodkjent.innsendingType
            }
        }
    }

    fun opprettInntektmeldingAvvist(inntektsmeldingAvvist: InntektsmeldingAvvist) {
        transaction(db) {
            InntektsmeldingTable.insert {
                it[id] = inntektsmeldingAvvist.inntektsmeldingId
                it[InntektsmeldingTable.forespoerselId] = inntektsmeldingAvvist.forespoerselId
                it[InntektsmeldingTable.vedtaksperiodeId] = inntektsmeldingAvvist.vedtaksperiodeId
                it[InntektsmeldingTable.status] = Status.MOTTATT
                it[InntektsmeldingTable.inntektsmeldingStatus] = InntektsmeldingStatus.AVVIST
                it[InntektsmeldingTable.innsendingType] = InnsendingType.FORESPURT_EKSTERN
            }
        }
    }

    fun hentInntektsmeldingerMedStatusMottatt(): List<InntektsmeldingEntity> =
        transaction(db) {
            InntektsmeldingEntity
                .find { InntektsmeldingTable.status eq Status.MOTTATT }
                .orderBy(InntektsmeldingTable.opprettet to SortOrder.ASC)
                .limit(maksAntallPerHenting)
                .toList()
        }

    fun setInntektsmeldingJobbTilBehandlet(inntektsmeldingId: UUID): Unit =
        transaction(db) {
            InntektsmeldingTable.update({ InntektsmeldingTable.id eq inntektsmeldingId }) {
                it[InntektsmeldingTable.status] = Status.BEHANDLET
            }
        }
}
