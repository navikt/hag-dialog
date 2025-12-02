import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable
import no.nav.helsearbeidsgiver.database.VedtaksperiodeSoeknadTable
import no.nav.helsearbeidsgiver.dokumentKobling.Status
import java.util.UUID

class DokumentKoblingTest :
    FunSpecWithDb(listOf(SykepengesoeknadTable, SykmeldingTable, VedtaksperiodeSoeknadTable), { db ->
        val repository = DokumentKoblingRepository(db)

        test("opprette og hente sykmelding") {
            val sykmelding = dokumentKoblingSykmelding
            repository.opprettSykmelding(sykmelding)
            val hentet = repository.hentSykmeldingEntitet(sykmelding.sykmeldingId)

            hentet.shouldNotBeNull()
            hentet.id.value shouldBe sykmelding.sykmeldingId
            hentet.status shouldBe Status.MOTTATT
        }

        test("opprette og hente sykepengesoeknad koblet til sykmelding") {
            val sykmelding = dokumentKoblingSykmelding
            val soeknad = dokumentKoblingSoeknad
            repository.opprettSykmelding(sykmelding)
            repository.opprettSykepengesoeknad(soeknad)

            val hentetSoeknad = repository.hentSykepengesoeknad(soeknad.soeknadId)

            hentetSoeknad.shouldNotBeNull()
            hentetSoeknad.id.value shouldBe soeknad.soeknadId
            hentetSoeknad.status shouldBe Status.MOTTATT
            hentetSoeknad.sykmeldingId shouldBe sykmelding.sykmeldingId
        }

        test("hente mottatte sykmeldinger") {
            val sykmelding = dokumentKoblingSykmelding
            val sykmeldingId2 = UUID.randomUUID()
            repository.opprettSykmelding(sykmelding)
            repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))

            val hentet = repository.henteSykemeldingerMedStatusMottatt()

            hentet.size shouldBe 2
            hentet[0].sykmeldingId shouldBe sykmelding.sykmeldingId
            hentet[1].sykmeldingId shouldBe sykmeldingId2
        }

        test("hente mottatte søknader") {
            val soeknad = dokumentKoblingSoeknad
            val soeknadId2 = UUID.randomUUID()
            repository.opprettSykepengesoeknad(soeknad)
            repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))

            val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
            hentet.size shouldBe 2
            hentet[0].soeknadId shouldBe soeknad.soeknadId
            hentet[1].soeknadId shouldBe soeknadId2
        }

        test("oppdatere sykmeldinger til behandlet") {
            val sykmelding = dokumentKoblingSykmelding
            val sykmeldingId2 = UUID.randomUUID()
            repository.opprettSykmelding(sykmelding)
            repository.opprettSykmelding(sykmelding.copy(sykmeldingId = sykmeldingId2))
            repository.settSykmeldingStatusTilBehandlet(sykmelding.sykmeldingId)

            val hentet = repository.henteSykemeldingerMedStatusMottatt()
            hentet.size shouldBe 1
            hentet[0].sykmeldingId shouldBe sykmeldingId2
        }

        test("oppdatere søknader til behandlet") {
            val soeknad = dokumentKoblingSoeknad
            val soeknadId2 = UUID.randomUUID()
            repository.opprettSykepengesoeknad(soeknad)
            repository.opprettSykepengesoeknad(soeknad.copy(soeknadId = soeknadId2))
            repository.settSykepengeSoeknadStatusTilBehandlet(soeknad.soeknadId)

            val hentet = repository.henteSykepengeSoeknaderMedStatusMottatt()
            hentet.size shouldBe 1
            hentet[0].soeknadId shouldBe soeknadId2
        }

        test("opprette vedtaksperiode soeknad kobling") {
            val vedtaksperiodeSoeknad = dokumentKoblingvedtaksperiodeSoeknad
            val soeknadId2 = UUID.randomUUID()
            repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId) shouldBe emptyList()
            repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)
            repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad.copy(soeknadId = soeknadId2))
            val hentet = repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)
            hentet.size shouldBe 2
            hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId, soeknadId2)
        }

        test("håndtere vedtaksperiode soeknad kobling som finnes fra før uten å oppdatere opprettettidspunktet") {
            val vedtaksperiodeSoeknad = dokumentKoblingvedtaksperiodeSoeknad
            repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId) shouldBe emptyList()
            repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

            val opprettetFør = repository.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId).first().opprettet

            repository.opprettVedtaksperiodeSoeknadKobling(vedtaksperiodeSoeknad)

            val opprettetEtter = repository.hentSoeknaderForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId).first().opprettet
            val hentet = repository.hentListeAvSoeknadIdForVedtaksperiodeId(vedtaksperiodeSoeknad.vedtaksperiodeId)

            hentet.size shouldBe 1
            hentet shouldContainOnly listOf(vedtaksperiodeSoeknad.soeknadId)
            opprettetFør shouldBe opprettetEtter
        }
    })
