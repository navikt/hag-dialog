package dokumentkobling

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
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

        test("hentForespoerslerKlarForBehandling skal velge nyeste sykmelding per forespørsel dersom de har samme forespoerselStatus") {
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

        test(
            "1hentForespoerslerKlarForBehandling skal beholde forespørsel-sykmeldingkobling med samme forespørselId og nyeste sykmeldingOpprettet hvis forespørselStatus er ulik",
        ) {
            val forespoerselId = UUID.randomUUID()
            val vedtaksperiodeId = UUID.randomUUID()

            val sykmeldingOpprettet = LocalDateTime.now().minusDays(3)

            val sykmeldingId = UUID.randomUUID()

            val koblinger =
                listOf(
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.SENDT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = sykmeldingId,
                        sykmeldingOpprettet = sykmeldingOpprettet,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.UTGAATT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = sykmeldingId,
                        sykmeldingOpprettet = sykmeldingOpprettet,
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 2

            assertSoftly(forespoersler) {
                it shouldHaveSize 2
                first().sykmeldingId shouldBe sykmeldingId
                first().forespoerselStatus shouldBe ForespoerselStatus.SENDT

                last().sykmeldingId shouldBe sykmeldingId
                last().forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }
        }

        test(
            "hentForespoerslerKlarForBehandling skal beholde forespørsel-sykmeldingkobling med samme forespørselId og nyeste sykmeldingOpprettet hvis forespørselStatus er ulik",
        ) {
            val forespoerselId = UUID.randomUUID()
            val vedtaksperiodeId = UUID.randomUUID()

            val gammelSykmeldingId = UUID.randomUUID()
            val gammelSykmeldingOpprettet = LocalDateTime.now().minusDays(3)

            val nySykmeldingId = UUID.randomUUID()
            val nySykmeldingOpprettet = LocalDateTime.now()

            val koblinger =
                listOf(
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.SENDT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = gammelSykmeldingId,
                        sykmeldingOpprettet = gammelSykmeldingOpprettet,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.UTGAATT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = gammelSykmeldingId,
                        sykmeldingOpprettet = gammelSykmeldingOpprettet,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.SENDT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = nySykmeldingId,
                        sykmeldingOpprettet = nySykmeldingOpprettet,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId,
                        forespoerselStatus = ForespoerselStatus.UTGAATT,
                        vedtaksperiodeId = vedtaksperiodeId,
                        sykmeldingId = nySykmeldingId,
                        sykmeldingOpprettet = nySykmeldingOpprettet,
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 2

            assertSoftly(forespoersler) {
                it shouldHaveSize 2
                first().sykmeldingId shouldBe nySykmeldingId
                first().forespoerselStatus shouldBe ForespoerselStatus.SENDT

                last().sykmeldingId shouldBe nySykmeldingId
                last().forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }
        }

        test("hentForespoerslerKlarForBehandling skal returnere tom liste når ingen koblinger er klare") {
            val koblinger =
                listOf(
                    lagKobling(
                        sykmeldingStatus = Status.MOTTATT,
                        soeknadStatus = Status.MOTTATT,
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler.shouldBeEmpty()
        }

        test(
            "hentForespoerslerKlarForBehandling skal håndtere flere forespørsler og velge nyeste sykmelding per forespørsel dersom de har samme forespoerselStatus",
        ) {
            val forespoerselId1 = UUID.randomUUID()
            val forespoerselId2 = UUID.randomUUID()
            val sykmeldingId1 = UUID.randomUUID()
            val nyesteSykmeldingOpprettet1 = LocalDateTime.now()
            val sykmeldingId2 = UUID.randomUUID()
            val nyesteSykmeldingOpprettet2 = LocalDateTime.now().minusDays(1)

            val koblinger =
                listOf(
                    lagKobling(
                        forespoerselId = forespoerselId1,
                        sykmeldingId = sykmeldingId1,
                        sykmeldingOpprettet = nyesteSykmeldingOpprettet1,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId1,
                        sykmeldingOpprettet = LocalDateTime.now().minusDays(3),
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId1,
                        sykmeldingOpprettet = LocalDateTime.now().minusDays(7),
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId2,
                        sykmeldingId = sykmeldingId2,
                        sykmeldingOpprettet = nyesteSykmeldingOpprettet2,
                    ),
                    lagKobling(
                        forespoerselId = forespoerselId2,
                        sykmeldingId = sykmeldingId2,
                        sykmeldingOpprettet = LocalDateTime.now().minusDays(1337),
                    ),
                )

            every { dokumentkoblingRepository.hentForespoerselSykmeldingKoblinger() } returns koblinger

            val service = DokumentkoblingService(dokumentkoblingRepository)
            val forespoersler = service.hentForespoerslerKlarForBehandling()

            forespoersler shouldHaveSize 2
            forespoersler.map { it.sykmeldingId } shouldContainExactly listOf(sykmeldingId1, sykmeldingId2)
            forespoersler.map { it.sykmeldingOpprettet } shouldContainExactly listOf(nyesteSykmeldingOpprettet1, nyesteSykmeldingOpprettet2)
        }
    })
