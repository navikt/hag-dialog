import dokumentkobling.DokumentkoblingService
import dokumentkobling.Status
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifySequence
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.dokumentkobling.ForespoerselJobb
import java.time.LocalDateTime
import java.util.UUID

class ForespoerselJobbTest :
    FunSpec({
        val repository = mockk<DokumentkoblingRepository>()
        val dialogportenService = mockk<DialogportenService>(relaxed = true)
        val dokumentKoblingService = mockk<DokumentkoblingService>(relaxed = true)

        val forespoerselJobb =
            ForespoerselJobb(
                dokumentkoblingRepository = repository,
                dokumentkoblingService = dokumentKoblingService,
                dialogportenService = dialogportenService,
            )

        val kobling = lagKobling()

        val koblingMedSammeVedtaksperiodeId =
            kobling.copy(forespoerselId = UUID.randomUUID(), soeknadId = UUID.randomUUID())

        val koblingForEnHeltAnnenSak = lagKobling()

        beforeTest {
            clearAllMocks()
            every { repository.hentForespoerselSykmeldingKoblinger() } returns emptyList()
            every { dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(any(), any()) } just runs
            every { repository.settForespoerselJobbTilBehandlet(any()) } just runs
        }

        test("forespoerselJobb skal opprette transmissions for forespørsler og sette status til BEHANDLET") {
            every { repository.hentForespoerselSykmeldingKoblinger() } returns
                listOf(
                    kobling,
                    koblingMedSammeVedtaksperiodeId,
                    koblingForEnHeltAnnenSak,
                )

            forespoerselJobb.doJob()

            verifySequence {
                repository.hentForespoerselSykmeldingKoblinger()

                listOf(kobling, koblingMedSammeVedtaksperiodeId, koblingForEnHeltAnnenSak).forEach {
                    dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                        it.forespoerselId,
                        it.sykmeldingId,
                    )
                    repository.settForespoerselJobbTilBehandlet(it.forespoerselId)
                }
            }
        }

        test("forespoerselJobb skal ikke behandle forespørsler når den ikke finner noen som er klare for behandling") {
            forespoerselJobb.doJob()

            verify(exactly = 1) { repository.hentForespoerselSykmeldingKoblinger() }
            verify(exactly = 0) { dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(any(), any()) }
            verify(exactly = 0) { repository.settForespoerselJobbTilBehandlet(any()) }
        }

        test(
            "forespoerselJobb skal fortsette å behandle forespørsler for andre vedtaksperioder når en av forespørslene for én vedtaksperiode feiler",
        ) {
            every { repository.hentForespoerselSykmeldingKoblinger() } returns
                listOf(
                    kobling,
                    koblingMedSammeVedtaksperiodeId,
                    koblingForEnHeltAnnenSak,
                )

            every {
                dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                    kobling.forespoerselId,
                    kobling.sykmeldingId,
                )
            } throws RuntimeException("Feil ved behandling")

            forespoerselJobb.doJob()

            verify(exactly = 1) { repository.hentForespoerselSykmeldingKoblinger() }

            // Verifiser at vi forsøkte å behandle den første forespørselen
            verify(exactly = 1) {
                kobling.let {
                    dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                        it.forespoerselId,
                        it.sykmeldingId,
                    )
                }
            }

            // Verifiser at vi ikke forsøkte å behandle den andre forespørselen med samme vedtaksperiodeId
            verify(exactly = 0) {
                koblingMedSammeVedtaksperiodeId.let {
                    dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                        it.forespoerselId,
                        it.sykmeldingId,
                    )
                }
            }

            // Verifiser at vi forsøkte å behandle den tredje forespørselen, som tilhører en annen sak
            verify(exactly = 1) {
                koblingForEnHeltAnnenSak.let {
                    dialogportenService.oppdaterDialogMedInntektsmeldingsforespoersel(
                        it.forespoerselId,
                        it.sykmeldingId,
                    )
                }
            }

            // Verifiser at vi kun satte den tredje forespørselen til BEHANDLET
            verify(exactly = 0) {
                repository.settForespoerselJobbTilBehandlet(kobling.forespoerselId)
                repository.settForespoerselJobbTilBehandlet(koblingMedSammeVedtaksperiodeId.forespoerselId)
            }
            verify(exactly = 1) {
                repository.settForespoerselJobbTilBehandlet(koblingForEnHeltAnnenSak.forespoerselId)
            }
        }
    })

fun lagKobling(): DokumentkoblingRepository.ForespoerselSykmeldingKobling =
    DokumentkoblingRepository.ForespoerselSykmeldingKobling(
        forespoerselId = UUID.randomUUID(),
        forespoerselStatus = ForespoerselStatus.SENDT,
        vedtaksperiodeId = UUID.randomUUID(),
        soeknadId = UUID.randomUUID(),
        sykmeldingId = UUID.randomUUID(),
        sykmeldingOpprettet = LocalDateTime.now(),
        sykmeldingStatus = Status.BEHANDLET,
        soeknadStatus = Status.BEHANDLET,
    )
