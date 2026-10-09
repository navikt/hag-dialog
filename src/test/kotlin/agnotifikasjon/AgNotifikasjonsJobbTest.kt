package agnotifikasjon

import DokumentKoblingMockUtils
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.hag.utils.bakgrunnsjobb.Bakgrunnsjobb
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.SykepengesoeknadEntity
import no.nav.helsearbeidsgiver.database.SykmeldingEntity
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjon
import no.nav.helsearbeidsgiver.notifikasjon.AgNotifikasjonsJobb
import no.nav.helsearbeidsgiver.notifikasjon.DokumentIkkeFunnetException
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.json.toJson
import java.util.UUID

class AgNotifikasjonsJobbTest :
    FunSpec({
        val dokumentkoblingRepository = mockk<DokumentkoblingRepository>()
        val agNotifikasjonKlient = mockk<ArbeidsgiverNotifikasjonKlient>()
        val unleashFeatureToggles = mockk<UnleashFeatureToggles>()
        val brregClient = mockk<BrregClient>()
        val jobb = AgNotifikasjonsJobb(dokumentkoblingRepository, agNotifikasjonKlient, unleashFeatureToggles, brregClient)

        fun bakgrunnsjobb(
            dokumentId: UUID,
            tjeneste: Tjeneste,
        ) = Bakgrunnsjobb(
            type = AgNotifikasjonsJobb.JOB_TYPE,
            dataJson = AgNotifikasjon(dokumentId, tjeneste).toJson(AgNotifikasjon.serializer()),
        )

        beforeTest {
            clearAllMocks()
            every { unleashFeatureToggles.skalOppretteNotifikasjoner() } returns true
        }

        test("oppretter sak og beskjed for sykmelding når den finnes") {
            val sykmelding = DokumentKoblingMockUtils.sykmelding
            val sykmeldingId = sykmelding.sykmeldingId
            val orgnr = sykmelding.orgnr.verdi
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmeldingId) } returns
                mockk<SykmeldingEntity> { every { data } returns sykmelding }
            coEvery { brregClient.hentOrganisasjonNavn(setOf(orgnr)) } returns mapOf(sykmelding.orgnr to "Bedrift AS")
            coEvery { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns "sak-id"
            coEvery {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns "beskjed-id"

            jobb.prosesser(bakgrunnsjobb(sykmeldingId, Tjeneste.SYKMELDING))

            coVerify(exactly = 1) {
                agNotifikasjonKlient.opprettNySak(
                    virksomhetsnummer = orgnr,
                    grupperingsid = sykmeldingId.toString(),
                    tjeneste = Tjeneste.SYKMELDING,
                    lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf",
                    tittel = match { it.contains(sykmelding.fulltNavn) && it.contains("Bedrift AS") },
                    statusTekst = "Mottatt sykmelding",
                    tilleggsinfo = any(),
                    initiellStatus = SaksStatus.MOTTATT,
                    hardDeleteOm = any(),
                )
            }
            coVerify(exactly = 1) {
                agNotifikasjonKlient.opprettNyBeskjed(
                    virksomhetsnummer = orgnr,
                    eksternId = sykmeldingId.toString(),
                    grupperingsid = sykmeldingId.toString(),
                    tjeneste = Tjeneste.SYKMELDING,
                    lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf",
                    tekst = "Ny sykmelding hos Bedrift AS",
                    tidspunkt = null,
                    varslingTittel = any(),
                    varslingInnhold = any(),
                    smsVarslingInnhold = any(),
                    hardDeleteOm = any(),
                )
            }
        }

        test("oppretter sak og beskjed for søknad når søknad og sykmelding finnes") {
            val sykmelding = DokumentKoblingMockUtils.sykmelding
            val soeknadId = DokumentKoblingMockUtils.soeknadId
            val orgnr = sykmelding.orgnr.verdi
            every { dokumentkoblingRepository.hentSykepengesoeknadMedId(soeknadId) } returns
                mockk<SykepengesoeknadEntity> {
                    every { sykmeldingId } returns sykmelding.sykmeldingId
                    every { this@mockk.orgnr } returns orgnr
                }
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmelding.sykmeldingId) } returns
                mockk<SykmeldingEntity> { every { data } returns sykmelding }
            coEvery { brregClient.hentOrganisasjonNavn(setOf(orgnr)) } returns emptyMap()
            coEvery { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) } returns "sak-id"
            coEvery {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            } returns "beskjed-id"

            jobb.prosesser(bakgrunnsjobb(soeknadId, Tjeneste.SOEKNAD))

            coVerify(exactly = 1) {
                agNotifikasjonKlient.opprettNySak(
                    virksomhetsnummer = orgnr,
                    grupperingsid = soeknadId.toString(),
                    tjeneste = Tjeneste.SOEKNAD,
                    lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf",
                    tittel = any(),
                    statusTekst = "Mottatt søknad om sykepenger",
                    tilleggsinfo = null,
                    initiellStatus = SaksStatus.MOTTATT,
                    hardDeleteOm = any(),
                )
            }
            coVerify(exactly = 1) {
                agNotifikasjonKlient.opprettNyBeskjed(
                    virksomhetsnummer = orgnr,
                    eksternId = soeknadId.toString(),
                    grupperingsid = soeknadId.toString(),
                    tjeneste = Tjeneste.SOEKNAD,
                    lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf",
                    tekst = "Ny søknad om sykepenger hos $orgnr",
                    tidspunkt = null,
                    varslingTittel = any(),
                    varslingInnhold = any(),
                    smsVarslingInnhold = any(),
                    hardDeleteOm = any(),
                )
            }
        }

        test("gjør ingenting når notifikasjoner er skrudd av i Unleash") {
            every { unleashFeatureToggles.skalOppretteNotifikasjoner() } returns false

            jobb.prosesser(bakgrunnsjobb(UUID.randomUUID(), Tjeneste.SYKMELDING))

            verify(exactly = 0) { dokumentkoblingRepository.hentSykmeldingEntitet(any()) }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        test("fullfører uten retry når dataJson mangler") {
            shouldNotThrowAny {
                jobb.prosesser(Bakgrunnsjobb(type = AgNotifikasjonsJobb.JOB_TYPE, dataJson = null))
            }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        test("fullfører uten retry når dataJson er ugyldig") {
            shouldNotThrowAny {
                jobb.prosesser(
                    Bakgrunnsjobb(
                        type = AgNotifikasjonsJobb.JOB_TYPE,
                        dataJson = mapOf("dokumentId" to "ikke-en-uuid".toJson()).toJson(),
                    ),
                )
            }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        test("kaster exception slik at jobben prøves igjen når sykmelding ikke finnes") {
            val sykmeldingId = UUID.randomUUID()
            every { dokumentkoblingRepository.hentSykmeldingEntitet(sykmeldingId) } returns null

            shouldThrow<DokumentIkkeFunnetException> {
                jobb.prosesser(bakgrunnsjobb(sykmeldingId, Tjeneste.SYKMELDING))
            }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        test("kaster exception slik at jobben prøves igjen når søknad ikke finnes") {
            val soeknadId = UUID.randomUUID()
            every { dokumentkoblingRepository.hentSykepengesoeknadMedId(soeknadId) } returns null

            shouldThrow<DokumentIkkeFunnetException> {
                jobb.prosesser(bakgrunnsjobb(soeknadId, Tjeneste.SOEKNAD))
            }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
        }

        test("fullfører uten retry ved ukjent tjeneste") {
            val ukjentTjeneste = Tjeneste.entries.first { it != Tjeneste.SYKMELDING && it != Tjeneste.SOEKNAD }
            shouldNotThrowAny {
                jobb.prosesser(bakgrunnsjobb(UUID.randomUUID(), ukjentTjeneste))
            }
            coVerify(exactly = 0) { agNotifikasjonKlient.opprettNySak(any(), any(), any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) {
                agNotifikasjonKlient.opprettNyBeskjed(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any())
            }
        }
    })
