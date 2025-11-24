import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import java.util.UUID
import no.nav.helsearbeidsgiver.database.DialogTable
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.Status
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable
import no.nav.helsearbeidsgiver.database.TransmissionTable
import org.jetbrains.exposed.sql.transactions.transaction

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

        val sykmelding = repository.opprettSykmelding(
            sykmeldingId = sykmeldingId,
            status = Status.MOTATT,
        )

        val sykmeldingHentet = repository.hentSykmelding(sykmeldingId)
        sykmeldingHentet.shouldNotBeNull()

        repository.opprettSykepengesoeknad(
            soeknadId = soeknadId,
            sykmelding = sykmeldingHentet,
            status = Status.BEHANDLET,
        )

        val hentet = repository.hentSykepengesoeknad(soeknadId)
        hentet.shouldNotBeNull()
        hentet.id.value shouldBe soeknadId
        hentet.status shouldBe Status.BEHANDLET

        transaction(db) {
            // verify kobling
            hentet.sykmelding.id.value shouldBe sykmeldingId
        }
    }
})