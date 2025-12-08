package dokumentkobling

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
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

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 1
            forespoersler.first().forespoerselId shouldBe forespoerselId
        }

        test("hentForespoerslerKlarForBehandling skal velge nyeste sykmelding per forespørsel") {
            val forespoerselId = UUID.randomUUID()
            val vedtaksperiodeId = UUID.randomUUID()
            val gammelSykmeldingId = UUID.randomUUID()
            val nySykmeldingId = UUID.randomUUID()

            val koblinger =
                listOf(
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.SENDT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = gammelSykmeldingId,
                        sykmeldingOpprettet = LocalDateTime.now().minusDays(3),
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.SENDT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = nySykmeldingId,
                        sykmeldingOpprettet = LocalDateTime.now(),
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 1
            forespoersler.first().sykmeldingId shouldBe nySykmeldingId
        }

        test("hentForespoerslerKlarForBehandling skal beholde kobling hvis forespørselStatus er ulik") {
            val kobling = lagKobling(forespoerselStatus = ForespoerselStatus.SENDT)

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns
                listOf(
                    kobling,
                    kobling.copy(forespoerselStatus = ForespoerselStatus.UTGAATT),
                )

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

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

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler.shouldBeEmpty()
        }

        test("hentForespoerslerKlarForBehandling skal returnere sortert liste etter forespoerselOpprettet") {
            val antallKoblinger = 100
            val koblingerTilfeldigSortert = List(antallKoblinger) { lagKobling() }.sortedBy { it.forespoerselId }

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblingerTilfeldigSortert

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize antallKoblinger
            forespoersler shouldBe koblingerTilfeldigSortert.sortedBy { it.forespoerselOpprettet }
        }
    })
