import dokumentkobling.InnsendingType
import dokumentkobling.Status
import dokumentkobling.VedtaksperiodeSoeknadKobling
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldNotBeEmpty
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
            val repository = DokumentkoblingRepository(db)

            test("opprette og hente sykmelding") {
                val sykmelding = dokumentkoblingSykmelding
                repository.opprettSykmelding(sykmelding)
                val hentet = repository.hentSykmeldingEntitet(sykmelding.sykmeldingId)

                hentet.shouldNotBeNull()
                hentet.id.value shouldBe sykmelding.sykmeldingId
                hentet.status shouldBe Status.MOTTATT
            }

            test("opprette og hente sykepengesoeknad koblet til sykmelding") {
                val sykmelding = dokumentkoblingSykmelding
                val soeknad = dokumentkoblingSoeknad
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykepengesoeknad(soeknad)

                val hentetSoeknad = repository.hentSykepengesoeknad(soeknad.soeknadId)

                hentetSoeknad.shouldNotBeNull()
                hentetSoeknad.id.value shouldBe soeknad.soeknadId
                hentetSoeknad.status shouldBe Status.MOTTATT
                hentetSoeknad.sykmeldingId shouldBe sykmelding.sykmeldingId
            }

            test("hente mottatte sykmeldinger") {
                val sykmelding = dokumentkoblingSykmelding
                val sykmeldingId2 = UUID.randomUUID()
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))

                val hentet = repository.henteSykemeldingerMedStatusMottatt()

                hentet.size shouldBe 2
                hentet[0].sykmeldingId shouldBe sykmelding.sykmeldingId
                hentet[1].sykmeldingId shouldBe sykmeldingId2
            }

            test("hente mottatte søknader") {
                val soeknad = dokumentkoblingSoeknad
                val soeknadId2 = UUID.randomUUID()
                repository.opprettSykepengesoeknad(soeknad)
                repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))

                val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
                hentet.size shouldBe 2
                hentet[0].soeknadId shouldBe soeknad.soeknadId
                hentet[1].soeknadId shouldBe soeknadId2
            }

            test("oppdatere sykmeldinger til behandlet") {
                val sykmelding = dokumentkoblingSykmelding
                val sykmeldingId2 = UUID.randomUUID()
                repository.opprettSykmelding(sykmelding)
                repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))
                repository.settSykmeldingJobbTilBehandlet(sykmelding.sykmeldingId)

                val hentet = repository.henteSykemeldingerMedStatusMottatt()
                hentet.size shouldBe 1
                hentet[0].sykmeldingId shouldBe sykmeldingId2
            }

            test("oppdatere søknader til behandlet") {
                val soeknad = dokumentkoblingSoeknad
                val soeknadId2 = UUID.randomUUID()
                repository.opprettSykepengesoeknad(soeknad)
                repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))
                repository.settSykepengeSoeknadJobbTilBehandlet(soeknad.soeknadId)

                val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
                hentet.size shouldBe 1
                hentet[0].soeknadId shouldBe soeknadId2
            }

            test("opprette vedtaksperiode soeknad kobling") {
                val vedtaksperiodeSoeknad = dokumentkoblingVedtaksperiodeSoeknad
                val soeknadId2 = UUID.randomUUID()
                repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId) shouldBe emptyList()
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad.copy(soeknadId = soeknadId2))
                val hentet = repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)
                hentet.size shouldBe 2
                hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId, soeknadId2)
            }

            test("håndtere vedtaksperiode soeknad kobling som finnes fra før uten å oppdatere opprettettidspunktet") {
                val vedtaksperiodeSoeknad = dokumentkoblingVedtaksperiodeSoeknad
                repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId) shouldBe emptyList()
                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

                val opprettetFoer =
                    repository
                        .hentSoeknaderForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)
                        .first()
                        .opprettet

                repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

                val opprettetEtter =
                    repository
                        .hentSoeknaderForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)
                        .first()
                        .opprettet
                val hentet = repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)

                hentet.size shouldBe 1
                hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId)
                opprettetFoer shouldBe opprettetEtter
            }

            test("opprette forespoersel sendt og utgaatt") {
                val forespoerselSendt = dokumentkoblingForespoerselSendt
                val forespoerselUtgaatt = dokumentkoblingForespoerselUtgaatt

                repository.hentForespoerslerMedStatusMottattEldstFoerst() shouldBe emptyList()

                repository.opprettForespoerselSendt(forespoerselSendt)

                val hentet = repository.hentForespoerslerMedStatusMottattEldstFoerst()
                hentet.size shouldBe 1
                hentet[0].forespoerselId shouldBe forespoerselSendt.forespoerselId
                hentet[0].vedtaksperiodeId shouldBe forespoerselSendt.vedtaksperiodeId
                hentet[0].status shouldBe Status.MOTTATT
                hentet[0].forespoerselStatus shouldBe ForespoerselStatus.SENDT

                repository.opprettForespoerselUtgaatt(forespoerselUtgaatt)
                val hentetEtterUtgaatt = repository.hentForespoerslerMedStatusMottattEldstFoerst()
                hentetEtterUtgaatt.size shouldBe 2
                hentetEtterUtgaatt[0].forespoerselId shouldBe forespoerselSendt.forespoerselId
                hentetEtterUtgaatt[1].forespoerselId shouldBe forespoerselUtgaatt.forespoerselId
                hentetEtterUtgaatt[1].forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }

            fun opprettDokumentkoblinger() {
                repository.opprettSykmelding(dokumentkoblingSykmelding)
                repository.opprettSykepengesoeknad(dokumentkoblingSoeknad)
                repository.opprettVedtaksperiodeSoeknadKobling(dokumentkoblingVedtaksperiodeSoeknad)
                repository.opprettForespoerselSendt(dokumentkoblingForespoerselSendt)
                repository.opprettForespoerselUtgaatt(dokumentkoblingForespoerselUtgaatt)
            }

            test("hentForespoerselSykmeldingKoblinger() returner riktig når jobber er behandlet") {
                opprettDokumentkoblinger()
                dokumentkoblingSoeknad.let {
                    repository.settSykmeldingJobbTilBehandlet(sykmeldingId = it.sykmeldingId)
                    repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId = it.soeknadId)
                }

                val hentet = repository.hentForespoerselSykmeldingKoblinger()

                hentet.shouldNotBeEmpty()
                assertSoftly(hentet[0]) {
                    sykmeldingId shouldBe dokumentkoblingSykmelding.sykmeldingId
                    soeknadId shouldBe dokumentkoblingSoeknad.soeknadId
                    forespoerselId shouldBe dokumentkoblingForespoerselSendt.forespoerselId
                    sykmeldingStatus shouldBe Status.BEHANDLET
                    soeknadStatus shouldBe Status.BEHANDLET
                    forespoerselStatus shouldBe ForespoerselStatus.SENDT
                }
                hentet[1].forespoerselStatus shouldBe ForespoerselStatus.UTGAATT
            }

            context("hentForespoerselSykmeldingKoblinger() returner med riktig jobb status") {
                test("når bare sykmelding er behandlet") {
                    opprettDokumentkoblinger()
                    repository.settSykmeldingJobbTilBehandlet(sykmeldingId = dokumentkoblingSoeknad.sykmeldingId)
                    val hentet = repository.hentForespoerselSykmeldingKoblinger()
                    hentet.shouldNotBeEmpty()
                    assertSoftly(hentet[0]) {
                        sykmeldingStatus shouldBe Status.BEHANDLET
                        soeknadStatus shouldBe Status.MOTTATT
                    }
                }

                test("når bare søknad er behandlet") {
                    opprettDokumentkoblinger()
                    repository.settSykepengeSoeknadJobbTilBehandlet(soeknadId = dokumentkoblingSoeknad.soeknadId)
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
                repository.opprettSykmelding(dokumentkoblingSykmelding.copy(sykmeldingId = sykmeldingId1))
                repository.opprettSykepengesoeknad(
                    dokumentkoblingSoeknad.copy(
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
                repository.opprettSykmelding(dokumentkoblingSykmelding.copy(sykmeldingId = sykmeldingId2))
                repository.opprettSykepengesoeknad(
                    dokumentkoblingSoeknad.copy(
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
                    dokumentkoblingForespoerselSendt.copy(
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
                val inntektsmeldingGodkjent = dokumentkoblingInntektsmeldingGodkjent
                repository.hentInntektsmeldingerMedStatusMottatt() shouldBe emptyList()

                repository.opprettInntektmeldingGodkjent(inntektsmeldingGodkjent)

                val hentet = repository.hentInntektsmeldingerMedStatusMottatt()
                hentet.shouldNotBeEmpty()
                assertSoftly(hentet[0]) {
                    forespoerselId shouldBe inntektsmeldingGodkjent.forespoerselId
                    vedtaksperiodeId shouldBe inntektsmeldingGodkjent.vedtaksperiodeId
                    status shouldBe Status.MOTTATT
                    inntektsmeldingStatus shouldBe InntektsmeldingStatus.GODKJENT
                    innsendingType shouldBe InnsendingType.FORESPURT_EKSTERN
                }
            }
        },
    )
