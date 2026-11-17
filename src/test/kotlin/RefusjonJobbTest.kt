import dokumentkobling.RefusjonJobb
import dokumentkobling.Status
import dokumentkobling.opprettTransmissionForRefusjon
import io.kotest.core.spec.style.FunSpec
import io.ktor.server.plugins.NotFoundException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.RefusjonInntektsmeldingKobling
import no.nav.helsearbeidsgiver.dialogporten.SykepengerDialogportenService
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import java.util.UUID

class RefusjonJobbTest :
    FunSpec({

        val repository = mockk<DokumentkoblingRepository>()
        val sykepengerDialogportenService = mockk<SykepengerDialogportenService>(relaxed = true)
        val unleashFeatureToggles = UnleashFeatureToggles()

        val refusjonJobb =
            RefusjonJobb(
                dokumentkoblingRepository = repository,
                sykepengerDialogportenService = sykepengerDialogportenService,
                unleashFeatureToggles = unleashFeatureToggles,
            )

        val refusjon = DokumentKoblingMockUtils.refusjon

        fun lagKobling(inntektsmeldingJobbStatus: Status) =
            RefusjonInntektsmeldingKobling(
                refusjonUtfallId = refusjon.refusjonUtfallId,
                sykmeldingId = refusjon.sykmeldingId,
                inntektsmeldingId = refusjon.inntektsmeldingId,
                orgnr = refusjon.orgnr,
                refusjonStatus = Status.MOTTATT,
                inntektsmeldingJobbStatus = inntektsmeldingJobbStatus,
            )

        beforeTest {
            clearAllMocks()
            every { repository.settRefusjonJobbTilBehandlet(any()) } just runs
            every { repository.hentRefusjonKlareForBehandling() } returns listOf(lagKobling(Status.BEHANDLET))
            every { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) } just runs
        }

        test("refusjonJobb skal opprette transmission når inntektsmelding er behandlet") {
            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 1) {
                sykepengerDialogportenService.oppdaterDialogMedRefusjon(
                    match {
                        it.refusjonUtfallId ==
                            refusjon.refusjonUtfallId
                    },
                )
            }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }

        test("refusjonJobb skal ikke opprette transmission når inntektsmelding ikke er behandlet") {
            every { repository.hentRefusjonKlareForBehandling() } returns listOf(lagKobling(Status.MOTTATT))

            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 0) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 0) { repository.settRefusjonJobbTilBehandlet(any()) }
        }

        test("refusjonJobb skal fortsatt opprette transmission på refusjon #2 når behandling av refusjon #1 kaster exception") {
            val exceptionRefusjonUtfallId = UUID.randomUUID()
            val exceptionKobling = lagKobling(Status.BEHANDLET).copy(refusjonUtfallId = exceptionRefusjonUtfallId)

            every { repository.hentRefusjonKlareForBehandling() } returns
                listOf(exceptionKobling, lagKobling(Status.BEHANDLET))
            every {
                sykepengerDialogportenService.opprettTransmissionForRefusjon(
                    refusjonUtfallId = exceptionRefusjonUtfallId,
                    sykmeldingId = exceptionKobling.sykmeldingId,
                    inntektsmeldingId = exceptionKobling.inntektsmeldingId,
                    orgnr = exceptionKobling.orgnr,
                )
            } throws NotFoundException("Fant ikke dialog for refusjon")

            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 2) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }
    })
