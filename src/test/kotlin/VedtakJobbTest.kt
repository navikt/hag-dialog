import dokumentkobling.Status
import dokumentkobling.VedtakJobb
import dokumentkobling.opprettTransmissionForVedtak
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.VedtakInntektsmeldingKobling
import no.nav.helsearbeidsgiver.dialogporten.SykepengerDialogportenService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import java.util.UUID

class VedtakJobbTest :
    FunSpec({

        val repository = mockk<DokumentkoblingRepository>()
        val sykepengerDialogportenService = mockk<SykepengerDialogportenService>(relaxed = true)
        val unleashFeatureToggles = UnleashFeatureToggles()

        val vedtakJobb =
            VedtakJobb(
                dokumentkoblingRepository = repository,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            )

        val vedtak = DokumentKoblingMockUtils.vedtak

        fun lagKobling(inntektsmeldingJobbStatus: Status) =
            VedtakInntektsmeldingKobling(
                vedtakId = vedtak.vedtakId,
                sykmeldingId = vedtak.sykmeldingId,
                inntektsmeldingId = vedtak.inntektsmeldingId,
                orgnr = vedtak.orgnr,
                vedtakStatus = Status.MOTTATT,
                inntektsmeldingJobbStatus = inntektsmeldingJobbStatus,
            )

        beforeTest {
            clearAllMocks()
            every { repository.settVedtakJobbTilBehandlet(any()) } just runs
            every { repository.hentVedtakKlareForBehandling() } returns listOf(lagKobling(Status.BEHANDLET))
            every { sykepengerDialogportenService.oppdaterDialogMedVedtak(any()) } just runs
        }

        test("vedtakJobb skal opprette transmission når inntektsmelding er behandlet") {
            vedtakJobb.doJob()

            verify(exactly = 1) { repository.hentVedtakKlareForBehandling() }
            verify(exactly = 1) { sykepengerDialogportenService.oppdaterDialogMedVedtak(match { it.vedtakId == vedtak.vedtakId }) }
            verify(exactly = 1) { repository.settVedtakJobbTilBehandlet(vedtak.vedtakId) }
        }

        test("vedtakJobb skal ikke opprette transmission når inntektsmelding ikke er behandlet") {
            every { repository.hentVedtakKlareForBehandling() } returns listOf(lagKobling(Status.MOTTATT))

            vedtakJobb.doJob()

            verify(exactly = 1) { repository.hentVedtakKlareForBehandling() }
            verify(exactly = 0) { sykepengerDialogportenService.oppdaterDialogMedVedtak(any()) }
            verify(exactly = 0) { repository.settVedtakJobbTilBehandlet(any()) }
        }

        test("vedtakJobb skal fortsatt opprette transmission på vedtak #2 når behandling av vedtak #1 kaster exception") {
            val exceptionVedtakId = UUID.randomUUID()
            val exceptionKobling = lagKobling(Status.BEHANDLET).copy(vedtakId = exceptionVedtakId)

            every { repository.hentVedtakKlareForBehandling() } returns
                listOf(exceptionKobling, lagKobling(Status.BEHANDLET))
            every {
                sykepengerDialogportenService.opprettTransmissionForVedtak(
                    vedtakId = exceptionVedtakId,
                    sykmeldingId = exceptionKobling.sykmeldingId,
                    inntektsmeldingId = exceptionKobling.inntektsmeldingId,
                    orgnr = exceptionKobling.orgnr,
                )
            } throws NotFoundException("Fant ikke dialog for vedtak")

            vedtakJobb.doJob()

            verify(exactly = 1) { repository.hentVedtakKlareForBehandling() }
            verify(exactly = 2) { sykepengerDialogportenService.oppdaterDialogMedVedtak(any()) }
            verify(exactly = 1) { repository.settVedtakJobbTilBehandlet(vedtak.vedtakId) }
        }
    })
