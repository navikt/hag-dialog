import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.database.DokumentKoblingRepository
import no.nav.helsearbeidsgiver.database.Status
import no.nav.helsearbeidsgiver.database.SykepengesoeknadTable
import no.nav.helsearbeidsgiver.database.SykmeldingTable
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmelding
import no.nav.helsearbeidsgiver.dokumentKobling.Sykmeldingsperiode
import no.nav.helsearbeidsgiver.utils.test.date.januar
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DokumentKoblingTest :
    FunSpecWithDb(listOf(SykepengesoeknadTable, SykmeldingTable), { db ->
        val repository = DokumentKoblingRepository(db)
        val sykmelding = Sykmelding(
            sykmeldingId = UUID.randomUUID(),
            orgnr = Orgnr.genererGyldig(),
            foedselsdato = 1.januar,
            fulltNavn = "Ola Nordmann",
            sykmeldingsperioder = listOf(Sykmeldingsperiode(1.januar, 31.januar)),
        )

        test("skal kunne opprette og hente sykmelding") {
            val sykmeldingId = sykmelding.sykmeldingId

            repository.opprettSykmelding(
                sykmelding = sykmelding
            )

            val hentet = repository.hentSykmelding(sykmeldingId)
            hentet.shouldNotBeNull()
            hentet.id.value shouldBe sykmeldingId
            hentet.status shouldBe Status.MOTATT
        }

        test("skal kunne opprette og hente sykepengesoeknad koblet til sykmelding") {
            val sykmeldingId = sykmelding.sykmeldingId
            val soeknadId = UUID.randomUUID()

            repository.opprettSykmelding(
                sykmelding = sykmelding
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
