package agnotifikasjon

import DokumentKoblingMockUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SakEllerOppgaveDuplikatException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.SykepengesoeknadEntity
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjonService
import no.nav.helsearbeidsgiver.notifikasjon.DokumentIkkeFunnetException
import java.util.UUID
import kotlin.time.Duration.Companion.days

class AgNotifikasjonServiceTest :
    FunSpec({
        val repository = mockk<DokumentkoblingRepository>()
        val klient = mockk<ArbeidsgiverNotifikasjonKlient>()
        val brregClient = mockk<BrregClient>()
        val service = AgNotifikasjonService(mockk<BakgrunnsjobbService>(), repository, klient, brregClient)
        val sykmelding = DokumentKoblingMockUtils.sykmelding
        val soeknadId = DokumentKoblingMockUtils.soeknadId
        val orgnr = sykmelding.orgnr.verdi

        beforeTest {
            clearAllMocks()
        }

        listOf(Tjeneste.SYKMELDING, Tjeneste.SOEKNAD).forEach { tjeneste ->
            test("oppretter sak og beskjed for $tjeneste") {
                val dokumentId = if (tjeneste == Tjeneste.SYKMELDING) sykmelding.sykmeldingId else soeknadId
                val dokumentType = if (tjeneste == Tjeneste.SYKMELDING) "sykmelding" else "sykepengesoeknad"
                val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/$dokumentType/$dokumentId.pdf"
                every { repository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns
                    mockk<SykmeldingEntity> { every { data } returns sykmelding }
                every { repository.hentSykepengesoeknadMedId(soeknadId) } returns
                    mockk<SykepengesoeknadEntity> {
                        every { sykmeldingId } returns sykmelding.sykmeldingId
                        every { this@mockk.orgnr } returns orgnr
                    }
                coEvery { brregClient.hentOrganisasjonNavn(setOf(orgnr)) } returns mapOf(sykmelding.orgnr to "Bedrift AS")
                coEvery { klient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns "sak-id"
                coEvery {
                    klient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
                } returns "beskjed-id"

                if (tjeneste == Tjeneste.SYKMELDING) {
                    service.opprettNotifikasjonerForSykmelding(dokumentId)
                } else {
                    service.opprettNotifikasjonerForSoeknad(dokumentId)
                }

                coVerify(exactly = 1) {
                    klient.opprettNySak(
                        virksomhetsnummer = orgnr,
                        grupperingsid = dokumentId.toString(),
                        tjeneste = tjeneste,
                        lenke = lenke,
                        tittel = match { it.contains(sykmelding.fulltNavn) },
                        statusTekst = any(),
                        tilleggsinfo = any(),
                        initiellStatus = SaksStatus.MOTTATT,
                        hardDeleteOm = 730.days,
                    )
                    klient.opprettNyBeskjed(
                        virksomhetsnummer = orgnr,
                        eksternId = dokumentId.toString(),
                        grupperingsid = dokumentId.toString(),
                        tjeneste = tjeneste,
                        lenke = lenke,
                        tekst = match { it.contains("Bedrift AS") },
                        tidspunkt = null,
                        varslingTittel = any(),
                        varslingInnhold = any(),
                        smsVarslingInnhold = any(),
                        hardDeleteOm = 730.days,
                    )
                }
            }
        }

        test("kaster exception naar sykmelding mangler") {
            val dokumentId = UUID.randomUUID()
            every { repository.hentSykmeldingEntitet(dokumentId) } returns null

            shouldThrow<DokumentIkkeFunnetException> {
                service.opprettNotifikasjonerForSykmelding(dokumentId)
            }

            coVerify { klient wasNot Called }
        }

        test("kaster exception naar soeknad mangler") {
            every { repository.hentSykepengesoeknadMedId(soeknadId) } returns null

            shouldThrow<DokumentIkkeFunnetException> {
                service.opprettNotifikasjonerForSoeknad(soeknadId)
            }

            coVerify { klient wasNot Called }
        }

        test("kaster exception naar sykmeldingen til soeknaden mangler") {
            every { repository.hentSykepengesoeknadMedId(soeknadId) } returns
                mockk<SykepengesoeknadEntity> { every { sykmeldingId } returns sykmelding.sykmeldingId }
            every { repository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns null

            shouldThrow<DokumentIkkeFunnetException> {
                service.opprettNotifikasjonerForSoeknad(soeknadId)
            }

            coVerify { klient wasNot Called }
        }

        test("ignorerer duplikat sak og beskjed uten å feile") {
            every { repository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns
                mockk<SykmeldingEntity> { every { data } returns sykmelding }
            coEvery { brregClient.hentOrganisasjonNavn(setOf(orgnr)) } returns emptyMap()
            coEvery {
                klient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any())
            } throws SakEllerOppgaveDuplikatException("sak-id", "duplikat")
            coEvery {
                klient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } throws SakEllerOppgaveDuplikatException("beskjed-id", "duplikat")

            shouldNotThrowAny {
                service.opprettNotifikasjonerForSykmelding(sykmelding.sykmeldingId)
            }

            coVerify(exactly = 1) {
                klient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        test("kaster klientfeil videre") {
            val feil = IllegalStateException("Notifikasjon feilet")
            every { repository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns
                mockk<SykmeldingEntity> { every { data } returns sykmelding }
            coEvery { brregClient.hentOrganisasjonNavn(setOf(orgnr)) } returns emptyMap()
            coEvery { klient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } throws feil

            shouldThrow<IllegalStateException> {
                service.opprettNotifikasjonerForSykmelding(sykmelding.sykmeldingId)
            } shouldBe feil

            coVerify(exactly = 0) {
                klient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    })
