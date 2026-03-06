package dokumentkobling

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository.ForespoerselSykmeldingKobling
import no.nav.helsearbeidsgiver.database.ForespoerselEntity
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.database.InntektsmeldingEntity
import no.nav.helsearbeidsgiver.database.SykepengesoeknadEntity
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.database.VedtaksperiodeSoeknadEntity
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
            forespoerselJobbStatus: Status = Status.MOTTATT,
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
            forespoerselJobbStatus = forespoerselJobbStatus,
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

        test("lagreInntektsmeldingGodkjent kaller repository") {
            dokumentkoblingService.lagreInntektsmeldingGodkjent(DokumentKoblingMockUtils.inntektsmeldingGodkjent)
            verify { dokumentkoblingRepository.opprettInntektmeldingGodkjent(DokumentKoblingMockUtils.inntektsmeldingGodkjent) }
        }

        test("lagreInntektsmeldingAvvist kaller repository") {
            dokumentkoblingService.lagreInntektsmeldingAvvist(DokumentKoblingMockUtils.inntektsmeldingAvvist)
            verify { dokumentkoblingRepository.opprettInntektmeldingAvvist(DokumentKoblingMockUtils.inntektsmeldingAvvist) }
        }

        test("finn orgnr for sykmeldingId") {
            val sykmelding = DokumentKoblingMockUtils.sykmelding
            val sykmeldingEntity =
                mockk<SykmeldingEntity> {
                    every { sykmeldingId } returns sykmelding.sykmeldingId
                    every { data } returns sykmelding
                }
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns sykmeldingEntity
            dokumentkoblingService.lagreSykmelding(sykmelding)
            val orgnr = dokumentkoblingService.hentSykmeldingOrgnr(sykmelding.sykmeldingId)
            orgnr shouldBe DokumentKoblingMockUtils.orgnr
        }

        test("hentKoblingMedForespoerselId skal velge nyeste sykmelding") {
            val nySykmeldingKobling = lagKobling()
            val gammelSykmeldingKobling =
                nySykmeldingKobling.copy(
                    sykmeldingId = UUID.randomUUID(),
                    sykmeldingOpprettet = LocalDateTime.now().minusDays(3),
                )

            val koblinger = listOf(nySykmeldingKobling, gammelSykmeldingKobling)
            every { dokumentkoblingRepository.hentKoblingerMedForespoerselId(nySykmeldingKobling.forespoerselId) } returns koblinger
            val forespoerselKobling = dokumentkoblingService.hentKoblingMedForespoerselId(nySykmeldingKobling.forespoerselId)

            forespoerselKobling.shouldNotBeNull()
            forespoerselKobling.sykmeldingId shouldBe nySykmeldingKobling.sykmeldingId
        }
        test("erDuplikat returnerer false når sykmelding ikke eksisterer") {
            val sykmelding = DokumentKoblingMockUtils.sykmelding
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(sykmelding)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når sykmelding allerede eksisterer") {
            val sykmelding = DokumentKoblingMockUtils.sykmelding
            val sykmeldingEntity =
                mockk<SykmeldingEntity> {
                    every { sykmeldingId } returns sykmelding.sykmeldingId
                    every { data } returns sykmelding
                }
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns sykmeldingEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(sykmelding)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når sykepengesoeknad ikke eksisterer") {
            val soeknad = DokumentKoblingMockUtils.soeknad
            every { dokumentkoblingRepository.hentSykepengesoeknad(soeknad.soeknadId, soeknad.sykmeldingId) } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(soeknad)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når sykepengesoeknad allerede eksisterer") {
            val soeknad = DokumentKoblingMockUtils.soeknad
            val soeknadEntity = mockk<SykepengesoeknadEntity>()
            every { dokumentkoblingRepository.hentSykepengesoeknad(soeknad.soeknadId, soeknad.sykmeldingId) } returns soeknadEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(soeknad)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når vedtaksperiode-søknad kobling ikke eksisterer") {
            val kobling = DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling
            every {
                dokumentkoblingRepository.hentVedtaksperiodeSoeknadKobling(
                    kobling.vedtaksperiodeId,
                    kobling.soeknadId,
                )
            } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(kobling)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når vedtaksperiode-søknad kobling allerede eksisterer") {
            val kobling = DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling
            val koblingEntity = mockk<VedtaksperiodeSoeknadEntity>()
            every {
                dokumentkoblingRepository.hentVedtaksperiodeSoeknadKobling(
                    kobling.vedtaksperiodeId,
                    kobling.soeknadId,
                )
            } returns koblingEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(kobling)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når forespørsel sendt ikke eksisterer") {
            val forespoersel = DokumentKoblingMockUtils.forespoerselSendt
            every {
                dokumentkoblingRepository.hentForespoerselMedStatus(
                    forespoersel.forespoerselId,
                    ForespoerselStatus.SENDT,
                )
            } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(forespoersel)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når forespørsel sendt allerede eksisterer") {
            val forespoersel = DokumentKoblingMockUtils.forespoerselSendt
            val forespoerselEntity = mockk<ForespoerselEntity>()
            every {
                dokumentkoblingRepository.hentForespoerselMedStatus(
                    forespoersel.forespoerselId,
                    ForespoerselStatus.SENDT,
                )
            } returns forespoerselEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(forespoersel)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når forespørsel utgått ikke eksisterer") {
            val forespoersel = DokumentKoblingMockUtils.forespoerselUtgaatt
            every {
                dokumentkoblingRepository.hentForespoerselMedStatus(
                    forespoersel.forespoerselId,
                    ForespoerselStatus.UTGAATT,
                )
            } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(forespoersel)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når forespørsel utgått allerede eksisterer") {
            val forespoersel = DokumentKoblingMockUtils.forespoerselUtgaatt
            val forespoerselEntity = mockk<ForespoerselEntity>()
            every {
                dokumentkoblingRepository.hentForespoerselMedStatus(
                    forespoersel.forespoerselId,
                    ForespoerselStatus.UTGAATT,
                )
            } returns forespoerselEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(forespoersel)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når inntektsmelding godkjent ikke eksisterer") {
            val inntektsmelding = DokumentKoblingMockUtils.inntektsmeldingGodkjent
            every { dokumentkoblingRepository.hentInntektsmelding(inntektsmelding.inntektsmeldingId) } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(inntektsmelding)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når inntektsmelding godkjent allerede eksisterer") {
            val inntektsmelding = DokumentKoblingMockUtils.inntektsmeldingGodkjent
            val inntektsmeldingEntity = mockk<InntektsmeldingEntity>()
            every { dokumentkoblingRepository.hentInntektsmelding(inntektsmelding.inntektsmeldingId) } returns inntektsmeldingEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(inntektsmelding)

            erDuplikat shouldBe true
        }

        test("erDuplikat returnerer false når inntektsmelding avvist ikke eksisterer") {
            val inntektsmelding = DokumentKoblingMockUtils.inntektsmeldingAvvist
            every { dokumentkoblingRepository.hentInntektsmelding(inntektsmelding.inntektsmeldingId) } returns null

            val erDuplikat = dokumentkoblingService.erDuplikat(inntektsmelding)

            erDuplikat shouldBe false
        }

        test("erDuplikat returnerer true når inntektsmelding avvist allerede eksisterer") {
            val inntektsmelding = DokumentKoblingMockUtils.inntektsmeldingAvvist
            val inntektsmeldingEntity = mockk<InntektsmeldingEntity>()
            every { dokumentkoblingRepository.hentInntektsmelding(inntektsmelding.inntektsmeldingId) } returns inntektsmeldingEntity

            val erDuplikat = dokumentkoblingService.erDuplikat(inntektsmelding)

            erDuplikat shouldBe true
        }
    })
