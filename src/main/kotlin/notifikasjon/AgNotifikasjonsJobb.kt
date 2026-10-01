package no.nav.helsearbeidsgiver.notifikasjon

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.hag.utils.bakgrunnsjobb.RecurringJob
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonKlient
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.SakEllerOppgaveDuplikatException
import no.nav.helsearbeidsgiver.arbeidsgivernotifikasjon.Tjeneste
import no.nav.helsearbeidsgiver.arbeidsgivernotifkasjon.graphql.generated.enums.SaksStatus
import no.nav.helsearbeidsgiver.brreg.BrregClient
import no.nav.helsearbeidsgiver.database.DokumentkoblingRepository
import no.nav.helsearbeidsgiver.database.NotifikasjonRepository
import no.nav.helsearbeidsgiver.kafka.getSykmeldingsPerioderString
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.tilNorskFormat
import java.time.Duration
import java.util.UUID
import kotlin.time.Duration.Companion.days
import no.nav.helsearbeidsgiver.kafka.Sykmeldingsperiode as SykmeldingsperiodeKafka

private val HARD_DELETE_OM = 730.days

class AgNotifikasjonsJobb(
    private val notifikasjonRepository: NotifikasjonRepository,
    private val dokumentkoblingRepository: DokumentkoblingRepository,
    private val agNotifikasjonKlient: ArbeidsgiverNotifikasjonKlient,
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val brregClient: BrregClient,
) : RecurringJob(CoroutineScope(Dispatchers.IO), Duration.ofSeconds(30).toMillis()) {
    override fun doJob() {
        runBlocking {
            behandleNotifikasjoner()
        }
    }

    private suspend fun behandleNotifikasjoner() {
        if (!unleashFeatureToggles.skalOppretteNotifikasjoner()) {
            logger.warn("Oppretter ikke notifikasjoner da det er deaktivert i Unleash.")
            return
        }

        val notifikasjoner = notifikasjonRepository.hentNyeNotifikasjoner()

        logger.info("Fant ${notifikasjoner.size} nye notifikasjoner klar til behandling.")

        notifikasjoner.forEach { notifikasjon ->
            try {
                val erOpprettet =
                    when (notifikasjon.tjeneste) {
                        Tjeneste.SYKMELDING -> {
                            opprettNotifikasjonerForSykmelding(notifikasjon.dokumentId)
                        }

                        Tjeneste.SOEKNAD -> {
                            opprettNotifikasjonerForSoeknad(notifikasjon.dokumentId)
                        }

                        else -> {
                            logger.warn(
                                "Ukjent tjeneste ${notifikasjon.tjeneste} for notifikasjon ${notifikasjon.notifikasjonId}, hopper over.",
                            )
                            false
                        }
                    }

                if (erOpprettet) {
                    notifikasjonRepository.settNotifikasjonSendt(notifikasjon.notifikasjonId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Feil ved behandling av notifikasjon for tjeneste ${notifikasjon.tjeneste} og dokument ${notifikasjon.dokumentId}".also {
                    logger.error(it)
                    sikkerLogger().error(it, e)
                }
            }
        }
    }

    private suspend fun opprettNotifikasjonerForSykmelding(sykmeldingId: UUID): Boolean {
        val sykmelding =
            dokumentkoblingRepository.hentSykmeldingEntitet(sykmeldingId)?.data
                ?: run {
                    logger.warn("Fant ikke sykmelding $sykmeldingId i databasen. Kan ikke opprette notifikasjoner enda.")
                    return false
                }

        val beskrivelse = "sykmelding $sykmeldingId"
        val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykmelding/$sykmeldingId.pdf"
        val grupperingsid = sykmeldingId.toString()
        val orgnr = sykmelding.orgnr.verdi
        val virksomhetsnavn = hentVirksomhetsnavn(orgnr)
        val htmlSikkertVirksomhetsnavn = virksomhetsnavn.escapetHtml()
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

        return true
    }

    private suspend fun opprettNotifikasjonerForSoeknad(soeknadId: UUID): Boolean {
        val soeknad =
            dokumentkoblingRepository.hentSykepengesoeknadMedId(soeknadId)
                ?: run {
                    logger.warn("Fant ikke sykepengesøknad $soeknadId i databasen. Kan ikke opprette notifikasjoner enda.")
                    return false
                }

        val sykmelding =
            dokumentkoblingRepository.hentSykmeldingEntitet(soeknad.sykmeldingId)?.data
                ?: run {
                    logger.warn(
                        "Fant ikke sykmelding ${soeknad.sykmeldingId} i databasen. " +
                            "Kan ikke opprette notifikasjoner for sykepengesøknad $soeknadId enda.",
                    )
                    return false
                }

        val beskrivelse = "sykepengesøknad $soeknadId"
        val lenke = "${Env.Nav.arbeidsgiverGuiBaseUrl}/dokument/sykepengesoeknad/$soeknadId.pdf"
        val grupperingsid = soeknadId.toString()
        val orgnr = soeknad.orgnr
        val virksomhetsnavn = hentVirksomhetsnavn(orgnr)
        val htmlSikkertVirksomhetsnavn = virksomhetsnavn.escapetHtml()

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

        return true
    }

    private suspend fun hentVirksomhetsnavn(orgnr: String): String =
        brregClient.hentOrganisasjonNavn(setOf(orgnr)).values.firstOrNull() ?: orgnr

    private fun String.escapetHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

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
                sikkerLogger().error(it, e)
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
                sikkerLogger().error(it, e)
            }
            throw e
        }
    }
}
