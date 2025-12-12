package dokumentkobling

import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.ForespoerselSykmeldingKobling
import no.nav.helsearbeidsgiver.database.InntektsmeldingEntity
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DokumentkoblingService(
    private val dokumentkoblingRepository: DokumentkoblingRepository,
) {
    fun lagreSykmelding(sykmelding: Sykmelding) {
        dokumentkoblingRepository.opprettSykmelding(sykmelding)
    }

    fun hentSykmeldingOrgnr(sykmeldingId: UUID): Orgnr? = dokumentkoblingRepository.hentSykmeldingEntitet(sykmeldingId)?.data?.orgnr

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
            .filtrerNyesteSykmeldingPerForespoersel()
            .sortedBy { it.forespoerselOpprettet }

    // Dette gjøres fordi forespoersel skal koble seg til nyeste sykmelding dialogen (i tilfeller der en forespoersel er koblet til flere sykmeldinger)
    private fun List<ForespoerselSykmeldingKobling>.filtrerNyesteSykmeldingPerForespoersel(): List<ForespoerselSykmeldingKobling> =
        this
            .sortedByDescending { it.sykmeldingOpprettet }
            .distinctBy { Pair(it.forespoerselId, it.forespoerselStatus) }

    fun settForespoerselJobbTilBehandlet(forespoerselId: UUID) {
        dokumentkoblingRepository.settForespoerselJobbTilBehandlet(forespoerselId)
    }

    fun lagreInntektsmeldingGodkjent(inntektsmeldingGodkjent: InntektsmeldingGodkjent) {
        dokumentkoblingRepository.opprettInntektmeldingGodkjent(inntektsmeldingGodkjent)
    }

    fun lagreInntektsmeldingAvvist(inntektsmeldingAvvist: InntektsmeldingAvvist) {
        dokumentkoblingRepository.opprettInntektmeldingAvvist(inntektsmeldingAvvist)
    }

    fun hentInntektsmeldingerMedStatusMottatt(): List<InntektsmeldingEntity> =
        dokumentkoblingRepository.hentInntektsmeldingerMedStatusMottatt()

    fun settInntektsmeldingJobbTilBehandlet(inntektsmeldingId: UUID) {
        dokumentkoblingRepository.settInntektsmeldingJobbTilBehandlet(inntektsmeldingId)
    }

    // TODO sorter resultatet etter samme regel som i filtrerNyesteSykmeldingPerForespoersel
    fun hentKoblingMedForespoerselId(forespoerselId: UUID): ForespoerselSykmeldingKobling? =
        dokumentkoblingRepository.hentKoblingMedForespoerselId(forespoerselId).firstOrNull()
}
