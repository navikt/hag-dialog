import dokumentkobling.Status
import no.nav.helsearbeidsgiver.database.ForespoerselEntity
import no.nav.helsearbeidsgiver.database.ForespoerselTable
import no.nav.helsearbeidsgiver.database.InntektsmeldingEntity
import no.nav.helsearbeidsgiver.database.InntektsmeldingTable
import no.nav.helsearbeidsgiver.database.SykepengesoeknadEntity
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.database.VedtaksperiodeSoeknadEntity
import no.nav.helsearbeidsgiver.database.VedtaksperiodeSoeknadTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

fun hentSykmelding(
    db: Database,
    sykmeldingId: UUID,
): SykmeldingEntity? =
    transaction(db) {
        SykmeldingEntity.findById(sykmeldingId)
    }

fun hentSykepengesoeknad(
    db: Database,
    soeknadId: UUID,
): SykepengesoeknadEntity? =
    transaction(db) {
        SykepengesoeknadEntity.findById(soeknadId)
    }

fun hentListeAvSoeknadIdForVedtaksperiodeId(
    db: Database,
    vedtaksperiodeId: UUID,
): List<UUID> =
    transaction(db) {
        VedtaksperiodeSoeknadEntity.find { VedtaksperiodeSoeknadTable.vedtaksperiodeId eq vedtaksperiodeId }.map {
            it.soeknadId
        }
    }

fun hentSoeknaderForVedtaksperiodeId(
    db: Database,
    vedtaksperiodeId: UUID,
): List<VedtaksperiodeSoeknadEntity> =
    transaction(db) {
        VedtaksperiodeSoeknadEntity
            .find { VedtaksperiodeSoeknadTable.vedtaksperiodeId eq vedtaksperiodeId }
            .toList()
    }

fun hentForespoerslerMedStatusMottattEldstFoerst(db: Database): List<ForespoerselEntity> =
    transaction(db) {
        ForespoerselEntity
            .find { ForespoerselTable.status eq Status.MOTTATT }
            .orderBy(ForespoerselTable.opprettet to SortOrder.ASC)
            .toList()
    }

fun hentForespoersel(
    db: Database,
    forespoerselId: UUID,
): ForespoerselEntity? =
    transaction(db) {
        ForespoerselEntity
            .find { ForespoerselTable.forespoerselId eq forespoerselId }
            .firstOrNull()
    }

fun hentInntektsmeldingerMedStatusMottatt(db: Database): List<InntektsmeldingEntity> =
    transaction(db) {
        InntektsmeldingEntity
            .find { InntektsmeldingTable.status eq Status.MOTTATT }
            .orderBy(InntektsmeldingTable.opprettet to SortOrder.ASC)
            .toList()
    }

fun hentInntektsmelding(
    db: Database,
    inntektsmeldingId: UUID,
): InntektsmeldingEntity? =
    transaction(db) {
        InntektsmeldingEntity.findById(inntektsmeldingId)
    }
