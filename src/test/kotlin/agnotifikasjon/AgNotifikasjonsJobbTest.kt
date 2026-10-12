package agnotifikasjon

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbStatus
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjon
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjonService
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjonsJobb
import no.nav.helsearbeidsgiver.notifikasjon.DokumentIkkeFunnetException
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class AgNotifikasjonsJobbTest :
    FunSpec({
        val service = mockk<AgNotifikasjonService>()
        val prosesserer = AgNotifikasjonsJobb(service)
        val dokumentId = UUID.randomUUID()

        fun jobb(tjeneste: Tjeneste) =
            Bakgrunnsjobb(
                type = AgNotifikasjonsJobb.JOB_TYPE,
                dataJson = AgNotifikasjon(dokumentId, tjeneste).toJson(AgNotifikasjon.serializer()),
            )

        beforeTest {
            clearAllMocks()
        }

        test("delegerer sykmelding til service") {
            coEvery { service.opprettNotifikasjonerForSykmelding(dokumentId) } just Runs

            prosesserer.prosesser(jobb(Tjeneste.SYKMELDING))

            coVerify(exactly = 1) { service.opprettNotifikasjonerForSykmelding(dokumentId) }
            coVerify(exactly = 0) { service.opprettNotifikasjonerForSoeknad(any()) }
        }

        test("delegerer soeknad til service") {
            coEvery { service.opprettNotifikasjonerForSoeknad(dokumentId) } just Runs

            prosesserer.prosesser(jobb(Tjeneste.SOEKNAD))

            coVerify(exactly = 1) { service.opprettNotifikasjonerForSoeknad(dokumentId) }
            coVerify(exactly = 0) { service.opprettNotifikasjonerForSykmelding(any()) }
        }

        test("avbryter jobb med manglende eller ugyldig data uten aa kalle service") {
            listOf(null, "ugyldig".toJson()).forEach { data ->
                val jobb = Bakgrunnsjobb(type = AgNotifikasjonsJobb.JOB_TYPE, dataJson = data)

                shouldNotThrowAny { prosesserer.prosesser(jobb) }

                jobb.status shouldBe BakgrunnsjobbStatus.AVBRUTT
            }
            verify { service wasNot Called }
        }

        test("avbryter jobb med ukjent tjeneste uten å kalle service") {
            val tjeneste = Tjeneste.entries.first { it != Tjeneste.SYKMELDING && it != Tjeneste.SOEKNAD }
            val jobb = jobb(tjeneste)

            shouldNotThrowAny { prosesserer.prosesser(jobb) }

            jobb.status shouldBe BakgrunnsjobbStatus.AVBRUTT
            verify { service wasNot Called }
        }

        test("kaster manglende dokument videre for retry") {
            val feil = DokumentIkkeFunnetException("Dokument mangler")
            coEvery { service.opprettNotifikasjonerForSykmelding(dokumentId) } throws feil

            shouldThrow<DokumentIkkeFunnetException> {
                prosesserer.prosesser(jobb(Tjeneste.SYKMELDING))
            } shouldBe feil
        }

        test("kaster feil fra service videre for retry") {
            val feil = IllegalStateException("Notifikasjon feilet")
            coEvery { service.opprettNotifikasjonerForSoeknad(dokumentId) } throws feil

            shouldThrow<IllegalStateException> {
                prosesserer.prosesser(jobb(Tjeneste.SOEKNAD))
            } shouldBe feil
        }
    })
