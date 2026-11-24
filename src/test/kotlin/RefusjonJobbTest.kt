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
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
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

        val sykmeldingEntity = mockk<SykmeldingEntity>()

        beforeTest {
            clearAllMocks()
            every { repository.settRefusjonJobbTilBehandlet(any()) } just runs
            every { repository.hentRefusjonKlareForBehandling() } returns listOf(refusjon)
            every { repository.hentSykmeldingEntitet(refusjon.sykmeldingId) } returns sykmeldingEntity
            every { sykmeldingEntity.status } returns Status.BEHANDLET
            every { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) } just runs
        }

        test("refusjonJobb skal opprette transmission når sykmelding er behandlet") {
            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 1) { repository.hentSykmeldingEntitet(refusjon.sykmeldingId) }
            verify(exactly = 1) {
                sykepengerDialogportenService.oppdaterDialogMedRefusjon(
                    match {
                        it.refusjonUtfallId == refusjon.refusjonUtfallId &&
                            it.sykmeldingId == refusjon.sykmeldingId
                    },
                )
            }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }

        test("refusjonJobb skal ikke opprette transmission når sykmelding ikke er behandlet") {
            every { sykmeldingEntity.status } returns Status.MOTTATT

            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 0) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 0) { repository.settRefusjonJobbTilBehandlet(any()) }
        }

        test("refusjonJobb skal vente til sykmelding finnes og er behandlet") {
            every { repository.hentSykmeldingEntitet(refusjon.sykmeldingId) } returns null

            refusjonJobb.doJob()

            verify(exactly = 0) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 0) { repository.settRefusjonJobbTilBehandlet(any()) }

            every { repository.hentSykmeldingEntitet(refusjon.sykmeldingId) } returns sykmeldingEntity

            refusjonJobb.doJob()

            verify(exactly = 1) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }

        test("refusjonJobb skal opprette transmission uten inntektsmeldingId") {
            every { repository.hentRefusjonKlareForBehandling() } returns listOf(refusjon.copy(inntektsmeldingId = null))

            refusjonJobb.doJob()

            verify(exactly = 1) {
                sykepengerDialogportenService.oppdaterDialogMedRefusjon(
                    match { it.sykmeldingId == refusjon.sykmeldingId && it.inntektsmeldingId == null },
                )
            }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }

        test("refusjonJobb skal fortsatt opprette transmission på refusjon #2 når behandling av refusjon #1 kaster exception") {
            val exceptionRefusjonUtfallId = UUID.randomUUID()
            val exceptionKobling = refusjon.copy(refusjonUtfallId = exceptionRefusjonUtfallId)

            every { repository.hentRefusjonKlareForBehandling() } returns
                listOf(exceptionKobling, refusjon)
            every {
                sykepengerDialogportenService.opprettTransmissionForRefusjon(
                    refusjonUtfallId = exceptionRefusjonUtfallId,
                    sykmeldingId = exceptionKobling.sykmeldingId,
                    inntektsmeldingId = exceptionKobling.inntektsmeldingId,
                    orgnr = exceptionKobling.orgnr,
                    fom = exceptionKobling.fom,
                    tom = exceptionKobling.tom,
                )
            } throws NotFoundException("Fant ikke dialog for refusjon")

            refusjonJobb.doJob()

            verify(exactly = 1) { repository.hentRefusjonKlareForBehandling() }
            verify(exactly = 2) { sykepengerDialogportenService.oppdaterDialogMedRefusjon(any()) }
            verify(exactly = 0) { repository.settRefusjonJobbTilBehandlet(exceptionRefusjonUtfallId) }
            verify(exactly = 1) { repository.settRefusjonJobbTilBehandlet(refusjon.refusjonUtfallId) }
        }
    })
