package no.nav.helsearbeidsgiver.dokumentKobling

import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository

class DokumentKoblingService(
    private val dokumentKoblingRepository: DokumentKoblingRepository,
) {
    fun lagreSykmelding(sykmelding: Sykmelding) {
        dokumentKoblingRepository.opprettSykmelding(sykmelding)
    }

    fun lagreSykepengesoeknad(sykepengesoeknad: Sykepengesoeknad) {
        dokumentKoblingRepository.opprettSykepengesoeknad(sykepengesoeknad)
    }

    fun lageVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad: VedtaksperiodeSoeknadKobling) {
        dokumentKoblingRepository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)
    }

    fun lagreForespoerselSendt(forespoerselSendt: ForespoerselSendt) {
        dokumentKoblingRepository.opprettForespoerselSendt(forespoerselSendt)
    }

    fun lagreForespoerselUtgaatt(forespoerselUtgaatt: ForespoerselUtgaatt) {
        dokumentKoblingRepository.opprettForespoerselUtgaatt(forespoerselUtgaatt)
    }
}
