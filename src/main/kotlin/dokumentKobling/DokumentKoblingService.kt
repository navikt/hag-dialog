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
}