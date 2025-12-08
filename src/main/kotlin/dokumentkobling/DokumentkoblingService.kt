package dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository

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

    fun lagreInntektsmeldingGodkjent(inntektsmeldingGodkjent: InntektsmeldingGodkjent) {
        dokumentkoblingRepository.opprettInntektmeldingGodkjent(
            inntektsmeldingId = inntektsmeldingGodkjent.inntektsmeldingId,
            forespoerselId = inntektsmeldingGodkjent.forespoerselId,
            vedtaksperiodeId = inntektsmeldingGodkjent.vedtaksperiodeId,
            kanal = inntektsmeldingGodkjent.kanal,
        )
    }
}
