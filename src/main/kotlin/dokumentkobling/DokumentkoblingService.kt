package dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.ForespoerselSykmeldingKobling
import java.util.UUID

class DokumentkoblingService(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
) {
    fun lagreSykmelding(sykmelding: Sykmelding) {
        dokumentkoblingRepository.opprettSykmelding(sykmelding)
    }

    fun lagreSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        dokumentkoblingRepository.opprettSykepengesoeknad(sykepengesoeknad)
    }

    fun lageVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad: VedtaksperiodeSoeknadKobling) {
        dokumentkoblingRepository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)
    }

    fun lagreForespoerselSendt(forespoerselSendt: ForespoerselSendt) {
        dokumentkoblingRepository.opprettForespoerselSendt(forespoerselSendt)
    }

    fun lagreForespoerselUtgaatt(forespoerselUtgaatt: ForespoerselUtgaatt) {
        dokumentkoblingRepository.opprettForespoerselUtgaatt(forespoerselUtgaatt)
    }

    fun hentForespoerslerKlarForBehandling(): List<ForespoerselSykmeldingKobling> =
        dokumentkoblingRepository
            .hentForespoerselSykmeldingKoblinger()
            .filter { it.sykmeldingStatus == Status.BEHANDLET }
            .filter { it.soeknadStatus == Status.BEHANDLET }
            .velgNyesteSykmeldingPerForespoersel()
            .sortedBy { it.forespoerselOpprettet }

    private fun List<ForespoerselSykmeldingKobling>.velgNyesteSykmeldingPerForespoersel(): List<ForespoerselSykmeldingKobling> =
        this
            .sortedByDescending { it.sykmeldingOpprettet }
            .distinctBy { Pair(it.forespoerselId, it.forespoerselStatus) }

    fun settForespoerselJobbTilBehandlet(forespoerselId: UUID) {
        dokumentkoblingRepository.settForespoerselJobbTilBehandlet(forespoerselId)
    }
}
