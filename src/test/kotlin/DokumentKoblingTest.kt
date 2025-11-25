import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.Status
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable

class DokumentKoblingTest:  FunSpecWithDb(listOf(SykepengesoeknadTable, SykmeldingTable), { db ->
    val repository = DokumentKoblingRepository(db)

    test("skal kunne opprette og hente sykmelding") {
        val sykmeldingId = UUID.randomUUID()

        repository.opprettSykmelding(
            sykmeldingId = sykmeldingId,
            status = Status.MOTATT,
        )

        val hentet = repository.hentSykmelding(sykmeldingId)
        hentet.shouldNotBeNull()
        hentet.id.value shouldBe sykmeldingId
        hentet.status shouldBe Status.MOTATT
    }

    test("skal kunne opprette og hente sykepengesoeknad koblet til sykmelding") {
        val sykmeldingId = UUID.randomUUID()
        val soeknadId = UUID.randomUUID()

        repository.opprettSykmelding(
            sykmeldingId = sykmeldingId,
            status = Status.MOTATT,
        )

        val sykmeldingHentet = repository.hentSykmelding(sykmeldingId)
        sykmeldingHentet.shouldNotBeNull()
        sykmeldingHentet.id.value shouldBe sykmeldingId
        sykmeldingHentet.status shouldBe Status.MOTATT

        repository.opprettSykepengesoeknad(
            soeknadId = soeknadId,
            sykmeldingId = sykmeldingId,
            status = Status.BEHANDLET,
        )

        val hentetSoeknad = repository.hentSykepengesoeknad(soeknadId)
        hentetSoeknad.shouldNotBeNull()
        hentetSoeknad.id.value shouldBe soeknadId
        hentetSoeknad.status shouldBe Status.BEHANDLET

        hentetSoeknad.sykmeldingId shouldBe sykmeldingId
    }
})