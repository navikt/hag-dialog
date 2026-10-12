package no.nav.helsearbeidsgiver.notifikasjon

import io.ktor.util.escapeHTML
import no.nav.hag.utils.bakgrunnsjobb.BakgrunnsjobbService
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SakEllerOppgaveDuplikatException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.json.toJson
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.util.UUID
import kotlin.time.Duration.Companion.days
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode as SykmeldingsperiodeKafka

private const val MAKS_ANTALL_FORSOK = 10
private val HARD_DELETE_OM = 730.days

class DokumentIkkeFunnetException(
    melding: String,
) : RuntimeException(melding)

class AgNotifikasjonService(
    private val bakgrunnsjobbService: BakgrunnsjobbService,
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val agNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    private val brregClient: BrregClient,
) {
    private val logger = logger()
    private val sikkerLogger = sikkerLogger()

    fun opprettAgNotifikasjonsJobb(
        dokumentId: UUID,
        tjeneste: Tjeneste,
    ) {
        bakgrunnsjobbService.opprettJobbJson<AgNotifikasjonsJobb>(
            maksAntallForsoek = MAKS_ANTALL_FORSOK,
            data =
                AgNotifikasjon(
                    dokumentId = dokumentId,
                    tjeneste = tjeneste,
                ).toJson(AgNotifikasjon.serializer()),
        )
        logger.info("Opprettet AgNotifikasjonsJobb for dokumentId: $dokumentId, tjeneste: $tjeneste")
    }

    suspend fun opprettNotifikasjonerForSykmelding(sykmeldingId: UUID) {
        val sykmelding =
            dokumentkoblingRepository.hentSykmeldingEntitet(sykmeldingId)?.data
                ?: throw DokumentIkkeFunnetException(
                    "Fant ikke sykmelding $sykmeldingId i databasen. Kan ikke opprette notifikasjoner enda.",
                )

        val beskrivelse = "sykmelding $sykmeldingId"
        val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf"
        val grupperingsid = sykmeldingId.toString()
        val orgnr = sykmelding.orgnr.verdi
        val virksomhetsnavn = hentVirksomhetsnavn(orgnr)
        val htmlSikkertVirksomhetsnavn = virksomhetsnavn.escapeHTML()
        opprettSak(
            beskrivelse = beskrivelse,
            virksomhetsnummer = orgnr,
            grupperingsid = grupperingsid,
            tjeneste = Tjeneste.SYKMELDING,
            lenke = lenke,
            tittel = "Sykmelding for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()}) hos $virksomhetsnavn",
            statusTekst = "Mottatt sykmelding",
            tilleggsinfo =
                sykmelding.sykmeldingsperioder
                    .map { SykmeldingsperiodeKafka(it.fom, it.tom) }
                    .getSykmeldingsPerioderString(),
        )

        opprettBeskjed(
            beskrivelse = beskrivelse,
            virksomhetsnummer = orgnr,
            eksternId = sykmeldingId.toString(),
            grupperingsid = grupperingsid,
            tjeneste = Tjeneste.SYKMELDING,
            lenke = lenke,
            tekst = "Ny sykmelding hos $virksomhetsnavn",
            varslingTittel = "Ny sykmelding for en av dine ansatte hos $virksomhetsnavn",
            varslingInnhold =
                "<p>En ansatt hos $htmlSikkertVirksomhetsnavn (orgnr $orgnr) har sendt inn en ny sykmelding.</p>" +
                    "<p>Logg inn på Altinn eller Nav for å se sykmeldingen.</p>" +
                    "<p>Vennlig hilsen Nav.</p>",
            smsVarslingInnhold =
                "En ansatt hos $virksomhetsnavn (orgnr $orgnr) har sendt inn en ny sykmelding. " +
                    "Logg inn på Altinn eller Nav for å se sykmeldingen. Vennlig hilsen Nav.",
        )
    }

    suspend fun opprettNotifikasjonerForSoeknad(soeknadId: UUID) {
        val soeknad =
            dokumentkoblingRepository.hentSykepengesoeknadMedId(soeknadId)
                ?: throw DokumentIkkeFunnetException(
                    "Fant ikke sykepengesøknad $soeknadId i databasen. Kan ikke opprette notifikasjoner enda.",
                )

        val sykmelding =
            dokumentkoblingRepository.hentSykmeldingEntitet(soeknad.sykmeldingId)?.data
                ?: throw DokumentIkkeFunnetException(
                    "Fant ikke sykmelding ${soeknad.sykmeldingId} i databasen. " +
                        "Kan ikke opprette notifikasjoner for sykepengesøknad $soeknadId enda.",
                )

        val beskrivelse = "sykepengesøknad $soeknadId"
        val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf"
        val grupperingsid = soeknadId.toString()
        val orgnr = soeknad.orgnr
        val virksomhetsnavn = hentVirksomhetsnavn(orgnr)
        val htmlSikkertVirksomhetsnavn = virksomhetsnavn.escapeHTML()
        logger.info("Oppretter notifikasjoner for sykepengesøknad $soeknadId hos virksomhet $virksomhetsnavn (orgnr $orgnr).")
        opprettSak(
            beskrivelse = beskrivelse,
            virksomhetsnummer = orgnr,
            grupperingsid = grupperingsid,
            tjeneste = Tjeneste.SOEKNAD,
            lenke = lenke,
            tittel =
                "Søknad om sykepenger for ${sykmelding.fulltNavn} (f. ${sykmelding.foedselsdato.tilNorskFormat()}) " +
                    "hos $virksomhetsnavn",
            statusTekst = "Mottatt søknad om sykepenger",
            tilleggsinfo = null,
        )

        opprettBeskjed(
            beskrivelse = beskrivelse,
            virksomhetsnummer = orgnr,
            eksternId = soeknadId.toString(),
            grupperingsid = grupperingsid,
            tjeneste = Tjeneste.SOEKNAD,
            lenke = lenke,
            tekst = "Ny søknad om sykepenger hos $virksomhetsnavn",
            varslingTittel = "Ny søknad om sykepenger for en av dine ansatte hos $virksomhetsnavn",
            varslingInnhold =
                "<p>En ansatt hos $htmlSikkertVirksomhetsnavn (orgnr $orgnr) har sendt inn en søknad om sykepenger.</p>" +
                    "<p>Logg inn på Altinn eller Nav for å se søknaden.</p>" +
                    "<p>Vennlig hilsen Nav.</p>",
            smsVarslingInnhold =
                "En ansatt hos $virksomhetsnavn (orgnr $orgnr) har sendt inn en søknad om sykepenger. " +
                    "Logg inn på Altinn eller Nav for å se søknaden. Vennlig hilsen Nav.",
        )
    }

    private suspend fun hentVirksomhetsnavn(orgnr: String): String =
        brregClient.hentOrganisasjonNavn(setOf(orgnr)).values.firstOrNull() ?: orgnr

    private suspend fun opprettSak(
        beskrivelse: String,
        virksomhetsnummer: String,
        grupperingsid: String,
        tjeneste: Tjeneste,
        lenke: String,
        tittel: String,
        statusTekst: String,
        tilleggsinfo: String?,
    ) {
        try {
            val sakId =
                agNotifikasjonKlient.opprettNySak(
                    virksomhetsnummer = virksomhetsnummer,
                    grupperingsid = grupperingsid,
                    tjeneste = tjeneste,
                    lenke = lenke,
                    tittel = tittel,
                    statusTekst = statusTekst,
                    tilleggsinfo = tilleggsinfo,
                    initiellStatus = SaksStatus.MOTTATT,
                    hardDeleteOm = HARD_DELETE_OM,
                )
            logger.info("Opprettet notifikasjon-sak $sakId for $beskrivelse.")
        } catch (e: SakEllerOppgaveDuplikatException) {
            logger.warn("Duplikat sak for $beskrivelse: ${e.eksisterendeId}")
        } catch (e: Exception) {
            "Feil ved opprettelse av notifikasjon-sak for $beskrivelse".also {
                logger.error(it)
                sikkerLogger.error(it, e)
            }
            throw e
        }
    }

    private suspend fun opprettBeskjed(
        beskrivelse: String,
        virksomhetsnummer: String,
        eksternId: String,
        grupperingsid: String,
        tjeneste: Tjeneste,
        lenke: String,
        tekst: String,
        varslingTittel: String,
        varslingInnhold: String,
        smsVarslingInnhold: String,
    ) {
        try {
            val beskjedId =
                agNotifikasjonKlient.opprettNyBeskjed(
                    virksomhetsnummer = virksomhetsnummer,
                    eksternId = eksternId,
                    grupperingsid = grupperingsid,
                    tjeneste = tjeneste,
                    lenke = lenke,
                    tekst = tekst,
                    tidspunkt = null,
                    varslingTittel = varslingTittel,
                    varslingInnhold = varslingInnhold,
                    smsVarslingInnhold = smsVarslingInnhold,
                    hardDeleteOm = HARD_DELETE_OM,
                )
            logger.info("Opprettet notifikasjon-beskjed $beskjedId for $beskrivelse.")
        } catch (e: SakEllerOppgaveDuplikatException) {
            logger.warn("Duplikat beskjed for $beskrivelse: ${e.eksisterendeId}")
        } catch (e: Exception) {
            "Feil ved opprettelse av notifikasjon-beskjed for $beskrivelse".also {
                logger.error(it)
                sikkerLogger.error(it, e)
            }
            throw e
        }
    }
}
