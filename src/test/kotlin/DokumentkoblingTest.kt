import dokumentkobling.InnsendingType
import dokumentkobling.Status
import dokumentkobling.VedtaksperiodeSoeknadKobling
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.ForespoerselStatus
import no.nav.helsearbeidsgiver.database.ForespoerselTable
import no.nav.helsearbeidsgiver.database.InntektsmeldingStatus
import no.nav.helsearbeidsgiver.database.InntektsmeldingTable
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable
import no.nav.helsearbeidsgiver.database.VedtaksperiodeSoeknadTable
import java.time.LocalDateTime
import java.util.UUID

class DokumentkoblingTest :
    FunSpecWithDb(
        listOf(
            SykepengesoeknadTable,
            SykmeldingTable,
            VedtaksperiodeSoeknadTable,
            ForespoerselTable,
            InntektsmeldingTable,
        ),
        { db ->
            val maksAntallPerHenting = 10

            val repository = DokumentkoblingRepository(db = db, maksAntallPerHenting = maksAntallPerHenting)

            test("opprette og hente sykmelding") {
                val sykmelding = DokumentKoblingMockUtils.sykmelding
                repository.opprettSykmelding(sykmelding)
                val hentet = repository.hentSykmeldingEntitet(sykmelding.sykmeldingId)
                hentet.shouldNotBeNull()
                hentet.id.value shouldBe sykmelding.sykmeldingId
                hentet.status shouldBe Status.MOTTATT
            }

            test("opprette og hente sykepengesoeknad koblet til sykmelding") {
                val sykmelding = DokumentKoblingMockUtils.sykmelding
                val soeknad = DokumentKoblingMockUtils.soeknad
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykepengesoeknad(soeknad)

                val hentetSoeknad = hentSykepengesoeknad(db = db, soeknadId = soeknad.soeknadId)

                hentetSoeknad.shouldNotBeNull()
                hentetSoeknad.id.value shouldBe soeknad.soeknadId
                hentetSoeknad.status shouldBe Status.MOTTATT
                hentetSoeknad.sykmeldingId shouldBe sykmelding.sykmeldingId
            }

            test("hente mottatte sykmeldinger") {
                val sykmelding = DokumentKoblingMockUtils.sykmelding
                val sykmeldingId2 = UUID.randomUUID()
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))

                val hentet = repository.henteSykemeldingerMedStatusMottatt()

                hentet.size shouldBe 2
                hentet[0].sykmeldingId shouldBe sykmelding.sykmeldingId
                hentet[1].sykmeldingId shouldBe sykmeldingId2
            }

            test("hente mottatte søknader") {
                val soeknad = DokumentKoblingMockUtils.soeknad
                val soeknadId2 = UUID.randomUUID()
                repository.opprettSykepengesoeknad(soeknad)
                repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))

                val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
                hentet.size shouldBe 2
                hentet[0].soeknadId shouldBe soeknad.soeknadId
                hentet[1].soeknadId shouldBe soeknadId2
            }

            test("henter kun de maksAntallPerHenting eldste sykmeldingene med mottatt status") {
                val sykmeldinger =
                    List(maksAntallPerHenting + 1) {
                        DokumentKoblingMockUtils.sykmelding.copy(sykmeldingId = UUID.randomUUID())
                    }

                sykmeldinger.forEach { repository.opprettSykmelding(it) }

                val hentet = repository.henteSykemeldingerMedStatusMottatt()

                assertSoftly(hentet) {
                    size shouldBe maksAntallPerHenting
                    map { it.sykmeldingId }.shouldNotContain(sykmeldinger.last().sykmeldingId)
                }
            }

            test("henter kun de maksAntallPerHenting eldste søknadene med mottatt status") {
                val soeknader =
                    List(maksAntallPerHenting + 1) {
                        DokumentKoblingMockUtils.soeknad.copy(soeknadId = UUID.randomUUID())
                    }

                soeknader.forEach { repository.opprettSykepengesoeknad(it) }

                val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()

                assertSoftly(hentet) {
                    size shouldBe maksAntallPerHenting
                    map { it.soeknadId }.shouldNotContain(soeknader.last().soeknadId)
                }
            }

            test("henter kun de maksAntallPerHenting eldste forespørsel-sykmelding-koblingene") {
                val forespoersler =
                    List(maksAntallPerHenting + 1) {
                        DokumentKoblingMockUtils.forespoerselSendt.copy(
                            forespoerselId = UUID.randomUUID(),
                            vedtaksperiodeId = UUID.randomUUID(),
                        )
                    }

                val vedtaksperiodeSoeknadKobling =
                    forespoersler.map {
                        DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling.copy(
                            vedtaksperiodeId = it.vedtaksperiodeId,
                            soeknadId = UUID.randomUUID(),
                        )
                    }

                val soeknader =
                    vedtaksperiodeSoeknadKobling.map {
                        DokumentKoblingMockUtils.soeknad.copy(
                            soeknadId = it.soeknadId,
                            sykmeldingId = UUID.randomUUID(),
                        )
                    }

                val sykmeldinger =
                    soeknader.map {
                        DokumentKoblingMockUtils.sykmelding.copy(
                            sykmeldingId = it.sykmeldingId,
                        )
                    }

                forespoersler.forEach { repository.opprettForespoerselSendt(it) }
                vedtaksperiodeSoeknadKobling.forEach { repository.opprettVedtaksperiodeSoeknadKobling(it) }
                soeknader.forEach { repository.opprettSykepengesoeknad(it) }
                sykmeldinger.forEach { repository.opprettSykmelding(it) }

                val hentet = repository.hentForespoerselSykmeldingKoblinger()

                assertSoftly(hentet) {
                    size shouldBe maksAntallPerHenting
                    map { it.forespoerselId }.shouldNotContain(forespoersler.last().forespoerselId)
                }
            }

            test("oppdatere sykmeldinger til behandlet") {
                val sykmelding = DokumentKoblingMockUtils.sykmelding
                val sykmeldingId2 = UUID.randomUUID()
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))
                repository.settSykmeldingJobbTilBehandlet(sykmelding.sykmeldingId)

                val hentet = repository.henteSykemeldingerMedStatusMottatt()
                hentet.size shouldBe 1
                hentet[0].sykmeldingId shouldBe sykmeldingId2
            }

            test("oppdatere søknader til behandlet") {
                val soeknad = DokumentKoblingMockUtils.soeknad
                val soeknadId2 = UUID.randomUUID()
                repository.opprettSykepengesoeknad(soeknad)
                repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))
                repository.settSykepengeSoeknadJobbTilBehandlet(soeknad.soeknadId)

                val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
                hentet.size shouldBe 1
                hentet[0].soeknadId shouldBe soeknadId2
            }

            test("bare mottatte sykmeldinger før tidsavbruddgrense blir satt til tidsavbrutt") {
                // Opprett to sykmeldinger, en gammel og en ny på hver sin side av tidsavbruddsgrensen
                val gammelSykmelding = DokumentKoblingMockUtils.sykmelding
                val nySykmelding = DokumentKoblingMockUtils.sykmelding.copy(sykmeldingId = UUID.randomUUID())
                repository.opprettSykmelding(gammelSykmelding)
                val tidsavbruddgrense = LocalDateTime.now()
                repository.opprettSykmelding(nySykmelding)

                // Sjekk at begge sykmeldingene er mottatt før vi setter til tidsavbrutt
                val mottatteSykmeldingerFoer = repository.henteSykemeldingerMedStatusMottatt()
                mottatteSykmeldingerFoer shouldBe listOf(gammelSykmelding, nySykmelding)

                // Sett sykmeldinger til tidsavbrutt før grensen
                val antallOppdatert =
                    repository.settSykmeldingerMedStatusMottattTilTidsavbrutt(tidsavbruddgrense = tidsavbruddgrense)

                // Sjekk at kun den gamle sykmeldingen er satt til tidsavbrutt
                antallOppdatert shouldBe 1
                val mottatteSykmeldingerEtter = repository.henteSykemeldingerMedStatusMottatt()
                mottatteSykmeldingerEtter shouldBe listOf(nySykmelding)

                // Sjekk at den gamle sykmeldingen faktisk har status TIDSAVBRUTT
                val tidsavbruttSykmelding = hentSykmelding(db = db, sykmeldingId = gammelSykmelding.sykmeldingId)
                tidsavbruttSykmelding.shouldNotBeNull().status shouldBe Status.TIDSAVBRUTT
            }

            test("bare mottatte søknader før tidsavbruddgrense blir satt til tidsavbrutt") {
                // Opprett to søknader, en gammel og en ny på hver sin side av tidsavbruddsgrensen
                val gammelSoeknad = DokumentKoblingMockUtils.soeknad
                val nySoeknad = DokumentKoblingMockUtils.soeknad.copy(soeknadId = UUID.randomUUID())
                repository.opprettSykepengesoeknad(gammelSoeknad)
                val tidsavbruddgrense = LocalDateTime.now()
                repository.opprettSykepengesoeknad(nySoeknad)

                // Sjekk at begge søknadene er mottatt før vi setter til tidsavbrutt
                val mottatteSoeknaderFoer = repository.henteSykepengeSoeknaderMedStatusMottatt()
                mottatteSoeknaderFoer shouldBe listOf(gammelSoeknad, nySoeknad)

                // Sett søknader til tidsavbrutt før grensen
                val antallOppdatert =
                    repository.settSykepengeSoeknaderMedStatusMottattTilTidsavbrutt(tidsavbruddgrense = tidsavbruddgrense)

                // Sjekk at kun den gamle søknaden er satt til tidsavbrutt
                antallOppdatert shouldBe 1
                val mottatteSoeknaderEtter = repository.henteSykepengeSoeknaderMedStatusMottatt()
                mottatteSoeknaderEtter shouldBe listOf(nySoeknad)

                // Sjekk at den gamle søknaden faktisk har status TIDSAVBRUTT
                val tidsavbruttSoeknad = hentSykepengesoeknad(db = db, soeknadId = gammelSoeknad.soeknadId)
                tidsavbruttSoeknad.shouldNotBeNull().status shouldBe Status.TIDSAVBRUTT
            }

            test("bare mottatte forespørsler før tidsavbruddgrense blir satt til tidsavbrutt") {
                // Opprett to forespørsler, en gammel og en ny på hver sin side av tidsavbruddsgrensen
                val gammelForespoersel = DokumentKoblingMockUtils.forespoerselSendt
                val nyForespoersel =
                    DokumentKoblingMockUtils.forespoerselSendt.copy(
                        forespoerselId = UUID.randomUUID(),
                        vedtaksperiodeId = UUID.randomUUID(),
                    )
                repository.opprettForespoerselSendt(gammelForespoersel)
                val tidsavbruddgrense = LocalDateTime.now()
                repository.opprettForespoerselSendt(nyForespoersel)

                // Sjekk at begge forespørslene er mottatt før vi setter til tidsavbrutt
                val mottatteForespoerslerFoer = hentForespoerslerMedStatusMottattEldstFoerst(db = db)
                mottatteForespoerslerFoer.size shouldBe 2
                mottatteForespoerslerFoer[0].forespoerselId shouldBe gammelForespoersel.forespoerselId
                mottatteForespoerslerFoer[1].forespoerselId shouldBe nyForespoersel.forespoerselId

                // Sett forespørsler til tidsavbrutt før grensen
                val antallOppdatert =
                    repository.settForespoerslerMedStatusMottattTilTidsavbrutt(tidsavbruddgrense = tidsavbruddgrense)

                // Sjekk at kun den gamle forespørselen er satt til tidsavbrutt
                antallOppdatert shouldBe 1
                val mottatteForespoerslerEtter = hentForespoerslerMedStatusMottattEldstFoerst(db = db)
                mottatteForespoerslerEtter.size shouldBe 1
                mottatteForespoerslerEtter[0].forespoerselId shouldBe nyForespoersel.forespoerselId

                // Sjekk at den gamle forespørselen faktisk har status TIDSAVBRUTT
                val tidsavbruttForespoersel = hentForespoersel(db = db, forespoerselId = gammelForespoersel.forespoerselId)
                tidsavbruttForespoersel.shouldNotBeNull().status shouldBe Status.TIDSAVBRUTT
            }

            test("bare mottatte inntektsmeldinger før tidsavbruddgrense blir satt til tidsavbrutt") {
                // Opprett to inntektsmeldinger, en gammel og en ny på hver sin side av tidsavbruddsgrensen
                val gammelInntektsmelding = DokumentKoblingMockUtils.inntektsmeldingGodkjent
                val nyInntektsmelding =
                    DokumentKoblingMockUtils.inntektsmeldingGodkjent.copy(
                        inntektsmeldingId = UUID.randomUUID(),
                        forespoerselId = UUID.randomUUID(),
                        vedtaksperiodeId = UUID.randomUUID(),
                    )
                repository.opprettInntektmeldingGodkjent(gammelInntektsmelding)
                val tidsavbruddgrense = LocalDateTime.now()
                repository.opprettInntektmeldingGodkjent(nyInntektsmelding)

                // Sjekk at begge inntektsmeldingene er mottatt før vi setter til tidsavbrutt
                val mottatteInnteksmeldingerFoer = hentInntektsmeldingerMedStatusMottatt(db = db)
                mottatteInnteksmeldingerFoer.size shouldBe 2
                mottatteInnteksmeldingerFoer[0].id.value shouldBe gammelInntektsmelding.inntektsmeldingId
                mottatteInnteksmeldingerFoer[1].id.value shouldBe nyInntektsmelding.inntektsmeldingId

                // Sett inntektsmeldinger til tidsavbrutt før grensen
                val antallOppdatert =
                    repository.settInntektsmeldingerMedStatusMottattTilTidsavbrutt(tidsavbruddgrense = tidsavbruddgrense)

                // Sjekk at kun den gamle inntektsmeldingen er satt til tidsavbrutt
                antallOppdatert shouldBe 1
                val mottatteInntektsmeldingerEtter = hentInntektsmeldingerMedStatusMottatt(db = db)
                mottatteInntektsmeldingerEtter.size shouldBe 1
                mottatteInntektsmeldingerEtter[0].id.value shouldBe nyInntektsmelding.inntektsmeldingId

                // Sjekk at den gamle inntektsmeldingen faktisk har status TIDSAVBRUTT
                val tidsavbruttInntektsmelding =
                    hentInntektsmelding(db = db, inntektsmeldingId = gammelInntektsmelding.inntektsmeldingId)
                tidsavbruttInntektsmelding.shouldNotBeNull().status shouldBe Status.TIDSAVBRUTT
            }

            test("opprette vedtaksperiode soeknad kobling") {
                val vedtaksperiodeSoeknad = DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling
                val soeknadId2 = UUID.randomUUID()
                hentListeAvSoeknadIdForVedtaksperiodeId(
                    db = db,
                    vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId,
                ) shouldBe emptyList()
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad.copy(soeknadId = soeknadId2))
                val hentet =
                    hentListeAvSoeknadIdForVedtaksperiodeId(
                        db = db,
                        vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId,
                    )
                hentet.size shouldBe 2
                hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId, soeknadId2)
            }

            test("håndtere vedtaksperiode soeknad kobling som finnes fra før uten å oppdatere opprettettidspunktet") {
                val vedtaksperiodeSoeknad = DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling
                hentListeAvSoeknadIdForVedtaksperiodeId(
                    db = db,
                    vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId,
                ) shouldBe emptyList()
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

                val opprettetFoer =
                    hentSoeknaderForVedtaksperiodeId(db = db, vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId)
                        .first()
                        .opprettet

                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

                val opprettetEtter =
                    hentSoeknaderForVedtaksperiodeId(db = db, vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId)
                        .first()
                        .opprettet
                val hentet =
                    hentListeAvSoeknadIdForVedtaksperiodeId(
                        db = db,
                        vedtaksperiodeId = vedtaksperiodeSoeknad.vedtaksperiodeId,
                    )

                hentet.size shouldBe 1
                hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId)
                opprettetFoer shouldBe opprettetEtter
            }

            test("opprette forespoersel sendt og utgaatt") {
                val forespoerselSendt = DokumentKoblingMockUtils.forespoerselSendt
                val forespoerselUtgaatt = DokumentKoblingMockUtils.forespoerselUtgaatt

                hentForespoerslerMedStatusMottattEldstFoerst(db = db) shouldBe emptyList()

                repository.opprettForespoerselSendt(forespoerselSendt)

                val hentet = hentForespoerslerMedStatusMottattEldstFoerst(db = db)
                hentet.size shouldBe 1
                hentet[0].forespoerselId shouldBe forespoerselSendt.forespoerselId
                hentet[0].vedtaksperiodeId shouldBe forespoerselSendt.vedtaksperiodeId
                hentet[0].status shouldBe Status.MOTTATT
                hentet[0].forespoerselStatus shouldBe ForespoerselStatus.SENDT

                repository.opprettForespoerselUtgaatt(forespoerselUtgaatt)
                val hentetEtterUtgaatt = hentForespoerslerMedStatusMottattEldstFoerst(db = db)
                hentetEtterUtgaatt.size shouldBe 2
                hentetEtterUtgaatt[0].forespoerselId shouldBe forespoerselSendt.forespoerselId
                hentetEtterUtgaatt[1].forespoerselId shouldBe forespoerselUtgaatt.forespoerselId
                hentetEtterUtgaatt[1].forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }

            fun opprettDokumentkoblinger() {
                repository.opprettSykmelding(DokumentKoblingMockUtils.sykmelding)
                repository.opprettSykepengesoeknad(DokumentKoblingMockUtils.soeknad)
                repository.opprettVedtaksperiodeSoeknadKobling(DokumentKoblingMockUtils.vedtaksperiodeSoeknadKobling)
                repository.opprettForespoerselSendt(DokumentKoblingMockUtils.forespoerselSendt)
                repository.opprettForespoerselUtgaatt(DokumentKoblingMockUtils.forespoerselUtgaatt)
            }

            test("hentForespoerselSykmeldingKoblinger() returner riktig når jobber er behandlet") {
                opprettDokumentkoblinger()
                DokumentKoblingMockUtils.soeknad.let {
                    repository.settSykmeldingJobbTilBehandlet(sykmeldingId = it.sykmeldingId)
                    repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId = it.soeknadId)
                }

                val hentet = repository.hentForespoerselSykmeldingKoblinger()

                hentet.shouldNotBeEmpty()
                assertSoftly(hentet[0]) {
                    sykmeldingId shouldBe DokumentKoblingMockUtils.sykmelding.sykmeldingId
                    soeknadId shouldBe DokumentKoblingMockUtils.soeknad.soeknadId
                    forespoerselId shouldBe DokumentKoblingMockUtils.forespoerselSendt.forespoerselId
                    sykmeldingStatus shouldBe Status.BEHANDLET
                    soeknadStatus shouldBe Status.BEHANDLET
                    forespoerselStatus shouldBe ForespoerselStatus.SENDT
                }
                hentet[1].forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }

            context("hentForespoerselSykmeldingKoblinger() returner med riktig jobb status") {
                test("når bare sykmelding er behandlet") {
                    opprettDokumentkoblinger()
                    repository.settSykmeldingJobbTilBehandlet(sykmeldingId = DokumentKoblingMockUtils.soeknad.sykmeldingId)
                    val hentet = repository.hentForespoerselSykmeldingKoblinger()
                    hentet.shouldNotBeEmpty()
                    assertSoftly(hentet[0]) {
                        sykmeldingStatus shouldBe Status.BEHANDLET
                        soeknadStatus shouldBe Status.MOTTATT
                    }
                }

                test("når bare søknad er behandlet") {
                    opprettDokumentkoblinger()
                    repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId = DokumentKoblingMockUtils.soeknad.soeknadId)
                    val hentet = repository.hentForespoerselSykmeldingKoblinger()
                    hentet.shouldNotBeEmpty()
                    assertSoftly(hentet[0]) {
                        sykmeldingStatus shouldBe Status.MOTTATT
                        soeknadStatus shouldBe Status.BEHANDLET
                    }
                }
            }

            test("hentForespoerselSykmeldingKoblinger() returnerer ett resultat per sykmelding koblet til forespørsel") {
                val vedtaksperiodeId = UUID.randomUUID()
                val forespoerselId = UUID.randomUUID()

                // Opprett første sykmelding og søknad
                val sykmeldingId1 = UUID.randomUUID()
                val soeknadId1 = UUID.randomUUID()
                repository.opprettSykmelding(DokumentKoblingMockUtils.sykmelding.copy(sykmeldingId = sykmeldingId1))
                repository.opprettSykepengesoeknad(
                    DokumentKoblingMockUtils.soeknad.copy(
                        sykmeldingId = sykmeldingId1,
                        soeknadId = soeknadId1,
                    ),
                )
                repository.opprettVedtaksperiodeSoeknadKobling(
                    VedtaksperiodeSoeknadKobling(
                        vedtaksperiodeId,
                        soeknadId1,
                    ),
                )

                // Opprett andre sykmelding og søknad koblet til samme vedtaksperiode
                val sykmeldingId2 = UUID.randomUUID()
                val soeknadId2 = UUID.randomUUID()
                repository.opprettSykmelding(DokumentKoblingMockUtils.sykmelding.copy(sykmeldingId = sykmeldingId2))
                repository.opprettSykepengesoeknad(
                    DokumentKoblingMockUtils.soeknad.copy(
                        sykmeldingId = sykmeldingId2,
                        soeknadId = soeknadId2,
                    ),
                )
                repository.opprettVedtaksperiodeSoeknadKobling(
                    VedtaksperiodeSoeknadKobling(
                        vedtaksperiodeId,
                        soeknadId2,
                    ),
                )

                // Opprett én forespørsel koblet til vedtaksperioden
                repository.opprettForespoerselSendt(
                    DokumentKoblingMockUtils.forespoerselSendt.copy(
                        forespoerselId = forespoerselId,
                        vedtaksperiodeId = vedtaksperiodeId,
                    ),
                )

                val hentet = repository.hentForespoerselSykmeldingKoblinger()

                // Forventer 2 resultater: én per sykmelding koblet til samme forespørsel
                hentet.size shouldBe 2
                hentet[0].forespoerselId shouldBe forespoerselId
                hentet[1].forespoerselId shouldBe forespoerselId
                hentet[0].sykmeldingId shouldBe sykmeldingId1
                hentet[1].sykmeldingId shouldBe sykmeldingId2
            }

            test("opprette inntektsmelding godkjent") {
                val inntektsmeldingGodkjent = DokumentKoblingMockUtils.inntektsmeldingGodkjent
                hentInntektsmeldingerMedStatusMottatt(db = db) shouldBe emptyList()

                repository.opprettInntektmeldingGodkjent(inntektsmeldingGodkjent)

                val hentet = hentInntektsmeldingerMedStatusMottatt(db = db)
                hentet.shouldNotBeEmpty()
                assertSoftly(hentet[0]) {
                    forespoerselId shouldBe inntektsmeldingGodkjent.forespoerselId
                    vedtaksperiodeId shouldBe inntektsmeldingGodkjent.vedtaksperiodeId
                    status shouldBe Status.MOTTATT
                    inntektsmeldingStatus shouldBe InntektsmeldingStatus.GODKJENT
                    innsendingType shouldBe InnsendingType.FORESPURT_EKSTERN
                }
            }

            test("opprette inntektsmelding avvist") {
                val inntektsmeldingAvvist = DokumentKoblingMockUtils.inntektsmeldingAvvist
                hentInntektsmeldingerMedStatusMottatt(db = db) shouldBe emptyList()

                repository.opprettInntektmeldingAvvist(inntektsmeldingAvvist)

                val hentet = hentInntektsmeldingerMedStatusMottatt(db = db)
                hentet.shouldNotBeEmpty()
                assertSoftly(hentet[0]) {
                    forespoerselId shouldBe inntektsmeldingAvvist.forespoerselId
                    vedtaksperiodeId shouldBe inntektsmeldingAvvist.vedtaksperiodeId
                    status shouldBe Status.MOTTATT
                    inntektsmeldingStatus shouldBe InntektsmeldingStatus.AVVIST
                    innsendingType shouldBe InnsendingType.FORESPURT_EKSTERN
                }
            }
        },
    )
