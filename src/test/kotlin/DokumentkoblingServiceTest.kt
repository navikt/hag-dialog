package dokumentkobling

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.ForespoerselSykmeldingKobling
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import java.time.LocalDateTime
import java.util.UUID

class DokumentkoblingServiceTest :
    FunSpec({
        beforeTest {
            clearAllMocks()
        }

        val dokumentkoblingRepository = mockk<DokumentkoblingRepository>(relaxed = true)
        val dokumentkoblingService = DokumentkoblingService(dokumentkoblingRepository)

        fun lagKobling(
            forespoerselId: UUID = UUID.randomUUID(),
            forespoerselStatus: ForespoerselStatus = ForespoerselStatus.SENDT,
            forespoerselOpprettet: LocalDateTime = LocalDateTime.now(),
            vedtaksperiodeId: UUID = UUID.randomUUID(),
            soeknadId: UUID = UUID.randomUUID(),
            sykmeldingId: UUID = UUID.randomUUID(),
            sykmeldingOpprettet: LocalDateTime = LocalDateTime.now(),
            sykmeldingStatus: Status = Status.BEHANDLET,
            soeknadStatus: Status = Status.BEHANDLET,
        ) = ForespoerselSykmeldingKobling(
            forespoerselId = forespoerselId,
            forespoerselStatus = forespoerselStatus,
            forespoerselOpprettet = forespoerselOpprettet,
            vedtaksperiodeId = vedtaksperiodeId,
            soeknadId = soeknadId,
            sykmeldingId = sykmeldingId,
            sykmeldingOpprettet = sykmeldingOpprettet,
            sykmeldingStatus = sykmeldingStatus,
            soeknadStatus = soeknadStatus,
        )

        test("hentForespoerslerKlarForBehandling skal kun returnere forespørsler hvor sykmelding og søknad er behandlet") {
            val forespoerselId = UUID.randomUUID()
            val koblinger =
                listOf(
                    lagKobling(sykmeldingStatus = Status.MOTTATT),
                    lagKobling(forespoerselId = forespoerselId),
                    lagKobling(soeknadStatus = Status.MOTTATT),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger
            val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 1
            forespoersler.first().forespoerselId shouldBe forespoerselId
        }

        test("hentForespoerslerKlarForBehandling skal velge nyeste sykmelding per forespørsel") {
            val nySykmeldingKobling = lagKobling()
            val gammelSykmeldingKobling =
                nySykmeldingKobling.copy(
                    sykmeldingId = UUID.randomUUID(),
                    sykmeldingOpprettet = LocalDateTime.now().minusDays(3),
                )

            val koblinger = listOf(nySykmeldingKobling, gammelSykmeldingKobling)
            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger
            val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 1
            forespoersler.first().sykmeldingId shouldBe nySykmeldingKobling.sykmeldingId
        }

        test("hentForespoerslerKlarForBehandling skal beholde kobling hvis forespørselStatus er ulik") {
            val kobling = lagKobling(forespoerselStatus = ForespoerselStatus.SENDT)
            val koblingUtgaatt = kobling.copy(forespoerselStatus = ForespoerselStatus.UTGAATT)

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns
                listOf(
                    kobling,
                    koblingUtgaatt,
                )
            val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 2

            assertSoftly(forespoersler) {
                it shouldHaveSize 2
                first().sykmeldingId shouldBe kobling.sykmeldingId
                first().forespoerselStatus shouldBe ForespoerselStatus.SENDT

                last().sykmeldingId shouldBe kobling.sykmeldingId
                last().forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }
        }

        test("hentForespoerslerKlarForBehandling skal returnere tom liste når ingen koblinger er klare") {
            val koblinger =
                listOf(
                    lagKobling(
                        sykmeldingStatus = Status.MOTTATT,
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger
            val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

            forespoersler.shouldBeEmpty()
        }

        test("hentForespoerslerKlarForBehandling skal returnere sortert liste etter forespoerselOpprettet") {
            val antallKoblinger = 100
            val koblingerTilfeldigSortert = List(antallKoblinger) { lagKobling() }.sortedBy { it.forespoerselId }

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblingerTilfeldigSortert
            val forespoersler = dokumentkoblingService.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize antallKoblinger
            forespoersler shouldBe koblingerTilfeldigSortert.sortedBy { it.forespoerselOpprettet }
        }

        test("lagreInntkettsmeldingGodkjent kaller repository") {
            dokumentkoblingService.lagreInntektsmeldingGodkjent(DokumentKoblingMockUtils.inntektsmeldingGodkjent)
            verify { dokumentkoblingRepository.opprettInntektmeldingGodkjent(DokumentKoblingMockUtils.inntektsmeldingGodkjent) }
        }
        test("lagreInntkettsmeldingAvvist kaller repository") {
            dokumentkoblingService.lagreInntektsmeldingAvvist(DokumentKoblingMockUtils.inntektsmeldingAvvist)
            verify { dokumentkoblingRepository.opprettInntektmeldingAvvist(DokumentKoblingMockUtils.inntektsmeldingAvvist) }
        }
    })
