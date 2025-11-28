import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable
import no.nav.helsearbeidsgiver.dokumentKobling.Status

class DokumentKoblingTest :
    FunSpecWithDb(listOf(SykepengesoeknadTable, SykmeldingTable), { db ->
        val repository = DokumentKoblingRepository(db)
        val sykmelding = dokumentKoblingSykmelding
        val soeknad = dokumentKoblingSoeknad

        test("skal kunne opprette og hente sykmelding") {
            val sykmeldingId = sykmelding.sykmeldingId

            repository.opprettSykmelding(
                sykmelding = sykmelding,
            )

            val hentet = repository.hentSykmelding(sykmeldingId)
            hentet.shouldNotBeNull()
            hentet.id.value shouldBe sykmeldingId
            hentet.status shouldBe Status.MOTTATT
        }

        test("skal kunne opprette og hente sykepengesoeknad koblet til sykmelding") {
            val sykmeldingId = sykmelding.sykmeldingId
            val soeknadId = soeknad.soeknadId

            repository.opprettSykmelding(
                sykmelding = sykmelding,
            )

            val sykmeldingHentet = repository.hentSykmelding(sykmeldingId)
            sykmeldingHentet.shouldNotBeNull()
            sykmeldingHentet.id.value shouldBe sykmeldingId
            sykmeldingHentet.status shouldBe Status.MOTTATT

            repository.opprettSykepengesoeknad(dokumentKoblingSoeknad)

            val hentetSoeknad = repository.hentSykepengesoeknad(soeknadId)
            hentetSoeknad.shouldNotBeNull()
            hentetSoeknad.id.value shouldBe soeknadId
            hentetSoeknad.status shouldBe Status.MOTTATT

            hentetSoeknad.sykmeldingId shouldBe sykmeldingId
        }
    })
